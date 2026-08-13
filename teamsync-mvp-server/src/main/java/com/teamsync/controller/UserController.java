package com.teamsync.controller;

import com.teamsync.common.Result;
import com.teamsync.common.UserContext;
import com.teamsync.dto.LoginDTO;
import com.teamsync.dto.UpdateProfileDTO;
import com.teamsync.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        userService.logout(request.getHeader("Authorization"));
        return Result.success();
    }

    @GetMapping("/info")
    public Result<?> info() {
        return Result.success(userService.getCurrentUser());
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(userService.getCurrentUserId());
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody UpdateProfileDTO dto) {
        return Result.success(userService.updateProfile(UserContext.getUserId(), dto));
    }
}
