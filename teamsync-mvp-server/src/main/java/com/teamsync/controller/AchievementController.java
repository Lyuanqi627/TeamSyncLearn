package com.teamsync.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsync.common.Result;
import com.teamsync.dto.AchievementDTO;
import com.teamsync.entity.Achievement;
import com.teamsync.service.AchievementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/achievement")
public class AchievementController {

    private final AchievementService achievementService;
    private final ObjectMapper objectMapper;

    public AchievementController(AchievementService achievementService, ObjectMapper objectMapper) {
        this.achievementService = achievementService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/upload")
    public Result<Achievement> upload(@RequestParam Long scheduleId,
                                     @RequestParam(required = false) String items,
                                     @RequestParam(required = false) MultipartFile[] files) throws IOException {
        List<AchievementDTO.ItemDTO> itemList = null;
        if (items != null && !items.isBlank()) {
            itemList = objectMapper.readValue(items, new TypeReference<List<AchievementDTO.ItemDTO>>() {});
        }
        return Result.success(achievementService.createComposite(scheduleId, itemList, files));
    }

    @GetMapping("/bySchedule/{scheduleId}")
    public Result<Achievement> getByScheduleId(@PathVariable Long scheduleId) {
        Achievement achievement = achievementService.getByScheduleId(scheduleId);
        return Result.success(achievement);
    }
}
