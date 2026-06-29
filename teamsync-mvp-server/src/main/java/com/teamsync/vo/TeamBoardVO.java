package com.teamsync.vo;

public class TeamBoardVO {
    private Long userId;
    private String username;
    private int totalSchedules;
    private int completedSchedules;
    private Integer avgDiligenceScore;
    private double completionRate;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getTotalSchedules() { return totalSchedules; }
    public void setTotalSchedules(int totalSchedules) { this.totalSchedules = totalSchedules; }
    public int getCompletedSchedules() { return completedSchedules; }
    public void setCompletedSchedules(int completedSchedules) { this.completedSchedules = completedSchedules; }
    public Integer getAvgDiligenceScore() { return avgDiligenceScore; }
    public void setAvgDiligenceScore(Integer avgDiligenceScore) { this.avgDiligenceScore = avgDiligenceScore; }
    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
}
