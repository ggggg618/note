package com.note.user.controller;

import com.note.common.Result;
import com.note.user.dto.LoginRequest;
import com.note.user.dto.LoginResponse;
import com.note.user.dto.RegisterRequest;
import com.note.user.dto.UpdateProfileRequest;
import com.note.user.dto.UserPublicResponse;
import com.note.user.dto.UserResponse;
import com.note.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/api/auth/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/api/auth/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @GetMapping("/api/user/me")
    public Result<UserResponse> getProfile() {
        return Result.ok(userService.getProfile());
    }

    @GetMapping("/api/users/{id}")
    public Result<UserPublicResponse> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUserProfile(id));
    }

    @PutMapping("/api/user/me")
    public Result<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(request));
    }

    @PostMapping("/api/user/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.uploadAvatar(file));
    }
}
