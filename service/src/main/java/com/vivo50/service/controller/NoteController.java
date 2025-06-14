package com.vivo50.service.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vivo50.common.Result.R;
import com.vivo50.service.constant.VivoAiPromptConstant;
import com.vivo50.service.entity.*;
import com.vivo50.service.service.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.filters.ExpiresFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
@Api(tags = "笔记服务")
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
    @ApiOperation("统计总笔记数量")
    @GetMapping("/totalNotes/{user_id}")
    public R getTotalNotes(@PathVariable String userId) {
        log.info("统计总笔记数量，用户ID: {}", userId);

        QueryWrapper<Note> noteWrapper = new QueryWrapper<>();

        noteWrapper.eq("user_id", userId).eq("is_deleted", 0);
        int totalNotes = noteService.count(noteWrapper);

        return R.ok().data("totalNotes", totalNotes);
    }
    @ApiOperation("统计有地址的笔记数量(用”.“来判断，若有则说明是有地址的，若没有就这个值是null)")
    @GetMapping("/notesWithAddress/{user_id}")
    public R getNotesWithAddress(@PathVariable String userId) {
        log.info("统计有地址的笔记数量，用户ID: {}", userId);
        QueryWrapper<Note> noteWrapper = new QueryWrapper<>();
        noteWrapper.eq("user_id", userId).eq("is_deleted", 0);
        List<Note> notes = noteService.list(noteWrapper);
        log.info("Fetched notes: {}", notes);
        long notesWithAddress = notes.stream()
                .filter(note -> note.getPosition() != null && note.getPosition().contains("."))
                .count();

        return R.ok().data("notesWithAddress", notesWithAddress);
    }

    @ApiOperation("获取最新写的笔记（返回笔记id）")
    @GetMapping("/latestNote/{user_id}")
    public R getLatestNote(@PathVariable String userId) {
        log.info("获取最新写的笔记，用户ID: {}", userId);
        QueryWrapper<Note> noteWrapper = new QueryWrapper<>();
        noteWrapper.eq("user_id", userId).eq("is_deleted", 0);
        List<Note> notes = noteService.list(noteWrapper);

        Note latestNote = notes.stream()
                .max((n1, n2) -> n1.getTime().compareTo(n2.getTime()))
                .orElse(null);

        return latestNote != null ? R.ok().data("latestNoteId", latestNote.getId()) : R.error().message("没有笔记");
    }

    @ApiOperation("获取最新写的且有地址的笔记（返回笔记id）")
    @GetMapping("/latestNoteWithAddress/{user_id}")
    public R getLatestNoteWithAddress(@PathVariable String userId) {
        log.info("获取最新写的且有地址的笔记，用户ID: {}", userId);

        QueryWrapper<Note> noteWrapper = new QueryWrapper<>();
        noteWrapper.eq("user_id", userId).eq("is_deleted", 0);
        List<Note> notes = noteService.list(noteWrapper);
        log.info("Fetched notes: {}", notes);
        Note latestNoteWithAddress = notes.stream()
                .filter(note -> note.getPosition() != null && note.getPosition().contains("."))
                .max((n1, n2) -> n1.getTime().compareTo(n2.getTime()))
                .orElse(null);
        log.info("Fetched notes: {}", latestNoteWithAddress);
        return latestNoteWithAddress != null ? R.ok().data("latestNoteWithAddressId", latestNoteWithAddress.getId()) : R.error().message("没有有地址的笔记");
    }
    @ApiOperation("添加新笔记")
    @PostMapping("/addNote")
    public R addNote(@RequestBody Note note) {
        log.info("添加新笔记，用户ID: {}", note.getUserId());
        try {
            // 设置创建时间和逻辑删除标志
            note.setGmtCreate(new Date());
            note.setGmtModified(new Date());
            note.setTime(new Date());
            note.setIsDeleted(0);
            log.info("now");
            boolean isSaved = noteService.save(note);
            return isSaved ? R.ok().message("笔记添加成功") : R.error().message("笔记添加失败");
        } catch (Exception e) {
            log.error("添加笔记失败: {}", e.getMessage(), e);
            return R.error().message("添加笔记时发生异常: " + e.getMessage());
        }
    }
    @ApiOperation("生成旅游攻略(未成功，等我明天改改)")
    @PostMapping("/generateTravelGuide")
    public R generateTravelGuide(@RequestParam String userId, @RequestBody List<String> noteIds) {
        log.info("生成旅游攻略，用户ID: {}, 笔记ID列表: {}", userId, noteIds);
        if (noteIds == null || noteIds.isEmpty()) {
            return R.error().message("笔记ID列表不能为空");
        }
        // 第一步：从数据库中获取笔记
        QueryWrapper<Note> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .in("id", noteIds)
                .eq("is_deleted", 0);
        List<Note> notes = noteService.list(queryWrapper);
        log.info("笔记{}", notes);
        if (notes == null || notes.isEmpty()) {
            return R.error().message("未找到对应的笔记");
        }
        MapController mapController = new MapController();
        // 第二步：为 Vivo AI 模型准备输入
        StringBuilder inputForModel = new StringBuilder("以下是几篇笔记内容，请结合内容生成一篇有趣的旅游攻略：\n");
        for (Note note : notes) {
            inputForModel.append("内容: ").append(note.getContent()).append("\n");
            if (note.getPosition() != null) {
                // 调用 MapController 中的 reverseGeocodeByPosition 方法
                log.info("now1");
                String address = String.valueOf(mapController.reverseGeocodeByPosition(note.getPosition()));
                inputForModel.append("位置: ").append(address).append("\n");
            }
            inputForModel.append("天气: ").append(note.getWeather()).append("\n");
            inputForModel.append("时间: ").append(note.getTime()).append("\n\n");
        }
        log.info("inputForModel：{}",inputForModel);
        
        // 第三步：调用 Vivo AI 模型生成旅游攻略
        String travelGuide;
        try {
            travelGuide=null;
//            String aiResponse = vivoAiService.vivogpt(inputForModel.toString(), VivoAiPromptConstant.NOTE_TO_TAGS_SYSTEM_PROMPT);
//            travelGuide = vivoAiService.getContentFromAiResponse(aiResponse);
        } catch (Exception e) {
            log.error("调用Vivo AI模型生成旅游攻略失败: {}", e.getMessage(), e);
            return R.error().message("生成旅游攻略失败: " + e.getMessage());
        }
        // 第四步：将旅游攻略字符串返回给前端
        return R.ok().message("旅游攻略生成成功").data("travelGuide", travelGuide);
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
    @ApiOperation("批量获取用户笔记内容")
    @PostMapping("/getNotesByIds")
    public R getNotesByIds(@RequestParam String userId, @RequestBody List<String> noteIds) {
        log.info("批量获取用户笔记内容，用户ID: {}, 笔记ID列表: {}", userId, noteIds);
        if (noteIds == null || noteIds.isEmpty()) {
            return R.error().message("笔记ID列表不能为空");
        }
        QueryWrapper<Note> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .in("id", noteIds)
                .eq("is_deleted", 0);
        List<Note> notes = noteService.list(queryWrapper);
        if (notes.isEmpty()) {
            return R.error().message("未找到对应的笔记");
        }
        log.info("笔记列表: {}", notes);
        return R.ok().data("notes", notes);
    }
    @ApiOperation("批量更新用户笔记内容")
    @PostMapping("/updateNotes")
    public R updateNotes(@RequestBody Map<String, Object> requestData) {
        log.info("批量更新用户笔记内容，请求数据: {}", requestData);
        // Extract data from the request body
        List<Integer> noteIds = (List<Integer>) requestData.get("noteId");
        List<String> contents = (List<String>) requestData.get("content");
        if (noteIds == null || contents == null || noteIds.isEmpty() || contents.isEmpty()) {
            return R.error().message("笔记ID和内容不能为空");
        }
        if (noteIds.size() != contents.size()) {
            return R.error().message("笔记ID和内容数量不匹配");
        }
        // Iterate through the note IDs and update each note
        for (int i = 0; i < noteIds.size(); i++) {
            Integer noteId = noteIds.get(i);
            String content = contents.get(i);
            Note note = noteService.getById(noteId);
            if (note == null) {
                return R.error().message("未找到ID为 " + noteId + " 的笔记");
            }
            // Update the note fields
            note.setContent(content);
            note.setGmtModified(new Date());
            // Save the updated note
            boolean isUpdated = noteService.updateById(note);
            if (!isUpdated) {
                return R.error().message("更新ID为 " + noteId + " 的笔记失败");
            }
        }
        return R.ok().message("批量更新笔记成功");
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



    @ApiOperation("根据IP获取天气信息，已经处理了中文转换")
    @GetMapping("/getWeather")
    public R getWeather(HttpServletRequest request) {
        double latitude = 39.12;    // 天津
        double longitude = 117.2;

        try {
            String url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                    latitude, longitude
            );

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                Map currentWeather = (Map) body.get("current_weather");
                if (currentWeather != null) {
                    double temperature = (double) currentWeather.get("temperature");
                    int weatherCode = (int) currentWeather.get("weathercode");

                    String weatherDesc = getWeatherDescription(weatherCode);
                    String result = String.format("%s", weatherDesc);

                    return R.ok().data("weather", result);
                }
            }
            return R.error().message("无法获取天气信息");
        } catch (Exception e) {
            return R.error().message("获取天气失败: " + e.getMessage());
        }
    }

    // 根据 weathercode 返回中文天气描述
    private String getWeatherDescription(int code) {
        switch (code) {
            case 0: return "晴";
            case 1: case 2: return "多云";
            case 3: return "阴";
            case 45: case 48: return "有雾";
            case 51: case 53: case 55: return "小雨";
            case 61: case 63: case 65: return "中到大雨";
            case 66: case 67: return "冻雨";
            case 71: case 73: case 75: return "小雪";
            case 77: return "阵雪";
            case 80: case 81: case 82: return "阵雨";
            case 85: case 86: return "大雪";
            case 95: return "雷雨";
            case 96: case 99: return "强雷雨";
            default: return "未知";
        }
    }


    @ApiOperation("根据用户id获取其所有笔记(测试)")
    @GetMapping("/getNotesByUserId/{userId}")
    public R saveNote(@PathVariable String userId) {
        log.info("根据用户id获取其所有笔记");
        QueryWrapper<Note> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<Note> noteList = noteService.list(wrapper);
        return R.ok().data("items",noteList);
    }

}

