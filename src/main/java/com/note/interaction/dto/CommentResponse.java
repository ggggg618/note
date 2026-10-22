package com.note.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;

    private Long noteId;

    private Long userId;

    private Long parentId;

    private String content;

    private LocalDateTime createdAt;

    private List<CommentResponse> children;
}
