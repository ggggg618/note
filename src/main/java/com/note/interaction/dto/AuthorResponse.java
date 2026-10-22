package com.note.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Long noteCount;

    private Long followerCount;
}
