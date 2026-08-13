package com.teamsync.controller;

import com.teamsync.common.Result;
import com.teamsync.common.Roles;
import com.teamsync.common.UserContext;
import com.teamsync.dto.ScheduleGenerateDTO;
import com.teamsync.mapper.AiResultMapper;
import com.teamsync.entity.AiResult;
import com.teamsync.entity.Schedule;
import com.teamsync.service.ScheduleService;
import com.teamsync.vo.RecentLearningVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiResultMapper aiResultMapper;
    private final ScheduleService scheduleService;

    public AiController(AiResultMapper aiResultMapper, ScheduleService scheduleService) {
        this.aiResultMapper = aiResultMapper;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/result/{achievementId}")
    public Result<AiResult> getResult(@PathVariable Long achievementId) {
        AiResult result = aiResultMapper.selectOne(
                new LambdaQueryWrapper<AiResult>()
                        .eq(AiResult::getAchievementId, achievementId)
                        .orderByDesc(AiResult::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (result == null) {
            return Result.success(null);
        }
        // 越权保护：AI 密钥旁路（role==null）放行；管理员可只读全量；普通成员只能看自己的结果。
        String role = UserContext.getUserRole();
        if (role == null) {
            return Result.success(result);
        }
        if (!Roles.isAdmin(role) && !result.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("无权查看该AI结果");
        }
        return Result.success(result);
    }

    /**
     * 获取用户近期学习记录（供 AI 分析使用）
     * <p>
     * 返回指定用户最近 N 天的日程、成果、AI 评分等结构化数据，
     * 可直接送入大模型进行学习分析、效果评估、建议生成。
     *
     * @param userId 用户ID（不传则取当前登录用户）
     * @param days   最近多少天（默认 10）
     */
    @GetMapping("/recent-records")
    public Result<RecentLearningVO> getRecentRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") int days) {

        // 越权保护：旁路（role==null）无会话，必须显式 userId；管理员可查任意；普通成员强制查自己。
        String role = UserContext.getUserRole();
        if (role == null) {
            if (userId == null) {
                return Result.error(400, "userId 不能为空");
            }
        } else if (Roles.isAdmin(role)) {
            if (userId == null) {
                userId = UserContext.getUserId();
            }
        } else {
            userId = UserContext.getUserId();
        }
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        RecentLearningVO vo = scheduleService.getRecentLearningRecords(userId, days);
        return Result.success(vo);
    }

    /**
     * AI 生成下一日程建议并入库（对应 Dify Agent 工具 generate_next_schedule）
     * <p>
     * 将本次分析摘要、薄弱点、近期趋势整合为一条「明日日程建议」写入 schedule 表，
     * 供用户在前端日程列表中查看。planDate 不传默认明天，title 不传默认「AI日程建议」。
     *
     * @param dto userId 必填（AI 密钥调用时无会话，必须显式指定）
     */
    @PostMapping("/schedule/generate")
    public Result<?> generateSchedule(@RequestBody ScheduleGenerateDTO dto) {
        // 写 owner-only：旁路（role==null）无会话，userId 必填；会话用户只能给自己生成，不可代他人。
        String role = UserContext.getUserRole();
        Long userId;
        if (role == null) {
            userId = dto.getUserId();
            if (userId == null) {
                return Result.error(400, "userId 不能为空");
            }
        } else {
            if (dto.getUserId() != null && !dto.getUserId().equals(UserContext.getUserId())) {
                return Result.error(400, "无权为该用户生成日程");
            }
            userId = UserContext.getUserId();
            if (userId == null) {
                return Result.error(401, "userId 不能为空");
            }
        }

        String title = dto.getTitle();
        if (title == null || title.isBlank()) {
            title = "AI日程建议";
        }

        LocalDate planDate = dto.getPlanDate();
        if (planDate == null) {
            planDate = LocalDate.now().plusDays(1);
        }

        String goalDesc = buildGoalDesc(dto);
        Schedule schedule = scheduleService.createAiSchedule(userId, title, goalDesc, planDate);
        return Result.success(schedule);
    }

    /**
     * 拼装日程目标描述：分析摘要 + 薄弱点 + 趋势，非空字段才追加。
     */
    private String buildGoalDesc(ScheduleGenerateDTO dto) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "分析摘要", dto.getAnalysisSummary());
        appendSection(sb, "薄弱点", dto.getWeakPoints());
        appendSection(sb, "趋势", dto.getRecentTrends());
        return sb.length() > 0 ? sb.toString() : "AI 生成的下一日程建议";
    }

    private void appendSection(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(label).append("：").append(value);
        }
    }
}
