package com.teamsync.common;

import cn.hutool.core.date.DateUtil;
import com.teamsync.entity.SysSession;
import com.teamsync.mapper.SysSessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final SysSessionMapper sessionMapper;

    @Value("${ai-integration.api-key:}")
    private String aiApiKey;

    public TokenInterceptor(SysSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或token已过期\",\"data\":null}");
            return false;
        }

        // AI Agent 集成：/api/ai/** 可用固定密钥访问，不绑定用户会话。
        // 用户身份由接口的显式 userId 参数提供，不设置 UserContext。
        String uri = request.getRequestURI();
        if (aiApiKey != null && !aiApiKey.isEmpty()
                && uri.startsWith("/api/ai/")
                && aiApiKey.equals(token)) {
            return true;
        }

        SysSession session = sessionMapper.selectById(token);
        if (session == null || session.getExpireAt().before(new Date())) {
            if (session != null) {
                sessionMapper.deleteById(token);
            }
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"token已过期，请重新登录\",\"data\":null}");
            return false;
        }

        UserContext.setUserId(session.getUserId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
