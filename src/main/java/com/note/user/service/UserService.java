package com.note.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.common.AuthUser;
import com.note.common.BusinessException;
import com.note.common.UserContext;
import com.note.user.User;
import com.note.user.UserMapper;
import com.note.user.dto.LoginRequest;
import com.note.user.dto.LoginResponse;
import com.note.user.dto.ChangePasswordRequest;
import com.note.user.dto.RegisterRequest;
import com.note.user.dto.UpdateProfileRequest;
import com.note.user.dto.UserPublicResponse;
import com.note.user.dto.UserResponse;
import com.note.util.FileStorageService;
import com.note.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_MS = 15 * 60 * 1000L;
    private static final Map<String, long[]> LOGIN_FAILURES = new ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;

    public UserResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername().trim())
        );
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setStatus(1);
        user.setTokenVersion(0);
        userMapper.insert(user);
        log.info("用户注册成功: username={}", user.getUsername());

        return toUserResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        long now = System.currentTimeMillis();
        String username = request.getUsername().trim();
        long[] failure = LOGIN_FAILURES.get(username);
        if (failure != null && failure[1] > now && failure[0] >= MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException(429, "尝试次数过多，请15分钟后再试");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFailure(username, now);
            log.warn("用户登录失败: username={}", username);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("禁用账号尝试登录: username={}", username);
            throw new BusinessException("账号已被禁用");
        }

        LOGIN_FAILURES.remove(username);
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        return new LoginResponse(token, toUserResponse(user));
    }

    private void recordLoginFailure(String username, long now) {
        long[] failure = LOGIN_FAILURES.computeIfAbsent(username, k -> new long[2]);
        if (failure[1] <= now) {
            failure[0] = 0;
            failure[1] = now + LOCK_MS;
        }
        failure[0]++;
    }

    public UserResponse getProfile() {
        User user = requireCurrentUser();
        return toUserResponse(user);
    }

    public UserPublicResponse getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new UserPublicResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = requireCurrentUser();
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname());
        }
        userMapper.updateById(user);
        return toUserResponse(user);
    }

    public String uploadAvatar(MultipartFile file) {
        User user = requireCurrentUser();
        String url = fileStorageService.storeImage(file);
        if (user.getAvatar() != null) {
            fileStorageService.delete(user.getAvatar());
        }
        user.setAvatar(url);
        userMapper.updateById(user);
        log.info("头像上传成功: userId={}, url={}", user.getId(), url);
        return url;
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = requireCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userMapper.updateById(user);
        log.info("用户修改密码: userId={}", user.getId());
    }

    private User requireCurrentUser() {
        AuthUser current = UserContext.get();
        if (current == null || current.getId() == null) {
            throw new BusinessException("请先登录");
        }

        User user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole()
        );
    }
}
