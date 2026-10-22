package com.note.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.common.AuthUser;
import com.note.common.BusinessException;
import com.note.common.UserContext;
import com.note.user.User;
import com.note.user.UserMapper;
import com.note.user.dto.LoginRequest;
import com.note.user.dto.LoginResponse;
import com.note.user.dto.RegisterRequest;
import com.note.user.dto.UpdateProfileRequest;
import com.note.user.dto.UserResponse;
import com.note.util.FileStorageService;
import com.note.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;

    public UserResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        return toUserResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, toUserResponse(user));
    }

    public UserResponse getProfile() {
        User user = requireCurrentUser();
        return toUserResponse(user);
    }

    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = requireCurrentUser();
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        userMapper.updateById(user);
        return toUserResponse(user);
    }

    public String uploadAvatar(MultipartFile file) {
        User user = requireCurrentUser();
        String url = fileStorageService.store(file);
        user.setAvatar(url);
        userMapper.updateById(user);
        return url;
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
