package com.vivo50.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vivo50.common.Result.R;
import com.vivo50.service.entity.Note;
import com.vivo50.service.service.NoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
@RestController
@RequestMapping("/service/note")
@CrossOrigin
@Slf4j
@Api(tags = "笔记服务")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @ApiOperation("获取用户首页统计信息（统计总笔记数量、有地址的总笔记数量、最新笔记日期、最新且有地址的笔记日期）")
    @GetMapping("/homeStats/{userId}")
    public R getHomeStats(@PathVariable String userId) {
        log.info("获取用户首页统计信息，用户ID: {}", userId);

        // 查询用户的所有笔记（排除已删除的）
        QueryWrapper<Note> noteWrapper = new QueryWrapper<>();
        noteWrapper.eq("user_id", userId).eq("is_deleted", 0);
        List<Note> notes = noteService.list(noteWrapper);

        // 统计总笔记数量
        int totalNotes = notes.size();

        // 统计有地址的笔记数量
        long notesWithAddress = notes.stream()
                .filter(note -> note.getPosition() != null && !note.getPosition().isEmpty())
                .count();

        // 获取最新写的笔记
        Note latestNote = notes.stream()
                .max((n1, n2) -> n1.getTime().compareTo(n2.getTime()))
                .orElse(null);

        // 获取最新写的且有地址的笔记
        Note latestNoteWithAddress = notes.stream()
                .filter(note -> note.getPosition() != null && !note.getPosition().isEmpty())
                .max((n1, n2) -> n1.getTime().compareTo(n2.getTime()))
                .orElse(null);

        // 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("totalNotes", totalNotes);
        response.put("notesWithAddress", notesWithAddress);
        response.put("latestNote", latestNote);
        response.put("latestNoteWithAddress", latestNoteWithAddress);

        return R.ok().data(response);
    }

    @ApiOperation("添加新笔记（简单）")
    @PostMapping("/addNote")
    public R addNote(@RequestBody Note note) {
        log.info("添加新笔记，用户ID: {}", note.getUserId());

        // 设置创建时间和逻辑删除标志
        note.setGmtCreate(new Date());
        note.setGmtModified(new Date());
        note.setIsDeleted(0);

        boolean isSaved = noteService.save(note);
        return isSaved ? R.ok().message("笔记添加成功") : R.error().message("笔记添加失败");
    }
}