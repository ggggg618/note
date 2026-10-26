package com.note.book.controller;

import com.note.book.dto.BookRequest;
import com.note.book.dto.BookResponse;
import com.note.book.service.BookService;
import com.note.common.PageResult;
import com.note.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    public Result<BookResponse> create(@Valid @RequestBody BookRequest request) {
        return Result.ok(bookService.create(request));
    }

    @GetMapping
    public Result<PageResult<BookResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(bookService.list(page, size, keyword));
    }

    @PutMapping("/{id}")
    public Result<BookResponse> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return Result.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.ok();
    }
}
