package com.teamsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamsync.common.Roles;
import com.teamsync.common.UserContext;
import com.teamsync.dto.UpdateRoleDTO;
import com.teamsync.entity.Achievement;
import com.teamsync.entity.AchievementItem;
import com.teamsync.entity.AiResult;
import com.teamsync.entity.Schedule;
import com.teamsync.entity.SysUser;
import com.teamsync.mapper.AchievementItemMapper;
import com.teamsync.mapper.AchievementMapper;
import com.teamsync.mapper.AiResultMapper;
import com.teamsync.mapper.ScheduleMapper;
import com.teamsync.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户管理（仅 SUPER_ADMIN 可用，URL 规则 + @PreAuthorize 双重控制）：
 * 角色调整、用户列表、级联删除。
 */
@Service
public class AdminService {

    private final SysUserMapper sysUserMapper;
    private final ScheduleMapper scheduleMapper;
    private final AchievementMapper achievementMapper;
    private final AchievementItemMapper achievementItemMapper;
    private final AiResultMapper aiResultMapper;

    public AdminService(SysUserMapper sysUserMapper, ScheduleMapper scheduleMapper,
                        AchievementMapper achievementMapper, AchievementItemMapper achievementItemMapper,
                        AiResultMapper aiResultMapper) {
        this.sysUserMapper = sysUserMapper;
        this.scheduleMapper = scheduleMapper;
        this.achievementMapper = achievementMapper;
        this.achievementItemMapper = achievementItemMapper;
        this.aiResultMapper = aiResultMapper;
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

    /**
     * 删除用户并级联清除其全部业务数据（日程 → 成果 → 成果明细 / AI 评分）。
     * 防呆：目标不存在；不能删自己；不能删 SUPER_ADMIN。
     * 被删用户的已登录会话因每请求查 DB 用户不存在而立即失效（无需清理 Redis）。
     */
    public void deleteUser(Long userId) {
        SysUser target = sysUserMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (target.getId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("不能删除自己的账号");
        }
        if (Roles.SUPER_ADMIN.equals(target.getRole())) {
            throw new IllegalArgumentException("不能删除超级管理员");
        }

        List<Schedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>().eq(Schedule::getUserId, userId));
        if (!schedules.isEmpty()) {
            List<Long> scheduleIds = schedules.stream().map(Schedule::getId).toList();
            List<Achievement> achievements = achievementMapper.selectList(
                    new LambdaQueryWrapper<Achievement>().in(Achievement::getScheduleId, scheduleIds));
            if (!achievements.isEmpty()) {
                List<Long> achievementIds = achievements.stream().map(Achievement::getId).toList();
                achievementItemMapper.delete(
                        new LambdaQueryWrapper<AchievementItem>().in(AchievementItem::getAchievementId, achievementIds));
                aiResultMapper.delete(
                        new LambdaQueryWrapper<AiResult>().in(AiResult::getAchievementId, achievementIds));
            }
            achievementMapper.delete(
                    new LambdaQueryWrapper<Achievement>().in(Achievement::getScheduleId, scheduleIds));
            scheduleMapper.delete(
                    new LambdaQueryWrapper<Schedule>().eq(Schedule::getUserId, userId));
        }
        // 兜底：AI 结果表还可能直接以 userId 关联（未关联具体成果的评估记录）
        aiResultMapper.delete(new LambdaQueryWrapper<AiResult>().eq(AiResult::getUserId, userId));

        sysUserMapper.deleteById(userId);
    }
}
