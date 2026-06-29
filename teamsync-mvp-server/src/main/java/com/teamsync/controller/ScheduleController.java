package com.teamsync.controller;

import com.teamsync.common.Result;
import com.teamsync.dto.ScheduleDTO;
import com.teamsync.service.ScheduleService;
import com.teamsync.vo.DashboardVO;
import com.teamsync.vo.ScheduleVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public Result<?> create(@RequestBody ScheduleDTO dto) {
        return Result.success(scheduleService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody ScheduleDTO dto) {
        return Result.success(scheduleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        scheduleService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<ScheduleVO>> list(@RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(scheduleService.getUserSchedules(userId, date));
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(@RequestParam(required = false) Long userId) {
        return Result.success(scheduleService.getDashboard(userId));
    }
}
