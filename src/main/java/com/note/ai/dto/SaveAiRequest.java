package com.note.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAiRequest {

    @NotNull(message = "noteId不能为空")
    private Long noteId;

    @NotBlank(message = "type不能为空")
    private String type;

    @NotBlank(message = "content不能为空")
    private String content;
}
