package com.teamsync.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userRoleHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    public static Long getUserId() {
        return userIdHolder.get();
    }

    public static void setUserRole(String role) {
        userRoleHolder.set(role);
    }

    public static String getUserRole() {
        return userRoleHolder.get();
    }

    /**
     * 从 SecurityContext 单向同步 userId / role 到本 ThreadLocal。
     * 仅当 principal 是 Long（真实用户会话）时写入；AI 密钥旁路 principal 为 String，
     * 故旁路下 userId/role 均为 null —— UserContext.getUserRole()==null 即旁路语义。
     */
    public static void syncFromSecurityContext() {
        userIdHolder.remove();
        userRoleHolder.remove();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        if (auth.getPrincipal() instanceof Long uid) {
            userIdHolder.set(uid);
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String authority = ga.getAuthority();
                if (authority != null && authority.startsWith("ROLE_")) {
                    userRoleHolder.set(authority.substring("ROLE_".length()));
                    break;
                }
            }
        }
    }

    public static void clear() {
        userIdHolder.remove();
        userRoleHolder.remove();
    }
}
