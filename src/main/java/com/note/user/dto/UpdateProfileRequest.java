package com.note.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;

    private String avatar;
}
