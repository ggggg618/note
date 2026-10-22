package com.note.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("note_image")
public class NoteImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private String url;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
