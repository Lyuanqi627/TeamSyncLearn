package com.teamsync.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenInterceptor.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${ai-integration.api-key:}")
    private String aiApiKey;

    public TokenInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return reject(response, "未登录或token已过期");
        }

        // AI Agent 集成：/api/ai/** 可用固定密钥访问，不绑定用户会话。
        // 用户身份由接口的显式 userId 参数提供，不设置 UserContext。
        // 必须在 Redis 查询之前判断——否则 AI 密钥会被当作会话 token 查 Redis 而误判 401。
        String uri = request.getRequestURI();
        if (aiApiKey != null && !aiApiKey.isEmpty()
                && uri.startsWith("/api/ai/")
                && aiApiKey.equals(token)) {
            return true;
        }

        // Session 存 Redis：key=token, value=userId, 过期由 TTL 兜底
        String userIdStr;
        try {
            userIdStr = redisTemplate.opsForValue().get(token);
        } catch (Exception e) {
            log.error("Redis 会话查询失败: {}", e.getMessage(), e);
            return reject(response, "会话服务暂不可用，请稍后重试");
        }
        if (userIdStr == null) {
            return reject(response, "token已过期，请重新登录");
        }
        try {
            UserContext.setUserId(Long.parseLong(userIdStr));
        } catch (NumberFormatException e) {
            log.warn("会话值非法, token={}", token);
            return reject(response, "会话无效，请重新登录");
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
