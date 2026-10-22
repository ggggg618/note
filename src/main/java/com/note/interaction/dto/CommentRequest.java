package com.note.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull(message = "noteId不能为空")
    private Long noteId;

    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
