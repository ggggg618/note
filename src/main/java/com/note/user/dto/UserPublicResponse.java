package com.note.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;
}
