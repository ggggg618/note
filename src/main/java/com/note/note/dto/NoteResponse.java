package com.note.note.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteResponse {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String bookName;

    private Integer visibility;

    private Integer auditStatus;

    private Integer likeCount;

    private LocalDateTime createdAt;

    private List<String> images;

    private String aiResult;
}
