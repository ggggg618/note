package com.note.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.interaction.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    @Select("SELECT followee_id, COUNT(*) AS cnt FROM follow "
            + "GROUP BY followee_id ORDER BY cnt DESC, followee_id ASC "
            + "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> selectHotAuthorsPage(@Param("offset") long offset,
                                                   @Param("size") long size);

    @Select("SELECT COUNT(DISTINCT followee_id) FROM follow")
    long countHotAuthors();
}
