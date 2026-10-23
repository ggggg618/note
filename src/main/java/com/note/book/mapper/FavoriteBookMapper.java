package com.note.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.book.entity.FavoriteBook;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteBookMapper extends BaseMapper<FavoriteBook> {
}
