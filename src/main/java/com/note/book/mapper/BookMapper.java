package com.note.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
