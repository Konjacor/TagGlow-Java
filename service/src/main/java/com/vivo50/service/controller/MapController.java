package com.vivo50.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vivo50.common.Result.R;
import com.vivo50.service.entity.Note;
import com.vivo50.service.entity.Picture;
import com.vivo50.service.service.NoteService;
import com.vivo50.service.service.PictureService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/service/map")
@CrossOrigin
@Slf4j
@Api(tags = "地图服务")
public class MapController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private PictureService pictureService;

    // 从配置文件中获取天地图API密钥
    @Value("${tianditu.api.key}")
    private String tiandituApiKey;
    @ApiOperation("获取用户笔记地图标记")
    @GetMapping("/mapMarkers")
    public R getMapMarkers(@RequestParam String userId, @RequestParam(required = false, defaultValue = "false") boolean includeImages) {
        log.info("获取用户笔记地图标记，用户ID: {}", userId);

        // 查询用户的所有笔记（排除已删除的）
        QueryWrapper<Note> noteWrapper = new QueryWrapper<>();
        noteWrapper.eq("user_id", userId)
                .eq("is_deleted", 0);
        List<Note> notes = noteService.list(noteWrapper);

        // 处理无笔记情况
        if (notes.isEmpty()) {
            return R.error().message("不存在笔记，请先开始一个笔记记录");
        }

        // 处理有笔记但无位置信息情况
        List<Note> notesWithPosition = notes.stream()
                .filter(note -> note.getPosition() != null && !note.getPosition().isEmpty())
                .collect(Collectors.toList());

        if (notesWithPosition.isEmpty()) {
            return R.ok().data("hasMarkers", false)
                    .data("message", "您的笔记没有地理位置信息");
        }

        // 构建地图标记数据
        List<Map<String, Object>> markers = notesWithPosition.stream().map(note -> {
            Map<String, Object> marker = new HashMap<>();
            marker.put("id", note.getId());
            marker.put("title", "笔记位置");

            // 解析位置信息 (格式: 经度,纬度)
            String[] coords = note.getPosition().split(",");
            if (coords.length == 2) {
                marker.put("longitude", Double.parseDouble(coords[0]));
                marker.put("latitude", Double.parseDouble(coords[1]));
            }

            // 添加笔记基本信息
            marker.put("content", note.getContent());
            marker.put("weather", note.getWeather());
            marker.put("time", note.getTime().getTime()); // 时间戳

            // 根据参数决定是否包含图片信息
            if (includeImages) {
                QueryWrapper<Picture> pictureWrapper = new QueryWrapper<>();
                pictureWrapper.eq("note_id", note.getId())
                        .eq("is_deleted", 0);
                List<Picture> pictures = pictureService.list(pictureWrapper);

                if (!pictures.isEmpty()) {
                    marker.put("imageUrl", pictures.get(0).getContent());
                }
            }

            return marker;
        }).collect(Collectors.toList());

        // 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("hasMarkers", true);
        response.put("markers", markers);
        response.put("tiandituApiKey", tiandituApiKey);

        return R.ok().data(response);
    }
    @ApiOperation("获取单个笔记的详细信息")
    @GetMapping("/noteDetail/{noteId}")
    public R getNoteDetail(@PathVariable String noteId) {
        Note note = noteService.getById(noteId);
        if (note == null) {
            return R.error().message("笔记不存在");
        }

        // 获取关联图片
        QueryWrapper<Picture> pictureWrapper = new QueryWrapper<>();
        pictureWrapper.eq("note_id", noteId)
                .eq("is_deleted", 0);
        List<Picture> pictures = pictureService.list(pictureWrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("note", note);
        response.put("pictures", pictures);

        return R.ok().data(response);
    }
    @ApiOperation("获取天地图图层服务URL模板（简易地图内容）")
    @GetMapping("/layerService")
    public R getLayerService() {
        log.info("获取天地图图层服务URL模板");

        // 天地图图层服务URL模板
        Map<String, String> layerUrls = new HashMap<>();
        layerUrls.put("vector", "http://t{0-7}.tianditu.gov.cn/DataServer?T=vec_w&x={x}&y={y}&l={z}&tk=" + tiandituApiKey);
        layerUrls.put("satellite", "http://t{0-7}.tianditu.gov.cn/DataServer?T=img_w&x={x}&y={y}&l={z}&tk=" + tiandituApiKey);
        layerUrls.put("terrain", "http://t{0-7}.tianditu.gov.cn/DataServer?T=ter_w&x={x}&y={y}&l={z}&tk=" + tiandituApiKey);

        return R.ok().data("layerUrls", layerUrls);
    }
    @ApiOperation("前端传来设备经纬度，后端根据经纬度进行逆地理编码（天地图的）")
    @GetMapping("/reverseGeocodeByPosition")
    public R reverseGeocodeByPosition(@RequestParam String position) {
        try {
            position = position.replace("\"", "");
            String[] coordinates = position.split(",");
            log.info("now,string0:{},string1:{}", coordinates[0], coordinates[1]);
            if (coordinates.length != 2) {
                return R.error().message("位置格式错误，应为 'longitude,latitude'");
            }

            double longitude = Double.parseDouble(coordinates[0].trim());
            double latitude = Double.parseDouble(coordinates[1].trim());
            log.info("now,double0:{},double1:{}", longitude, latitude);

            // 手动构建 JSON 字符串
            String postStr = String.format("{\"lon\":%f,\"lat\":%f,\"ver\":1}", longitude, latitude);
            String tianDiTuUrl = String.format(
                    "http://api.tianditu.gov.cn/geocoder?postStr=%s&type=geocode&tk=cb3168e2f21d5f5569a6a8a1cc92c1a9",
                    URLEncoder.encode(postStr, StandardCharsets.UTF_8.toString())
            );

            log.info("Constructed URL: {}", tianDiTuUrl);

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(tianDiTuUrl, Map.class);
            log.info("Response: {}", response);

            if (response != null && response.containsKey("result")) {
                return R.ok().data("location", response.get("result"));
            } else {
                return R.error().message("无法从天地图获取位置信息");
            }
        } catch (NumberFormatException e) {
            return R.error().message("位置格式错误，无法解析经纬度: " + e.getMessage());
        } catch (Exception e) {
            return R.error().message("获取位置信息失败: " + e.getMessage());
        }
    }
}
