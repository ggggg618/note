package com.note.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteAdminResponse {

    private Long id;

    private Long userId;

    private String title;

    private String bookName;

    private Integer visibility;

    private Integer auditStatus;

    private Integer likeCount;

    private LocalDateTime createdAt;
}
