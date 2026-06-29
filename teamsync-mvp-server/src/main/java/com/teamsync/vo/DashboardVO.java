package com.teamsync.vo;

import java.util.List;

public class DashboardVO {
    private int totalSchedules;
    private int completedSchedules;
    private int pendingSchedules;
    private int totalAchievements;
    private Integer avgDiligenceScore;
    private List<DayScore> recentWeekScores;

    public int getTotalSchedules() { return totalSchedules; }
    public void setTotalSchedules(int totalSchedules) { this.totalSchedules = totalSchedules; }
    public int getCompletedSchedules() { return completedSchedules; }
    public void setCompletedSchedules(int completedSchedules) { this.completedSchedules = completedSchedules; }
    public int getPendingSchedules() { return pendingSchedules; }
    public void setPendingSchedules(int pendingSchedules) { this.pendingSchedules = pendingSchedules; }
    public int getTotalAchievements() { return totalAchievements; }
    public void setTotalAchievements(int totalAchievements) { this.totalAchievements = totalAchievements; }
    public Integer getAvgDiligenceScore() { return avgDiligenceScore; }
    public void setAvgDiligenceScore(Integer avgDiligenceScore) { this.avgDiligenceScore = avgDiligenceScore; }
    public List<DayScore> getRecentWeekScores() { return recentWeekScores; }
    public void setRecentWeekScores(List<DayScore> recentWeekScores) { this.recentWeekScores = recentWeekScores; }

    public static class DayScore {
        private String date;
        private int score;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }
}
