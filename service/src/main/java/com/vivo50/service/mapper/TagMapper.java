package com.vivo50.service.mapper;

import com.vivo50.service.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
public interface TagMapper extends BaseMapper<Tag> {
    @Select("SELECT t.id, t.content, t.type, t.gmt_create, t.gmt_modified, t.is_deleted " +
            "FROM tag t " +
            "JOIN user_tag_relation utr ON t.id = utr.tag_id " +
            "WHERE utr.user_id = #{userId} " +
            "AND t.is_deleted = 0 " +
            "AND utr.is_deleted = 0")
    List<Tag> getTagsByUserId(@Param("userId") String userId);
}
