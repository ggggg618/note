package com.note.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiRequest {

    @NotNull(message = "noteId不能为空")
    private Long noteId;
}
