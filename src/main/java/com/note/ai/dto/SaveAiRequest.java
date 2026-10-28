package com.note.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveAiRequest {

    @NotNull(message = "noteId不能为空")
    private Long noteId;

    @NotBlank(message = "type不能为空")
    @Size(max = 30, message = "type长度不能超过30")
    @Pattern(regexp = "SUMMARY|RECOMMEND|SCORE|MIXED", message = "type必须是SUMMARY、RECOMMEND、SCORE或MIXED")
    private String type;

    @NotBlank(message = "content不能为空")
    private String content;
}
