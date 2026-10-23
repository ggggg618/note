package com.note.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FavoriteBookRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名长度不能超过100")
    private String bookName;
}
