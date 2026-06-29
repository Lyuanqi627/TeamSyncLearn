package com.teamsync.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.teamsync.common.UserContext;
import com.teamsync.dto.LoginDTO;
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

    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
