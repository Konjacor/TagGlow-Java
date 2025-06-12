package com.vivo50.service.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vivo50.common.Result.R;
import com.vivo50.service.entity.Note;
import com.vivo50.service.entity.NoteTagRelation;
import com.vivo50.service.entity.Tag;
import com.vivo50.service.entity.UserTagRelation;
import com.vivo50.service.service.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
//import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
//import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;





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

    @ApiOperation(value = "删除笔记")
    @DeleteMapping("/delete/{Id}")
    public R deleteNote(@PathVariable String Id, @RequestParam String userId) {
        System.out.println("删除笔记 ID: " + Id);
        Note note = noteService.getById(Id);
        System.out.println("查询到的笔记：" + note);
        if (note == null) {
            return R.error().message("笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            return R.error().message("你不能删除别人的笔记");
        }

        boolean result = noteService.removeById(Id);

        return result ? R.ok().message("删除成功") : R.error().message("删除失败");
    }

    @ApiOperation(value = "获取当前时间")
    @GetMapping("/time/now")
    public R getCurrentBeijingTime() {
        // 获取当前北京时间
        LocalDateTime beijingTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        String formattedTime = beijingTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        // 返回标准响应对象
        return R.ok().data("time", formattedTime);
    }

//    @GetMapping("/location")
//    @ApiOperation("根据客户端 IP 获取地理位置")
//    public R getClientLocation(HttpServletRequest request) {
//        String ip = request.getHeader("X-Forwarded-For");
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getRemoteAddr();
//        }
//        if (ip != null && ip.contains(",")) {
//            ip = ip.split(",")[0];
//        }
//        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
//            ip = "8.8.8.8"; // 本地测试用示例 IP
//        }
//
//        String apiUrl = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
//        RestTemplate rt = new RestTemplate();
//        Map<String, Object> loc = rt.getForObject(apiUrl, Map.class);
//        if (loc != null && "success".equals(loc.get("status"))) {
//            String country = loc.getOrDefault("country", "").toString();
//            String region = loc.getOrDefault("regionName", "").toString();
//            String city = loc.getOrDefault("city", "").toString();
//            String location = country + "-" + region + "-" + city;
//            return R.ok().data("ip", ip).data("location", location);
//        }
//        return R.error().message("无法定位您的IP地址对应位置");
//    }


//    @GetMapping("/getLocation")
//    public R getLocation(HttpServletRequest request) {
//        String ip = request.getRemoteAddr(); // 这里也可以换成更复杂的 IP 获取方式
//        try {
//            IpInfo info = ip2regionSearcher.memorySearch(ip);
//            String region = info.getRegion(); // e.g., 中国|天津|天津市|南开区|...
//            return R.ok().data("region", region);
//        } catch (Exception e) {
//            return R.error().message("IP 定位失败: " + e.getMessage());
//        }
//    }

    @ApiOperation("根据请求 IP 获取所在城市、区域等地理信息")
    @GetMapping("/getLocation")
    public R getLocation(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "111.30.132.206"; // 这是天津的一个电信 IP，仅用于测试
        }
        String url = "http://ip-api.com/json/" + ip + "?fields=status,message,country,regionName,city&lang=zh-CN";
        try {
            RestTemplate rt = new RestTemplate();
            Map<String, Object> resp = rt.getForObject(url, Map.class);
            if (resp != null && "success".equals(resp.get("status"))) {
                Map<String, String> data = new HashMap<>();
                data.put("country", (String) resp.get("country"));
                data.put("province", (String) resp.get("regionName"));
                data.put("city", (String) resp.get("city"));
                return R.ok().data("location", data);
            } else {
                String msg = resp != null ? resp.get("message").toString() : "未知错误";
                return R.error().message("无法获取位置: " + msg);
            }
        } catch (Exception e) {
            return R.error().message("获取地理位置异常: " + e.getMessage());
        }
    }



}

