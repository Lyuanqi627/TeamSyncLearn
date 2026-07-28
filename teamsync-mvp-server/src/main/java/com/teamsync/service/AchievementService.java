package com.teamsync.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamsync.common.UserContext;
import com.teamsync.dto.AchievementDTO;
import com.teamsync.entity.Achievement;
import com.teamsync.entity.AchievementItem;
import com.teamsync.entity.Schedule;
import com.teamsync.mapper.AchievementItemMapper;
import com.teamsync.mapper.AchievementMapper;
import com.teamsync.mapper.ScheduleMapper;

import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;

@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementMapper achievementMapper;
    private final AchievementItemMapper achievementItemMapper;
    private final ScheduleMapper scheduleMapper;
    private final AiEvaluationService aiEvaluationService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public AchievementService(AchievementMapper achievementMapper,
                              AchievementItemMapper achievementItemMapper,
                              ScheduleMapper scheduleMapper,
                              AiEvaluationService aiEvaluationService,
                              JdbcTemplate jdbcTemplate) {
        this.achievementMapper = achievementMapper;
        this.achievementItemMapper = achievementItemMapper;
         this.scheduleMapper = scheduleMapper;
        this.aiEvaluationService = aiEvaluationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadPath));
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadPath, e);
        }
        // Auto-create achievement_item table if it doesn't exist
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS achievement_item (" +
                "    id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "    achievement_id BIGINT NOT NULL," +
                "    content_type VARCHAR(20) NOT NULL," +
                "    content TEXT," +
                "    file_url VARCHAR(500)," +
                "    file_name VARCHAR(255)," +
                "    sort_order INT DEFAULT 0," +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
        } catch (Exception e) {
            log.warn("Could not auto-create achievement_item table: {}", e.getMessage());
        }
    }

    public Achievement createComposite(Long scheduleId, List<AchievementDTO.ItemDTO> items, MultipartFile[] files) throws IOException {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("日程不存在");
        }
        if (!schedule.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("无权为此日程提交成果");
        }

        // Check if an achievement already exists for this schedule (re-upload scenario)
        Achievement existing = achievementMapper.selectOne(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getScheduleId, scheduleId)
                        .orderByDesc(Achievement::getCreatedAt)
                        .last("LIMIT 1")
        );

        boolean isUpdate = existing != null;
        Achievement achievement;

        if (isUpdate) {
            achievement = existing;
            achievement.setCreatedAt(LocalDateTime.now());
            achievementMapper.updateById(achievement);
            // Delete old items
            achievementItemMapper.delete(
                    new LambdaQueryWrapper<AchievementItem>()
                            .eq(AchievementItem::getAchievementId, achievement.getId())
            );
        } else {
            achievement = new Achievement();
            achievement.setScheduleId(scheduleId);
            achievement.setCreatedAt(LocalDateTime.now());
            achievementMapper.insert(achievement);

            // Only set schedule to completed on first upload
            schedule.setStatus(2);
            scheduleMapper.updateById(schedule);
        }

        // Build a file map for easy lookup
        Map<Integer, MultipartFile> fileMap = new HashMap<>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i] != null && !files[i].isEmpty()) {
                    fileMap.put(i, files[i]);
                }
            }
        }

        // Create achievement items
        StringBuilder combinedContent = new StringBuilder();
        int sortOrder = 0;

        if (items != null) {
            for (AchievementDTO.ItemDTO item : items) {
                AchievementItem entity = new AchievementItem();
                entity.setAchievementId(achievement.getId());
                entity.setContentType(item.getContentType());
                entity.setSortOrder(sortOrder++);
                entity.setCreatedAt(LocalDateTime.now());

                if ("FILE".equals(item.getContentType())) {
                    // Check if keeping existing file from previous upload
                    if (item.getExistingFileUrl() != null && (item.getFileIndex() == null || !fileMap.containsKey(item.getFileIndex()))) {
                        entity.setFileUrl(item.getExistingFileUrl());
                        entity.setFileName(item.getFileName());
                        combinedContent.append("已上传文件: ").append(item.getFileName() != null ? item.getFileName() : "附件").append("\n");
                    } else {
                        // Look up the file by index
                        MultipartFile file = item.getFileIndex() != null ? fileMap.get(item.getFileIndex()) : null;
                        if (file != null) {
                            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                            String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
                            String fileName = IdUtil.fastSimpleUUID() + "." + ext;
                            String dirPath = uploadPath + "/" + datePath;
                            Files.createDirectories(Paths.get(dirPath));
                            String relativePath = datePath + "/" + fileName;
                            file.transferTo(new File(uploadPath + "/" + relativePath));
                            entity.setFileUrl(relativePath);
                            entity.setFileName(file.getOriginalFilename());
                            combinedContent.append("已上传文件: ").append(file.getOriginalFilename()).append("\n");
                        }
                    }
                } else {
                    entity.setContent(item.getContent());
                    if (item.getContent() != null) {
                        combinedContent.append(item.getContent()).append("\n");
                    }
                }

                achievementItemMapper.insert(entity);
            }
        }

        // Trigger async AI evaluation
        String goalContent = schedule.getGoalDesc() != null ? schedule.getGoalDesc() : schedule.getTitle();
        String achievementContent = combinedContent.toString().isBlank()
                ? "已提交成果"
                : combinedContent.toString();
        aiEvaluationService.evaluateAndSummarize(achievement.getId(), UserContext.getUserId(), goalContent, achievementContent);

        return achievement;
    }

    /**
     * Keep the old single-item method for backward compatibility
     */
    public Achievement create(Long scheduleId, String contentType, String content, MultipartFile file) throws IOException {
        List<AchievementDTO.ItemDTO> items = new ArrayList<>();
        AchievementDTO.ItemDTO item = new AchievementDTO.ItemDTO();
        item.setContentType(contentType != null ? contentType : (file != null ? "FILE" : "TEXT"));
        if (file != null) {
            item.setFileIndex(0);
        } else {
            item.setContent(content);
        }
        items.add(item);
        return createComposite(scheduleId, items, file != null ? new MultipartFile[]{file} : null);
    }

    public Achievement getByScheduleId(Long scheduleId) {
        return achievementMapper.selectOne(
                new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getScheduleId, scheduleId)
                        .orderByDesc(Achievement::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

    public List<AchievementItem> getItemsByAchievementId(Long achievementId) {
        return achievementItemMapper.selectList(
                new LambdaQueryWrapper<AchievementItem>()
                        .eq(AchievementItem::getAchievementId, achievementId)
                        .orderByAsc(AchievementItem::getSortOrder)
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
