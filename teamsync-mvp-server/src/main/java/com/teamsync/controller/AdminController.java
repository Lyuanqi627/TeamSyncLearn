package com.teamsync.controller;

import com.teamsync.common.Result;
import com.teamsync.dto.UpdateRoleDTO;
import com.teamsync.entity.SysUser;
import com.teamsync.mapper.SysUserMapper;
import com.teamsync.service.AdminService;
import com.teamsync.service.ScheduleService;
import com.teamsync.service.WordCloudService;
import com.teamsync.vo.DashboardVO;
import com.teamsync.vo.TeamBoardVO;
import com.teamsync.vo.WordCloudVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {

    private final SysUserMapper userMapper;
    private final ScheduleService scheduleService;
    private final WordCloudService wordCloudService;
    private final AdminService adminService;

    public AdminController(SysUserMapper userMapper, ScheduleService scheduleService,
                           WordCloudService wordCloudService, AdminService adminService) {
        this.userMapper = userMapper;
        this.scheduleService = scheduleService;
        this.wordCloudService = wordCloudService;
        this.adminService = adminService;
    }

    /** 用户列表（全量，含三角色）—— 仅最终管理员 */
    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<SysUser>> listUsers() {
        return Result.success(adminService.listAllUsers());
    }

    /** 调整用户角色（授予/收回 ADMIN）—— 仅最终管理员 */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> updateUserRole(@PathVariable Long userId, @RequestBody UpdateRoleDTO dto) {
        adminService.updateUserRole(userId, dto);
        return Result.success();
    }

    @GetMapping("/members")
    public Result<List<SysUser>> listMembers() {
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "MEMBER")
        );
        return Result.success(users);
    }

    @GetMapping("/teamboard")
    public Result<List<TeamBoardVO>> teamBoard() {
        List<SysUser> members = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "MEMBER")
        );

        List<TeamBoardVO> board = members.stream().map(member -> {
            DashboardVO dash = scheduleService.getDashboard(member.getId());
            TeamBoardVO vo = new TeamBoardVO();
            vo.setUserId(member.getId());
            vo.setUsername(member.getUsername());
            vo.setTotalSchedules(dash.getTotalSchedules());
            vo.setCompletedSchedules(dash.getCompletedSchedules());
            vo.setAvgDiligenceScore(dash.getAvgDiligenceScore());
            vo.setCompletionRate(dash.getTotalSchedules() > 0
                    ? (double) dash.getCompletedSchedules() / dash.getTotalSchedules() * 100 : 0);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(board);
    }

    @GetMapping("/wordcloud")
    public Result<List<WordCloudVO>> wordCloud(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        return Result.success(wordCloudService.getWordCloud(startDate, endDate));
    }

    @GetMapping("/member/{userId}")
    public Result<?> memberDetail(@PathVariable Long userId) {
        DashboardVO dashboard = scheduleService.getDashboard(userId);
        return Result.success(dashboard);
    }
}
