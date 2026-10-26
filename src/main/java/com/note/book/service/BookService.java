package com.note.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.book.dto.BookRequest;
import com.note.book.dto.BookResponse;
import com.note.book.entity.Book;
import com.note.book.mapper.BookMapper;
import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;

    public BookResponse create(BookRequest request) {
        Book book = new Book();
        apply(book, request);
        book.setCreatedAt(LocalDateTime.now());
        bookMapper.insert(book);
        return toResponse(book);
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        apply(book, request);
        bookMapper.updateById(book);
        return toResponse(book);
    }

    public PageResult<BookResponse> list(long page, long size, String keyword) {
        PageUtil.check(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + escapeLike(keyword.trim()) + "%";
            wrapper.and(w -> w.like(Book::getTitle, like)
                    .or().like(Book::getAuthor, like)
                    .or().like(Book::getPublisher, like));
        }
        wrapper.orderByDesc(Book::getCreatedAt);
        Page<Book> result = bookMapper.selectPage(new Page<>(page, size), wrapper);
        List<BookResponse> records = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(result.getTotal(), page, size, records);
    }

    public void delete(Long id) {
        if (bookMapper.deleteById(id) == 0) {
            throw new BusinessException("图书不存在");
        }
    }

    private void apply(Book book, BookRequest request) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(),
                book.getPublisher(), book.getIsbn(), book.getDescription(), book.getCreatedAt());
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
