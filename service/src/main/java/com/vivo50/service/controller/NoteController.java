package com.vivo50.service.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vivo50.common.Result.R;
import com.vivo50.service.entity.*;
import com.vivo50.service.service.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
@RestController
@RequestMapping("/service/note")
@CrossOrigin
@Slf4j
public class NoteController {

    @Autowired
    NoteService noteService;

    @Autowired
    TagService tagService;

    @Autowired
    NoteTagRelationService noteTagRelationService;

    @Autowired
    UserTagRelationService userTagRelationService;

    @Autowired
    VivoAiService vivoAiService;

    @ApiOperation("保存笔记提取标签保存标签")
    @PostMapping("/saveNote")
    public R saveNote(@RequestBody Note note) {
        log.info("保存笔记提取标签保存标签");
        boolean check = noteService.save(note);
        if(!check) return R.error().message("笔记保存失败");
        List<Tag> tags = vivoAiService.getTagsByContent(note.getContent());
        List<NoteTagRelation> noteTagRelations = new ArrayList<>();
        List<UserTagRelation> userTagRelations = new ArrayList<>();
        for(Tag tag : tags) {//如果对应标签-类型对已经存在，则直接记录已存在的id，如果不存在，则保存，保存后id会回写给tag对象
            QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("content", tag.getContent()).eq("type", tag.getType());
            Tag tagSelect = tagService.getOne(queryWrapper);
            if(tagSelect != null) {
                tag.setId(tagSelect.getId());
            } else {
                check = tagService.save(tag);
                if(!check) return R.error().message("标签保存失败");
            }
            NoteTagRelation noteTagRelation = new NoteTagRelation();
            noteTagRelation.setNoteId(note.getId());
            noteTagRelation.setTagId(tag.getId());
            noteTagRelations.add(noteTagRelation);
            UserTagRelation userTagRelation = new UserTagRelation();
            userTagRelation.setUserId(note.getUserId());
            userTagRelation.setTagId(tag.getId());
            userTagRelations.add(userTagRelation);
        }
        check = noteTagRelationService.saveBatch(noteTagRelations);
        if(!check) return R.error().message("笔记-标签关系保存失败");
        for(UserTagRelation userTagRelation : userTagRelations) {
            QueryWrapper<UserTagRelation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userTagRelation.getUserId()).eq("tag_id", userTagRelation.getTagId());
            UserTagRelation userTagRelationSelect = userTagRelationService.getOne(queryWrapper);
            if(userTagRelationSelect == null) {
                check = userTagRelationService.save(userTagRelation);
                if(!check) return R.error().message("用户-标签关系保存失败");
            }
        }
        return R.ok().message("保存成功");
    }

}

