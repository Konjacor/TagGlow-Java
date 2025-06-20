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

    @ApiOperation("保存笔记及标签，并生成AI回复")
    @PostMapping("/saveNote")
    public R saveNote(@RequestBody Note note, @RequestParam List<String> tagList) {
        log.info("保存笔记及标签：{}", note.getContent());

        // 1. 保存笔记（包括 content、userId、position、weather、classification）
        boolean check = noteService.save(note);
        if (!check) return R.error().message("笔记保存失败");

        // 2. 处理标签及关系
        List<NoteTagRelation> noteTagRelations = new ArrayList<>();
        List<UserTagRelation> userTagRelations = new ArrayList<>();

        for (String tagContent : tagList) {
            // 检查标签是否已存在
            QueryWrapper<Tag> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("content", tagContent);
            Tag tag = tagService.getOne(queryWrapper);

            if (tag == null) {
                // 不存在则创建新标签
                tag = new Tag();
                tag.setContent(tagContent);
                tag.setType(""); // 或者 "default"
                check = tagService.save(tag);
                if (!check) return R.error().message("标签保存失败");
            }

            // 建立 Note-Tag 关系
            NoteTagRelation noteTagRelation = new NoteTagRelation();
            noteTagRelation.setNoteId(note.getId());
            noteTagRelation.setTagId(tag.getId());
            noteTagRelations.add(noteTagRelation);

            // 检查并建立 User-Tag 关系（避免重复）
            QueryWrapper<UserTagRelation> userTagWrapper = new QueryWrapper<>();
            userTagWrapper.eq("user_id", note.getUserId()).eq("tag_id", tag.getId());
            if (userTagRelationService.getOne(userTagWrapper) == null) {
                UserTagRelation userTagRelation = new UserTagRelation();
                userTagRelation.setUserId(note.getUserId());
                userTagRelation.setTagId(tag.getId());
                userTagRelations.add(userTagRelation);
            }
        }

        // 批量保存关系
        if (!noteTagRelations.isEmpty() && !noteTagRelationService.saveBatch(noteTagRelations)) {
            return R.error().message("笔记-标签关系保存失败");
        }
        if (!userTagRelations.isEmpty() && !userTagRelationService.saveBatch(userTagRelations)) {
            return R.error().message("用户-标签关系保存失败");
        }

        // 3. 调用 vivoAI 回复内容（不保存，只返回）
        String aiReply;
        try {
            aiReply = vivoAiService.generateRespons(note.getContent());
            log.info("AI 回复内容：{}", aiReply);
        } catch (Exception e) {
            log.error("调用 vivoAI 失败", e);
            aiReply = "AI 回复生成失败";
        }

        // 4. 返回结果
        return R.ok().message("笔记保存成功").data("aiReply", aiReply);
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
    @ApiOperation("统计有地址的笔记数量")
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

    @ApiOperation("获取最新写的笔记")
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

    @ApiOperation("获取最新且有地址的笔记")
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
    @ApiOperation("一键生成笔记内容")
    @PostMapping("/generation")
    public R generation(@RequestParam String tags) {
        log.info("标签内容：{}" , tags);
        try {
            StringBuilder inputForModel = new StringBuilder("我会给你3-5个标签，请你生成180字内的生活片段随笔，要求：\n" +
                    "1. **文风**：像朋友发微信语音般自然\n" +
                    "2. **内容**：\n" +
                    "   - 70% 真实感受（别用\"惆怅\"\"寂寥\"等文言词，改用\"心里空落落\"等表达）\n" +
                    "   - 30% 场景白描（如#国贸写\"写字楼格子间还亮着几盏惨白的灯\"等表达方式）\n" +
                    "   (可选) 10% 希望表达（如#夜晚好冷\"突然觉得，要是这时候有人递杯热咖啡，该多好。\"）\n"+
                    "3. **禁忌**：\n" +
                    "   × 堆砌形容词 × 强行升华 × 超过180字\n" +
                    "\n" +
                    "示例：\n" +
                    "输入标签：#误车 #广州南站 #暴雨  \n" +
                    "输出：\n" +
                    "\"手机弹出晚点通知时，雨正泼在高铁站玻璃顶上。  \n" +
                    "充电口挤满焦虑的打工仔，泡面味混着潮湿的袜子味。  \n" +
                    "想起出门前我妈喊『带伞』，当时还嫌烦。  \n" +
                    "现在看着窗外模糊的霓虹灯，突然特别想吃她煮的姜汤面。"+
                    "这是下面是我的tag，按照上面提示直接给我随笔"+tags);
            // Step 4: Generate tags
            String generatedTags = vivoAiService.generateRespons(inputForModel.toString());
            // Step 7: Return validated tags
            return R.ok().message("一键生成笔记内容成功").data("NoteDefaultaitag", generatedTags);
        } catch (IllegalArgumentException e) {
            log.error("输入参数错误: {}", e.getMessage(), e);
            return R.error().message("输入参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("一键生成笔记内容失败: {}", e.getMessage(), e);
            return R.error().message("一键生成笔记内容发生异常: " + e.getMessage());
        }
    }
    @ApiOperation("生成笔记的默认AI标签")
    @PostMapping("/NoteDefaultaitag")
    public R NoteDefaultaitag(@RequestParam String userId, @RequestParam String position, @RequestParam Integer classification) {
        log.info("笔记默认AI标签生成，用户ID: {}, 位置: {}, 分类: {}", userId, position, classification);
        try {
            // Step 1: Get current time
            R timeResponse = getCurrentBeijingTime();
            String time = (String) timeResponse.getData().get("time");

            // Step 2: Get location, weather, and classification name
            MapController mapController = new MapController();
            R locationResponse = mapController.reverseGeocodeByPosition(position);
            R weatherResponse = mapController.getWeatherByPosition(position);
            String classificationName = getClassificationName(classification);

            // Step 3: Construct input for AI model
            StringBuilder inputForModel = new StringBuilder("请基于用户输入的{主题}、{位置}、{时间}、{天气}四要素生成标签，严格遵循：\n" +
                    "1. 标签格式：以#开头，中文，长度≤8字\n" +
                    "2. 数量限制：3-5个标签\n" +
                    "3. 内容维度：\n" +
                    "   - 必须包含1个时间/主题融合标签（例：#深夜加班）\n" +
                    "   - 必须包含2个地点标签：①城市级（#北京）②最小单位（#京东大厦）\n" +
                    "   - 可选1个天气关联标签（例：#暴雨通勤）\n" +
                    "   - 可选1个情绪/状态标签（例：#高效专注）\n" +
                    "4. 禁止出现：区/街道级地名、英文、标点符号\n" +
                    "\n");
            inputForModel.append("位置: ").append(locationResponse).append("\n");
            inputForModel.append("天气: ").append(weatherResponse).append("\n");
            inputForModel.append("时间: ").append(time).append("\n");
            inputForModel.append("笔记主题: ").append(classificationName).append("\n");

            // Step 4: Generate tags
            String generatedTags = vivoAiService.generateRespons(inputForModel.toString());

            // Step 5: Validate generated tags
            String validationInput = "请验证以下标签是否符合要求：" +
                    "1. 标签格式：以#开头，中文，长度≤8字\n" +
                    "2. 数量限制：3-5个标签\n" +
                    "3. 内容维度：\n" +
                    "   - 必须包含1个时间/主题融合标签\n" +
                    "   - 必须包含2个地点标签\n" +
                    "   - 可选1个天气关联标签\n" +
                    "   - 可选1个情绪/状态标签\n" +
                    "4. 禁止出现：区/街道级地名、英文、标点符号\n" +
                    "\n" +
                    "之前生成的标签: " + generatedTags +"如果有问题，请你只返回给我更好的标签\n";
            String validationResponse = vivoAiService.generateRespons(validationInput);

            // Step 6: Check validation result
            if (validationResponse.contains("不符合") || validationResponse.contains("错误")) {
                return R.error().message("生成的标签未通过验证: " + validationResponse);
            }

            // Step 7: Return validated tags
            return R.ok().message("笔记默认AI标签生成成功").data("NoteDefaultaitag", generatedTags);
        } catch (IllegalArgumentException e) {
            log.error("输入参数错误: {}", e.getMessage(), e);
            return R.error().message("输入参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("笔记默认AI标签生成失败: {}", e.getMessage(), e);
            return R.error().message("笔记默认AI标签生成时发生异常: " + e.getMessage());
        }
    }

    @ApiOperation("生成旅游攻略")
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
        StringBuilder inputForModel = new StringBuilder("以下是几篇笔记内容，请结合内容生成一篇有趣的旅游攻略，通过模仿小红书/大众点评网友那种活泼、实用、充满种草感的风格，实用信息一定要醒目。像交通方式、门票价格这些，比如地铁标志后面写线路，钱袋符号后面写费用，避免写成官方介绍那种冷冰冰的语气，多用人称代词“我”和“你”，制造对话感。比如“相信我”、“你绝对会爱上”这样的句式。开头用一个吸引眼球的标题和引言，然后分几个板块介绍亮点。每个板块都要突出“最”字——比如“最出片的地方”、“最不能错过的美食”。最后加上实用小贴士，这样信息全面又容易阅读。字数控制在350以内。：\n");
        for (Note note : notes) {
            inputForModel.append("内容: ").append(note.getContent()).append("\n");
            if (note.getPosition() != null) {
                // 调用 MapController 中的 reverseGeocodeByPosition 方法
                log.info("now1");
                String address = String.valueOf(mapController.reverseGeocodeByPosition(note.getPosition()));
                log.info("address：{}",address);
                inputForModel.append("位置: ").append(address).append("\n");

            }
            inputForModel.append("天气: ").append(note.getWeather()).append("\n");
            inputForModel.append("时间: ").append(note.getTime()).append("\n\n");
        }
        log.info("inputForModel：{}",inputForModel);
        
        // 第三步：调用 Vivo AI 模型生成旅游攻略
        String travelGuide;
        try {
            // 调用 VivoAiService 的新方法
            travelGuide = vivoAiService.generateRespons(inputForModel.toString());
        } catch (Exception e) {
            log.error("调用 Vivo AI 模型生成旅游攻略失败: {}", e.getMessage(), e);
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
    @ApiOperation("批量获取用户笔记及Tag")
    @PostMapping("/getNotesByIds")
    public R getNotesByIds(@RequestParam String userId, @RequestBody List<String> noteIds) {
        log.info("批量获取用户笔记内容，用户ID: {}, 笔记ID列表: {}", userId, noteIds);
        if (noteIds == null || noteIds.isEmpty()) {
            return R.error().message("笔记ID列表不能为空");
        }

        // 查询笔记
        QueryWrapper<Note> noteQuery = new QueryWrapper<>();
        noteQuery.eq("user_id", userId)
                .in("id", noteIds)
                .eq("is_deleted", 0);
        List<Note> notes = noteService.list(noteQuery);

        if (notes.isEmpty()) {
            return R.error().message("未找到对应的笔记");
        }

        // 提取所有笔记ID
        List<String> foundNoteIds = notes.stream()
                .map(Note::getId)
                .collect(Collectors.toList());

        // 查询笔记-标签关系
        QueryWrapper<NoteTagRelation> relQuery = new QueryWrapper<>();
        relQuery.in("note_id", foundNoteIds);
        List<NoteTagRelation> relations = noteTagRelationService.list(relQuery);

        // 提取所有 tagId
        Set<String> tagIds = relations.stream()
                .map(NoteTagRelation::getTagId)
                .collect(Collectors.toSet());

        // 查询标签信息
        List<Tag> tags = (List<Tag>) tagService.listByIds(tagIds);
        Map<String, Tag> tagMap = tags.stream()
                .collect(Collectors.toMap(Tag::getId, tag -> tag));

        // 构建 noteId -> List<Tag> 映射
        Map<String, List<Tag>> noteIdToTags = new HashMap<>();
        for (NoteTagRelation rel : relations) {
            noteIdToTags.computeIfAbsent(rel.getNoteId(), k -> new ArrayList<>())
                    .add(tagMap.get(rel.getTagId()));
        }

        // 构建结果对象列表
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Note note : notes) {
            Map<String, Object> map = new HashMap<>();
            map.put("note", note);
            map.put("tags", noteIdToTags.getOrDefault(note.getId(), Collections.emptyList()));
            resultList.add(map);
        }

        log.info("笔记及标签列表: {}", resultList);
        return R.ok().data("notes", resultList);
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


    @ApiOperation("获取用户特定分类笔记数量")
    @GetMapping("/getNoteCountByClassification")
    public R getNoteCountByClassification(@RequestParam String userId, @RequestParam int classification) {
        log.info("获取用户特定分类笔记数量，用户ID: {}, 分类: {}", userId, classification);

        QueryWrapper<Note> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("classification", classification)
                .eq("is_deleted", 0); // Ensure only non-deleted notes are counted

        int count = noteService.count(queryWrapper);

        return R.ok().data("classification", classification).data("count", count);
    }
    @ApiOperation("根据classification获取其主题名字")
    @GetMapping("/getClassificationName/{classification}")
    public String getClassificationName(Integer classification) {
        switch (classification) {
            case 0:
                return "学习";
            case 1:
                return "工作";
            case 2:
                return "日常";
            case 3:
                return "生活";
            case 4:
                return "旅行";
            case 5:
                return "情感";
            case 6:
                return "美食";
            default:
                return "未知分类";
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

