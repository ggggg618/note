package com.note.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorStatsResponse {

    private long noteCount;

    private long followerCount;

    private long followingCount;
}
