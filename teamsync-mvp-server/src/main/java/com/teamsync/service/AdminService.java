package com.teamsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamsync.common.Roles;
import com.teamsync.common.UserContext;
import com.teamsync.dto.UpdateRoleDTO;
import com.teamsync.entity.SysUser;
import com.teamsync.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户角色管理（仅 SUPER_ADMIN 可用，URL 规则 + @PreAuthorize 双重控制）。
 */
@Service
public class AdminService {

    private final SysUserMapper sysUserMapper;

    public AdminService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public List<SysUser> listAllUsers() {
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
    }

    /**
     * 授予/收回"管理员"权限（ADMIN <-> MEMBER）。
     * 防呆：目标不存在；角色白名单仅 ADMIN/MEMBER；不能改自己；不能改 SUPER_ADMIN。
     */
    public void updateUserRole(Long userId, UpdateRoleDTO dto) {
        SysUser target = sysUserMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String newRole = dto.getRole();
        if (!Roles.ADMIN.equals(newRole) && !Roles.MEMBER.equals(newRole)) {
            throw new IllegalArgumentException("角色不合法，仅支持 ADMIN / MEMBER");
        }
        if (target.getId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("不能修改自己的角色");
        }
        if (Roles.SUPER_ADMIN.equals(target.getRole())) {
            throw new IllegalArgumentException("不能修改超级管理员的角色");
        }

        SysUser patch = new SysUser();
        patch.setId(target.getId());
        patch.setRole(newRole);
        sysUserMapper.updateById(patch);
    }
}
