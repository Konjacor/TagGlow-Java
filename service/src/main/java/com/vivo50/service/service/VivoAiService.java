package com.vivo50.service.service;

import com.vivo50.service.entity.Tag;

import java.util.List;

/**
 * @author Konjacer
 * @create 2025-06-10 20:51
 * 和vivo的ai相关的service方法
 */
public interface VivoAiService {

    List<Tag> getTagsByContent(String content);


    String generateTravelGuide(String toString);
}
