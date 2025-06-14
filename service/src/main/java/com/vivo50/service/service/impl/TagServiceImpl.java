package com.vivo50.service.service.impl;

import com.vivo50.service.entity.Tag;
import com.vivo50.service.mapper.TagMapper;
import com.vivo50.service.service.TagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
    @Override
    public List<Tag> getTagsByUserId(String userId) {
        return this.baseMapper.getTagsByUserId(userId);
    }
}
