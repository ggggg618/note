package com.note.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String role;

    private Integer status;

    private LocalDateTime createdAt;
}
