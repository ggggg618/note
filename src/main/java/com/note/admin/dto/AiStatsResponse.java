package com.note.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiStatsResponse {

    private Long totalCalls;

    private Long successCalls;

    private Long failedCalls;
}
