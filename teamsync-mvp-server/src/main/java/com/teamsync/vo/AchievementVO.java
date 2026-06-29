package com.teamsync.vo;

import java.time.LocalDateTime;
import java.util.List;

public class AchievementVO {
    private Long id;
    private Long scheduleId;
    private String contentType;
    private String content;
    private String fileUrl;
    private LocalDateTime createdAt;
    private List<AchievementItemVO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<AchievementItemVO> getItems() { return items; }
    public void setItems(List<AchievementItemVO> items) { this.items = items; }
}
