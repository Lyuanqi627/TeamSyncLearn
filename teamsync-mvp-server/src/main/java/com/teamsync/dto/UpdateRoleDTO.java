package com.teamsync.dto;

/**
 * 超级管理员调整用户角色的入参。
 * 仅允许授予/收回 ADMIN / MEMBER（SUPER_ADMIN 不可由接口授予）。
 */
public class UpdateRoleDTO {
    /** 目标角色：仅 ADMIN / MEMBER */
    private String role;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
