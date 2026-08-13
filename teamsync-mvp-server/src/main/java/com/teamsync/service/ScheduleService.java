package com.teamsync.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamsync.common.Roles;
import com.teamsync.common.UserContext;
import com.teamsync.dto.ScheduleDTO;
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
import com.teamsync.vo.AchievementItemVO;
import com.teamsync.vo.AchievementVO;
import com.teamsync.vo.AiResultVO;
import com.teamsync.vo.DashboardVO;
import com.teamsync.vo.RecentLearningVO;
import com.teamsync.vo.ScheduleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleMapper scheduleMapper;
    private final AchievementMapper achievementMapper;
    private final AchievementItemMapper achievementItemMapper;
    private final AiResultMapper aiResultMapper;
    private final SysUserMapper sysUserMapper;

    public ScheduleService(ScheduleMapper scheduleMapper, AchievementMapper achievementMapper,
                           AchievementItemMapper achievementItemMapper, AiResultMapper aiResultMapper,
                           SysUserMapper sysUserMapper) {
        this.scheduleMapper = scheduleMapper;
        this.achievementMapper = achievementMapper;
        this.achievementItemMapper = achievementItemMapper;
        this.aiResultMapper = aiResultMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public Schedule create(ScheduleDTO dto) {
        Schedule schedule = new Schedule();
        BeanUtil.copyProperties(dto, schedule);
        schedule.setUserId(UserContext.getUserId());
        schedule.setStatus(0);
        schedule.setCreatedAt(LocalDateTime.now());
        scheduleMapper.insert(schedule);
        return schedule;
    }

    /**
     * AI 生成下一日程建议入库（不依赖登录会话，userId 显式指定）。
     * 供 Dify Agent 的 generate_next_schedule 工具调用。
     */
    public Schedule createAiSchedule(Long userId, String title, String goalDesc, LocalDate planDate) {
        Schedule schedule = new Schedule();
        schedule.setUserId(userId);
        schedule.setTitle(title);
        schedule.setGoalDesc(goalDesc);
        schedule.setPlanDate(planDate);
        schedule.setStatus(0);
        schedule.setCreatedAt(LocalDateTime.now());
        scheduleMapper.insert(schedule);
        return schedule;
    }

    public Schedule update(Long id, ScheduleDTO dto) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("日程不存在");
        }
        if (!schedule.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("无权修改此日程");
        }
        BeanUtil.copyProperties(dto, schedule);
        scheduleMapper.updateById(schedule);
        return schedule;
    }

    public void delete(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) return;
        if (!schedule.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("无权删除此日程");
        }
        scheduleMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("日程不存在");
        }
        if (!schedule.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("无权修改此日程状态");
        }
        schedule.setStatus(status);
        scheduleMapper.updateById(schedule);
    }

    public List<ScheduleVO> getUserSchedules(Long userId, LocalDate date) {
        userId = resolveReadUserId(userId);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId);
        if (date != null) {
            wrapper.eq(Schedule::getPlanDate, date);
        }
        wrapper.orderByAsc(Schedule::getPlanDate).orderByDesc(Schedule::getCreatedAt);

        List<Schedule> schedules = scheduleMapper.selectList(wrapper);
        return schedules.stream().map(this::toVO).collect(Collectors.toList());
    }

    public ScheduleVO toVO(Schedule schedule) {
        ScheduleVO vo = new ScheduleVO();
        BeanUtil.copyProperties(schedule, vo);
        vo.setStatusText(getStatusText(schedule.getStatus()));

        // Get achievement
        Achievement achievement = achievementMapper.selectOne(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getScheduleId, schedule.getId())
                        .orderByDesc(Achievement::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (achievement != null) {
            AchievementVO achievementVO = new AchievementVO();
            BeanUtil.copyProperties(achievement, achievementVO);

            // Get achievement items (gracefully handle missing table)
            try {
                List<AchievementItem> items = achievementItemMapper.selectList(
                        new LambdaQueryWrapper<AchievementItem>()
                                .eq(AchievementItem::getAchievementId, achievement.getId())
                                .orderByAsc(AchievementItem::getSortOrder)
                );
                if (!items.isEmpty()) {
                    achievementVO.setItems(items.stream().map(item -> {
                        AchievementItemVO itemVO = new AchievementItemVO();
                        BeanUtil.copyProperties(item, itemVO);
                        return itemVO;
                    }).collect(Collectors.toList()));
                }
            } catch (Exception e) {
                log.warn("Failed to query achievement items (table may not exist yet): {}", e.getMessage());
            }

            vo.setAchievement(achievementVO);

            // Get AI result
            AiResult aiResult = aiResultMapper.selectOne(
                    new LambdaQueryWrapper<AiResult>()
                            .eq(AiResult::getAchievementId, achievement.getId())
                            .last("LIMIT 1")
            );
            if (aiResult != null) {
                AiResultVO aiResultVO = new AiResultVO();
                BeanUtil.copyProperties(aiResult, aiResultVO);
                vo.setAiResult(aiResultVO);
            }
        }
        return vo;
    }

    public DashboardVO getDashboard(Long userId) {
        userId = resolveReadUserId(userId);
        DashboardVO dashboard = new DashboardVO();

        List<Schedule> allSchedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>().eq(Schedule::getUserId, userId));
        dashboard.setTotalSchedules(allSchedules.size());
        dashboard.setCompletedSchedules((int) allSchedules.stream().filter(s -> s.getStatus() == 2).count());
        dashboard.setPendingSchedules((int) allSchedules.stream().filter(s -> s.getStatus() == 0).count());

        List<Achievement> achievements = achievementMapper.selectList(
                new LambdaQueryWrapper<Achievement>()
                        .inSql(Achievement::getScheduleId,
                                "SELECT id FROM schedule WHERE user_id = " + userId)
        );
        dashboard.setTotalAchievements(achievements.size());

        // Average diligence score
        List<AiResult> aiResults = aiResultMapper.selectList(
                new LambdaQueryWrapper<AiResult>().eq(AiResult::getUserId, userId));
        dashboard.setAvgDiligenceScore((int) Math.round(aiResults.stream()
                .mapToInt(AiResult::getDiligenceScore)
                .average()
                .orElse(0)));

        // Recent week scores
        List<DashboardVO.DayScore> weekScores = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            DashboardVO.DayScore ds = new DashboardVO.DayScore();
            ds.setDate(day.toString());

            int score = (int) Math.round(aiResults.stream()
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(day))
                    .mapToInt(AiResult::getDiligenceScore)
                    .average()
                    .orElse(0));
            ds.setScore(score);
            weekScores.add(ds);
        }
        dashboard.setRecentWeekScores(weekScores);

        return dashboard;
    }

    /**
     * 获取用户近期学习记录（适合 AI 分析的结构化数据）
     *
     * @param userId 用户ID（null 则取当前登录用户）
     * @param days   统计最近多少天的记录（默认 30）
     */
    public RecentLearningVO getRecentLearningRecords(Long userId, int days) {
        if (userId == null) {
            userId = UserContext.getUserId();
        }
        if (days <= 0) days = 30;

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 获取用户名
        SysUser user = sysUserMapper.selectById(userId);
        String username = user != null ? user.getUsername() : String.valueOf(userId);

        // 查询该用户指定日期范围内的日程
        List<Schedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getUserId, userId)
                        .ge(Schedule::getPlanDate, startDate)
                        .le(Schedule::getPlanDate, endDate)
                        .orderByAsc(Schedule::getPlanDate)
                        .orderByDesc(Schedule::getCreatedAt)
        );

        // 组装 VO
        RecentLearningVO vo = new RecentLearningVO();
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setStatsDays(days);
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);

        // 统计汇总
        vo.setTotalSchedules(schedules.size());
        vo.setCompletedSchedules((int) schedules.stream().filter(s -> s.getStatus() == 2).count());
        vo.setPendingSchedules((int) schedules.stream().filter(s -> s.getStatus() == 0).count());
        vo.setInProgressSchedules((int) schedules.stream().filter(s -> s.getStatus() == 1).count());

        // 查询该用户的所有 AI 结果用于计算平均分
        List<AiResult> allAiResults = aiResultMapper.selectList(
                new LambdaQueryWrapper<AiResult>().eq(AiResult::getUserId, userId));
        vo.setAvgDiligenceScore((int) Math.round(allAiResults.stream()
                .mapToInt(AiResult::getDiligenceScore)
                .average()
                .orElse(0)));

        // 明细记录
        List<RecentLearningVO.ScheduleRecord> records = schedules.stream().map(schedule -> {
            RecentLearningVO.ScheduleRecord record = new RecentLearningVO.ScheduleRecord();
            record.setScheduleId(schedule.getId());
            record.setTitle(schedule.getTitle());
            record.setGoalDesc(schedule.getGoalDesc());
            record.setStatus(schedule.getStatus());
            record.setStatusText(getStatusText(schedule.getStatus()));
            record.setPlanDate(schedule.getPlanDate());
            record.setCreatedAt(schedule.getCreatedAt() != null ? schedule.getCreatedAt().toString() : null);

            // 查询该日程对应的成果
            Achievement achievement = achievementMapper.selectOne(
                    new LambdaQueryWrapper<Achievement>()
                            .eq(Achievement::getScheduleId, schedule.getId())
                            .orderByDesc(Achievement::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (achievement != null) {
                record.setAchievementContent(achievement.getContent());
                record.setContentType(achievement.getContentType());
                vo.setTotalAchievements(vo.getTotalAchievements() + 1);

                // 查询对应的 AI 评分
                AiResult aiResult = aiResultMapper.selectOne(
                        new LambdaQueryWrapper<AiResult>()
                                .eq(AiResult::getAchievementId, achievement.getId())
                                .last("LIMIT 1")
                );
                if (aiResult != null) {
                    record.setDiligenceScore(aiResult.getDiligenceScore());
                    record.setAiComment(aiResult.getAiComment());
                    record.setAiSummary(aiResult.getAiSummary());
                }
            }
            return record;
        }).collect(Collectors.toList());

        vo.setRecords(records);
        return vo;
    }

    /**
     * 读操作归属解析：管理员（ADMIN/SUPER_ADMIN）可查任意用户（requested 为 null 则查自己），
     * 普通成员强制查自己 —— 堵住 ?userId= 越权读取。
     */
    private Long resolveReadUserId(Long requested) {
        if (Roles.isAdmin(UserContext.getUserRole())) {
            return requested != null ? requested : UserContext.getUserId();
        }
        return UserContext.getUserId();
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 0 -> "未开始";
            case 1 -> "进行中";
            case 2 -> "已完成";
            default -> "未知";
        };
    }
}
