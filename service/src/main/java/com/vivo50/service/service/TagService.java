package com.vivo50.service.service;

import com.vivo50.service.entity.Tag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
public interface TagService extends IService<Tag> {
    List<Tag> getTagsByUserId(String userId);
}
