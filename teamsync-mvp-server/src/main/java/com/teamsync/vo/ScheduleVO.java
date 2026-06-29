package com.teamsync.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ScheduleVO {
    private Long id;
    private String title;
    private String goalDesc;
    private Integer status;
    private LocalDate planDate;
    private LocalDateTime createdAt;
    private String statusText;
    private AchievementVO achievement;
    private AiResultVO aiResult;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGoalDesc() { return goalDesc; }
    public void setGoalDesc(String goalDesc) { this.goalDesc = goalDesc; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public AchievementVO getAchievement() { return achievement; }
    public void setAchievement(AchievementVO achievement) { this.achievement = achievement; }
    public AiResultVO getAiResult() { return aiResult; }
    public void setAiResult(AiResultVO aiResult) { this.aiResult = aiResult; }
}
