package com.vivo50.service.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author Konjacer
 * @create 2022-05-03 11:28
 */
public interface OssService {
    String uploadPicture(MultipartFile file);//以原名上传图片
    String uploadPictureRename(MultipartFile file,String name);//以指定名字上传图片
}
