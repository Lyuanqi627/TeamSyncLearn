package com.teamsync.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * AI分析用的用户近期学习记录 VO
 * 结构与 AI 提示词对齐，方便直接送入大模型分析
 */
public class RecentLearningVO {

    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 统计周期（天数） */
    private int statsDays;
    /** 统计周期起始日 */
    private LocalDate startDate;
    /** 统计周期结束日 */
    private LocalDate endDate;

    // ── 汇总统计 ──
    private int totalSchedules;
    private int completedSchedules;
    private int pendingSchedules;
    private int inProgressSchedules;
    private Integer avgDiligenceScore;
    private int totalAchievements;

    /** 详细的日程学习记录列表 */
    private List<ScheduleRecord> records;

    // ── getter / setter ──

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getStatsDays() { return statsDays; }
    public void setStatsDays(int statsDays) { this.statsDays = statsDays; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getTotalSchedules() { return totalSchedules; }
    public void setTotalSchedules(int totalSchedules) { this.totalSchedules = totalSchedules; }
    public int getCompletedSchedules() { return completedSchedules; }
    public void setCompletedSchedules(int completedSchedules) { this.completedSchedules = completedSchedules; }
    public int getPendingSchedules() { return pendingSchedules; }
    public void setPendingSchedules(int pendingSchedules) { this.pendingSchedules = pendingSchedules; }
    public int getInProgressSchedules() { return inProgressSchedules; }
    public void setInProgressSchedules(int inProgressSchedules) { this.inProgressSchedules = inProgressSchedules; }
    public Integer getAvgDiligenceScore() { return avgDiligenceScore; }
    public void setAvgDiligenceScore(Integer avgDiligenceScore) { this.avgDiligenceScore = avgDiligenceScore; }
    public int getTotalAchievements() { return totalAchievements; }
    public void setTotalAchievements(int totalAchievements) { this.totalAchievements = totalAchievements; }
    public List<ScheduleRecord> getRecords() { return records; }
    public void setRecords(List<ScheduleRecord> records) { this.records = records; }

    // ── 内部类：单条日程学习记录 ──
    public static class ScheduleRecord {
        private Long scheduleId;
        private String title;
        private String goalDesc;
        /** 状态：0-未开始 1-进行中 2-已完成 */
        private Integer status;
        private String statusText;
        private LocalDate planDate;
        private String createdAt;

        /** 该日程提交的成果内容（文本摘要） */
        private String achievementContent;
        /** 成果类型 */
        private String contentType;
        /** AI 勤奋度评分 */
        private Integer diligenceScore;
        /** AI 点评 */
        private String aiComment;
        /** AI 总结 */
        private String aiSummary;

        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getGoalDesc() { return goalDesc; }
        public void setGoalDesc(String goalDesc) { this.goalDesc = goalDesc; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusText() { return statusText; }
        public void setStatusText(String statusText) { this.statusText = statusText; }
        public LocalDate getPlanDate() { return planDate; }
        public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getAchievementContent() { return achievementContent; }
        public void setAchievementContent(String achievementContent) { this.achievementContent = achievementContent; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Integer getDiligenceScore() { return diligenceScore; }
        public void setDiligenceScore(Integer diligenceScore) { this.diligenceScore = diligenceScore; }
        public String getAiComment() { return aiComment; }
        public void setAiComment(String aiComment) { this.aiComment = aiComment; }
        public String getAiSummary() { return aiSummary; }
        public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    }
}
