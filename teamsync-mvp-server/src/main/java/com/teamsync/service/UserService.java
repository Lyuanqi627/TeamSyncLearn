package com.teamsync.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.teamsync.common.UserContext;
import com.teamsync.dto.LoginDTO;
import com.teamsync.dto.UpdateProfileDTO;
import com.teamsync.entity.SysSession;
import com.teamsync.entity.SysUser;
import com.teamsync.mapper.SysSessionMapper;
import com.teamsync.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysSessionMapper sessionMapper;

    @Value("${token.expiry:72}")
    private int tokenExpiry;

    public UserService(SysUserMapper userMapper, SysSessionMapper sessionMapper) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
    }

    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
        );

        if (user == null) {
            // Auto-register
            user = new SysUser();
            user.setUsername(dto.getUsername());
            user.setPassword(dto.getPassword());
            user.setRole("MEMBER");
            userMapper.insert(user);
        } else if (dto.getPassword() != null && !dto.getPassword().isEmpty()
                && !dto.getPassword().equals(user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        // Create session
        String token = IdUtil.fastSimpleUUID();
        SysSession session = new SysSession();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setExpireAt(DateUtil.offsetHour(new Date(), tokenExpiry));
        sessionMapper.insert(session);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("avatar", user.getAvatar());
        return result;
    }

    public void logout() {
        // clear session will be handled by token expiry
    }

    public SysUser getCurrentUser() {
        return userMapper.selectById(UserContext.getUserId());
    }

    public Map<String, Object> getCurrentUserId() {
        SysUser user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return result;
    }

    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public SysUser updateProfile(Long userId, UpdateProfileDTO dto) {
        SysUser patch = new SysUser();
        patch.setId(userId);
        // MyBatis-Plus 默认 update 策略为 NOT_NULL:null 字段不会进入 SET 子句(即不改动)
        if (dto.getAvatar() != null) patch.setAvatar(dto.getAvatar());
        if (dto.getNickname() != null) patch.setNickname(dto.getNickname());
        if (dto.getBio() != null) patch.setBio(dto.getBio());
        if (dto.getGender() != null) patch.setGender(dto.getGender());
        if (dto.getAge() != null) patch.setAge(dto.getAge());
        if (dto.getAddress() != null) patch.setAddress(dto.getAddress());
        userMapper.updateById(patch);
        return userMapper.selectById(userId);
    }
}
