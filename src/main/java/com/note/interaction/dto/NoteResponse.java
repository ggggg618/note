package com.note.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponse {

    private Long id;

    private String title;

    private String bookName;

    private Long likeCount;
}
