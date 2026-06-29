package com.teamsync.controller;

import com.teamsync.common.Result;
import com.teamsync.dto.LoginDTO;
import com.teamsync.service.UserService;
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
    public Result<?> logout() {
        userService.logout();
        return Result.success();
    }

    @GetMapping("/info")
    public Result<?> info() {
        return Result.success(userService.getCurrentUser());
    }
}
