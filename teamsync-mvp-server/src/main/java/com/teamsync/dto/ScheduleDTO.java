package com.teamsync.dto;

import java.time.LocalDate;

public class ScheduleDTO {
    private String title;
    private String goalDesc;
    private LocalDate planDate;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGoalDesc() { return goalDesc; }
    public void setGoalDesc(String goalDesc) { this.goalDesc = goalDesc; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
}
