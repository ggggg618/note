package com.note.book.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private Long id;

    private String title;

    private String author;

    private String publisher;

    private String isbn;

    private String description;

    private LocalDateTime createdAt;
}
