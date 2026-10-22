package com.note.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_log")
public class AiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private String prompt;

    private String response;

    private Integer success;

    private LocalDateTime createdAt;
}
