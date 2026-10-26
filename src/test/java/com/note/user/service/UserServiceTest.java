package com.note.user.service;

import com.note.user.User;
import com.note.user.UserMapper;
import com.note.user.dto.LoginRequest;
import com.note.user.dto.RegisterRequest;
import com.note.common.BusinessException;
import com.note.util.FileStorageService;
import com.note.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void clearFailures() throws Exception {
        Field field = UserService.class.getDeclaredField("LOGIN_FAILURES");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    @Test
    void registerTrimsUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  alice  ");
        request.setPassword("pass123");
        request.setNickname("Alice");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("alice", captor.getValue().getUsername());
    }

    @Test
    void loginFailureUsesTrimmedUsername() {
        when(userMapper.selectOne(any())).thenReturn(null);

        for (int i = 0; i < 5; i++) {
            LoginRequest request = new LoginRequest();
            request.setUsername("  admin  ");
            request.setPassword("wrong");
            assertThrows(BusinessException.class, () -> userService.login(request));
        }

        LoginRequest finalRequest = new LoginRequest();
        finalRequest.setUsername("admin");
        finalRequest.setPassword("wrong");
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(finalRequest));
        assertEquals(429, exception.getCode());
    }
}
