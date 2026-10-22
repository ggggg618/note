package com.note.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.note.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
