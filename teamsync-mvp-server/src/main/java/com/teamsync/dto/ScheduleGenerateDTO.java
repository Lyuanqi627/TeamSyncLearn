package com.teamsync.dto;

import java.time.LocalDate;

/**
 * AI 生成下一日程建议的入参（对应 Dify Agent 工具 generate_next_schedule）。
 */
public class ScheduleGenerateDTO {
    /** 用户ID（AI 密钥调用时必须显式传，会话调用可不传取当前登录用户） */
    private Long userId;
    /** 本次分析结果摘要，由 LLM 填充 */
    private String analysisSummary;
    /** 识别出的薄弱点 */
    private String weakPoints;
    /** 近期趋势描述 */
    private String recentTrends;
    /** 日程标题（可选，默认"AI日程建议"） */
    private String title;
    /** 建议日期（可选，默认明天） */
    private LocalDate planDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAnalysisSummary() { return analysisSummary; }
    public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }
    public String getWeakPoints() { return weakPoints; }
    public void setWeakPoints(String weakPoints) { this.weakPoints = weakPoints; }
    public String getRecentTrends() { return recentTrends; }
    public void setRecentTrends(String recentTrends) { this.recentTrends = recentTrends; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
}
