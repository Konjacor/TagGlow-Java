package com.vivo50.service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WeatherService {
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchWeather(double lat, double lon) {
        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                lat, lon);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response != null && response.containsKey("current_weather")) {
            Map<String, Object> weather = (Map<String, Object>) response.get("current_weather");
            Object code = weather.get("weathercode");
            return code != null ? "天气代码：" + code.toString() : "未知天气";
        }
        return "天气获取失败";
    }
}
