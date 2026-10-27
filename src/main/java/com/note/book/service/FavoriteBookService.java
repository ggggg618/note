package com.note.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.book.dto.FavoriteBookRequest;
import com.note.book.dto.FavoriteBookResponse;
import com.note.book.entity.FavoriteBook;
import com.note.book.mapper.FavoriteBookMapper;
import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteBookService {

    private final FavoriteBookMapper favoriteBookMapper;

    public FavoriteBookResponse add(Long userId, FavoriteBookRequest request) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        FavoriteBook book = new FavoriteBook();
        book.setUserId(userId);
        book.setBookName(request.getBookName());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
        book.setCover(request.getCover());
        book.setCreatedAt(LocalDateTime.now());
        try {
            favoriteBookMapper.insert(book);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该书已收藏");
        }
        return toResponse(book);
    }

    public PageResult<FavoriteBookResponse> list(Long userId, long page, long size) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        PageUtil.check(page, size);
        Page<FavoriteBook> result = favoriteBookMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<FavoriteBook>()
                        .eq(FavoriteBook::getUserId, userId)
                        .orderByDesc(FavoriteBook::getCreatedAt));
        List<FavoriteBookResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return new PageResult<>(result.getTotal(), page, size, records);
    }

    public void delete(Long userId, Long id) {
        FavoriteBook book = favoriteBookMapper.selectById(id);
        if (book == null || !book.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除");
        }
        favoriteBookMapper.deleteById(id);
    }

    private FavoriteBookResponse toResponse(FavoriteBook book) {
        return new FavoriteBookResponse(book.getId(), book.getBookName(), book.getAuthor(),
                book.getPublisher(), book.getIsbn(), book.getDescription(), book.getCover(), book.getCreatedAt());
    }
}
