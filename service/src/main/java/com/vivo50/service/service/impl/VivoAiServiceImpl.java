package com.vivo50.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vivo50.service.constant.VivoAiPromptConstant;
import com.vivo50.service.constant.VivoAuthConstant;
import com.vivo50.service.entity.Tag;
import com.vivo50.service.service.VivoAiService;
import com.vivo50.service.utils.vivo.VivoAuth;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

// import com.sun.javafx.collections.MappingChange;

/**
 * @author Konjacer
 * @create 2025-06-10 20:54
 */
@Service
public class VivoAiServiceImpl implements VivoAiService {

    @Override
    public List<Tag> getTagsByContent(String content) {
        try {
            String aiResponse = vivogpt(VivoAiPromptConstant.NOTE_TO_TAGS_PROMPT +content,VivoAiPromptConstant.NOTE_TO_TAGS_SYSTEM_PROMPT);
            String result = getContentFromAiResponse(aiResponse);
            String[] stringTagTypes = result.split(",");
            List<Tag> tags = new ArrayList<>();
            for (String stringTagType : stringTagTypes) {
                String[] tagType = stringTagType.split(":");
                if (tagType.length < 2) {
                    // 跳过格式错误的标签，如 "水果"
                    continue;
                }
                Tag tag = new Tag();
                tag.setContent(tagType[0]);
                tag.setType(tagType[1]);
                tags.add(tag);
            }
            return tags;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    //生成旅游攻略的
    public String generateTravelGuide(String inputForModel) {
        try {
            // 调用 vivogpt 方法生成 AI 响应
            String aiResponse = vivogpt(inputForModel, VivoAiPromptConstant.NOTE_TO_TRAVEL_GUIDE_SYSTEM_PROMPT);
            // 从 AI 响应中提取内容
            return getContentFromAiResponse(aiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // 或者抛出自定义异常
        }
    }


    private String getContentFromAiResponse(String aiResponse) throws JsonProcessingException {
        // 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 将 JSON 字符串转换为 Map
        Map<String, Object> map = objectMapper.readValue(aiResponse, Map.class);
        return (String) ((Map) map.get("data")).get("content");
    }


    private String vivogpt(String prompt,String systemPrompt) throws Exception {

        String appId = VivoAuthConstant.APP_ID;
        String appKey = VivoAuthConstant.APP_KEY;
        String URI = "/vivogpt/completions";
        String DOMAIN = "api-ai.vivo.com.cn";
        String METHOD = "POST";
        UUID requestId = UUID.randomUUID();
//        System.out.println("requestId: " + requestId);


        Map<String, Object> map = new HashMap<>();
        map.put("requestId", requestId.toString());
        String queryStr = mapToQueryString(map);

        //构建请求体
        Map<String, String> data = new LinkedHashMap<>();
        data.put("prompt", prompt);
        data.put("model", "vivo-BlueLM-TB-Pro");
        UUID sessionId = UUID.randomUUID();
        data.put("sessionId", sessionId.toString());
//        System.out.println(sessionId);
        data.put("systemPrompt", systemPrompt);


        HttpHeaders headers = VivoAuth.generateAuthHeaders(appId, appKey, METHOD, URI, queryStr);
        headers.add("Content-Type", "application/json");
//        System.out.println(headers);
        String url = String.format("http://%s%s?%s", DOMAIN, URI, queryStr);
        String requsetBodyString = new ObjectMapper().writeValueAsString(data);
//        System.out.println("requsetBodyString: " + requsetBodyString);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);//让头中自动包含charset=UTF-8，不然中文传过去是乱码，ai读取不到。
        httpHeaders.addAll(headers);
        HttpEntity<String> requestEntity = new HttpEntity<>(requsetBodyString, httpHeaders);
//        System.out.println(requestEntity.getBody());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Response body: " + response.getBody());
        } else {
            System.out.println("Error response: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private static String mapToQueryString(Map<String, Object> map) {
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (queryStringBuilder.length() > 0) {
                queryStringBuilder.append("&");
            }
            queryStringBuilder.append(entry.getKey());
            queryStringBuilder.append("=");
            queryStringBuilder.append(entry.getValue());
        }
        return queryStringBuilder.toString();
    }

}
