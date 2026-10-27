package com.note.book.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRecognizeResponse {

    private String title;

    private String author;

    private String description;

    private String cover;
}
