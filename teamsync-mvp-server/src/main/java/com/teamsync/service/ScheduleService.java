package com.teamsync.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamsync.common.UserContext;
import com.teamsync.dto.ScheduleDTO;
import com.teamsync.entity.Achievement;
import com.teamsync.entity.AchievementItem;
import com.teamsync.entity.AiResult;
import com.teamsync.entity.Schedule;
import com.teamsync.mapper.AchievementItemMapper;
import com.teamsync.mapper.AchievementMapper;
import com.teamsync.mapper.AiResultMapper;
import com.teamsync.mapper.ScheduleMapper;
import com.teamsync.vo.AchievementItemVO;
import com.teamsync.vo.AchievementVO;
import com.teamsync.vo.AiResultVO;
import com.teamsync.vo.DashboardVO;
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

    public ScheduleService(ScheduleMapper scheduleMapper, AchievementMapper achievementMapper, AchievementItemMapper achievementItemMapper, AiResultMapper aiResultMapper) {
        this.scheduleMapper = scheduleMapper;
        this.achievementMapper = achievementMapper;
        this.achievementItemMapper = achievementItemMapper;
        this.aiResultMapper = aiResultMapper;
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
        schedule.setStatus(status);
        scheduleMapper.updateById(schedule);
    }

    public List<ScheduleVO> getUserSchedules(Long userId, LocalDate date) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId != null ? userId : UserContext.getUserId());
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
        if (userId == null) {
            userId = UserContext.getUserId();
        }
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

    private String getStatusText(int status) {
        return switch (status) {
            case 0 -> "未开始";
            case 1 -> "进行中";
            case 2 -> "已完成";
            default -> "未知";
        };
    }
}
