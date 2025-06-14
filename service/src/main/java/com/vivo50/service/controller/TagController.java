package com.vivo50.service.controller;


import com.vivo50.service.entity.Tag;
import com.vivo50.service.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/service/tag")
@CrossOrigin
@Slf4j
public class TagController {
    @Autowired
    private TagService tagService;

    @GetMapping("/user/{userId}")
    public List<Tag> getUserTags(@PathVariable String userId) {
        return tagService.getTagsByUserId(userId);
    }
}

