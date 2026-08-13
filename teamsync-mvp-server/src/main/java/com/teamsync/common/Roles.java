package com.teamsync.common;

/**
 * 三级权限角色常量与工具。
 * SUPER_ADMIN（最终管理员）/ ADMIN（二级管理员）/ MEMBER（普通成员）。
 */
public final class Roles {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String MEMBER = "MEMBER";

    private Roles() {}

    /** DB role 为 null/空 时兜底为 MEMBER */
    public static String normalize(String role) {
        return (role == null || role.isBlank()) ? MEMBER : role;
    }

    /** 管理员及以上（可读全量数据） */
    public static boolean isAdmin(String role) {
        return ADMIN.equals(role) || SUPER_ADMIN.equals(role);
    }

    /** role -> ROLE_<role>，供 SimpleGrantedAuthority 与 @PreAuthorize 对齐 */
    public static String authority(String role) {
        return "ROLE_" + normalize(role);
    }
}
