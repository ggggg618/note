package com.note.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FavoriteBookRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名长度不能超过100")
    private String bookName;

    @Size(max = 100, message = "作者长度不能超过100")
    private String author;

    @Size(max = 100, message = "出版社长度不能超过100")
    private String publisher;

    @Size(max = 50, message = "ISBN长度不能超过50")
    private String isbn;

    @Size(max = 2000, message = "简介长度不能超过2000")
    private String description;
}
