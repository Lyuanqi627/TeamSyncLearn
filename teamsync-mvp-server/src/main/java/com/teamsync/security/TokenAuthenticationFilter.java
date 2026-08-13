package com.teamsync.security;

import com.teamsync.common.Roles;
import com.teamsync.common.UserContext;
import com.teamsync.entity.SysUser;
import com.teamsync.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 无状态 token 认证过滤器，替代原 TokenInterceptor。
 * 裸 token（Authorization 头，无 Bearer 前缀）：
 *   1. 非 /api/** 请求直接放行；
 *   2. AI 密钥旁路：/api/ai/** 且 token==ai-key → 合成 ROLE_AI_BYPASS 认证（不填 UserContext）；
 *   3. Redis 会话查询 → userId → 每请求查 DB 读 role（角色变更即时生效）→ 构造认证。
 * 本过滤器绝不写 401/403；无效凭证只"不设认证"，由异常处理链输出统一 JSON。
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    static final String BYPASS_PRINCIPAL = "__AI_BYPASS__";
    static final String ROLE_AI_BYPASS = "ROLE_AI_BYPASS";

    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;

    @Value("${ai-integration.api-key:}")
    private String aiApiKey;

    public TokenAuthenticationFilter(StringRedisTemplate redisTemplate, SysUserMapper sysUserMapper) {
        this.redisTemplate = redisTemplate;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String uri = request.getRequestURI();
            if (uri == null || !uri.startsWith("/api/")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // AI 旁路：必须在 Redis 查询前判断，否则密钥会被当作会话 token 查 Redis。
            if (aiApiKey != null && !aiApiKey.isEmpty()
                    && uri.startsWith("/api/ai/")
                    && aiApiKey.equals(token)) {
                setBypassAuthentication();
                filterChain.doFilter(request, response);
                return;
            }

            String userIdStr;
            try {
                userIdStr = redisTemplate.opsForValue().get(token);
            } catch (Exception e) {
                log.error("Redis 会话查询失败: {}", e.getMessage(), e);
                filterChain.doFilter(request, response); // 会话服务不可用 → 不设认证 → 401
                return;
            }
            if (userIdStr == null) {
                filterChain.doFilter(request, response); // token 无效/过期 → 不设认证 → 401
                return;
            }

            long userId;
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("会话值非法, token={}", token);
                filterChain.doFilter(request, response);
                return;
            }

            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                filterChain.doFilter(request, response); // 用户已被删除 → 不设认证 → 401
                return;
            }

            setUserAuthentication(user.getId(), Roles.normalize(user.getRole()));
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束，清理线程上下文，避免线程复用污染
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** 真实用户会话：principal=userId(Long)，ROLE_<role>，并同步 UserContext。 */
    private void setUserAuthentication(Long userId, String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority(Roles.authority(role));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(auth);
        UserContext.syncFromSecurityContext();
    }

    /** AI 密钥旁路：principal 为非 Long 字符串，UserContext 不填充（getUserRole()==null）。 */
    private void setBypassAuthentication() {
        GrantedAuthority authority = new SimpleGrantedAuthority(ROLE_AI_BYPASS);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(BYPASS_PRINCIPAL, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(auth);
        UserContext.syncFromSecurityContext();
    }
}
