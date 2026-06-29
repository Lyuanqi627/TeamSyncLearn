package com.teamsync.controller;

import com.teamsync.common.Result;
import com.teamsync.mapper.AiResultMapper;
import com.teamsync.entity.AiResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiResultMapper aiResultMapper;

    public AiController(AiResultMapper aiResultMapper) {
        this.aiResultMapper = aiResultMapper;
    }

    @GetMapping("/result/{achievementId}")
    public Result<AiResult> getResult(@PathVariable Long achievementId) {
        AiResult result = aiResultMapper.selectOne(
                new LambdaQueryWrapper<AiResult>()
                        .eq(AiResult::getAchievementId, achievementId)
                        .orderByDesc(AiResult::getCreatedAt)
                        .last("LIMIT 1")
        );
        return Result.success(result);
    }
}
