package com.note.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.interaction.entity.NoteLike;
import com.note.interaction.dto.NoteResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {

    @Select("SELECT n.id, n.user_id, n.title, n.book_name, n.content, n.visibility, n.like_count "
            + "FROM note_like l JOIN note n ON l.note_id = n.id "
            + "WHERE l.user_id = #{userId} AND n.visibility = 1 AND n.audit_status = 1 AND n.deleted = 0 "
            + "ORDER BY l.created_at DESC, l.id DESC LIMIT #{offset}, #{size}")
    List<NoteResponse> selectLikedNotePage(@Param("userId") Long userId,
                                           @Param("offset") long offset,
                                           @Param("size") long size);

    @Select("SELECT COUNT(*) FROM note_like l JOIN note n ON l.note_id = n.id "
            + "WHERE l.user_id = #{userId} AND n.visibility = 1 AND n.audit_status = 1 AND n.deleted = 0")
    long countLikedNotes(@Param("userId") Long userId);
}
