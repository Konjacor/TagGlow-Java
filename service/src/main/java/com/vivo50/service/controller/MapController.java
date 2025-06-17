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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @ApiOperation("根据经纬度获取地点信息")
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
    @ApiOperation("根据经纬度获取天气信息")
    @GetMapping("/getWeatherByPosition")
    public R getWeatherByPosition(@RequestParam String position) {
        try {
            MapController mapController=new MapController();
            // Step 1: 调用 reverseGeocodeByPosition 获取地理位置信息
            R locationResponse = mapController.reverseGeocodeByPosition(position);
            log.info("locationResponse: {}", locationResponse);
            if (locationResponse.getSuccess()==false ) {
                return R.error().message("获取地理位置信息失败: " + (locationResponse != null ? locationResponse.getMessage() : "响应为空"));
            }
            // Step 2: 根据经纬度直接获取天气信息
            String[] coordinates = position.split(",");
            if (coordinates.length != 2) {
                return R.error().message("位置格式错误，应为 'longitude,latitude'");
            }

            double latitude = Double.parseDouble(coordinates[1].trim());
            double longitude = Double.parseDouble(coordinates[0].trim());

            String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                    latitude, longitude
            );

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> weatherResponse = restTemplate.getForEntity(weatherUrl, Map.class);

            if (weatherResponse.getStatusCode() == HttpStatus.OK && weatherResponse.getBody() != null) {
                Map body = weatherResponse.getBody();
                Map currentWeather = (Map) body.get("current_weather");
                if (currentWeather != null) {
                    double temperature = (double) currentWeather.get("temperature");
                    int weatherCode = (int) currentWeather.get("weathercode");

                    String weatherDesc = getWeatherDescription(weatherCode);
                    String weather = String.format("%s, 温度: %.1f°C", weatherDesc, temperature);

                    // Step 3: 返回该地理位置的天气信息
                    return R.ok().data("weather:", weather);
                }
            }
            return R.error().message("无法获取天气信息");
        } catch (NumberFormatException e) {
            return R.error().message("位置格式错误，无法解析经纬度: " + e.getMessage());
        } catch (Exception e) {
            return R.error().message("获取天气信息时发生异常: " + e.getMessage());
        }
    }
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
}
