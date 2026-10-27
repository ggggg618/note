package com.note.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("favorite_book")
public class FavoriteBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String bookName;

    private String author;

    private String publisher;

    private String isbn;

    private String description;

    private String cover;

    private LocalDateTime createdAt;
}
