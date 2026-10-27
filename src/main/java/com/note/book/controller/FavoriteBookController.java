package com.note.book.controller;

import com.note.book.dto.FavoriteBookRequest;
import com.note.book.dto.FavoriteBookResponse;
import com.note.book.dto.BookRecognizeResponse;
import com.note.book.service.FavoriteBookService;
import com.note.ai.service.AiClient;
import com.note.common.PageResult;
import com.note.common.Result;
import com.note.common.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class FavoriteBookController {

    private final FavoriteBookService favoriteBookService;
    private final AiClient aiClient;

    @PostMapping
    public Result<FavoriteBookResponse> add(@Valid @RequestBody FavoriteBookRequest request) {
        return Result.ok(favoriteBookService.add(UserContext.id(), request));
    }

    @GetMapping
    public Result<PageResult<FavoriteBookResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(favoriteBookService.list(UserContext.id(), page, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        favoriteBookService.delete(UserContext.id(), id);
        return Result.ok();
    }

    @PostMapping("/recognize")
    public Result<BookRecognizeResponse> recognize(@RequestParam("file") MultipartFile file) throws Exception {
        Map<String, String> info = aiClient.recognizeBook(file.getBytes(), file.getContentType());
        return Result.ok(new BookRecognizeResponse(info.get("title"), info.get("author"), info.get("description"), null));
    }
}
