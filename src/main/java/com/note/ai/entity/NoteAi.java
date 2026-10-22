package com.note.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note_ai")
public class NoteAi {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private String type;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
