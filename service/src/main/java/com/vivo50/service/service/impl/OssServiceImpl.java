package com.vivo50.service.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.vivo50.service.service.OssService;
import com.vivo50.service.utils.oss.ConstantPropertiesUtil;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * @author Konjacer
 * @create 2022-05-03 11:29
 */
@Service
public class OssServiceImpl implements OssService {
    @Override
    public String uploadPicture(MultipartFile file) {//上传图片
        //用工具类获取值
        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
        String endpoint = ConstantPropertiesUtil.END_POINT;
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = ConstantPropertiesUtil.ACCESS_KEY_ID;
        String accessKeySecret = ConstantPropertiesUtil.ACCESS_KEY_SECRET;
        String bucketName = ConstantPropertiesUtil.BUCKET_NAME;

        try {
            // 创建OSSClient实例。
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            //获取上传文件的输入流
            InputStream inputStream = file.getInputStream();

            //获取文件名称
            String fileName = file.getOriginalFilename();

            //1.在文件名称里面添加唯一随机值
            String uuid = UUID.randomUUID().toString().replaceAll("-","");
            fileName = uuid + fileName;

            //2.把文件按照日期进行分类
            //路径例：2021/11/17/01.jpg
            //获取当前日期(用引入的joda-time依赖)
            String datePath = new DateTime().toString("yyyy/MM/dd");
            //拼接
            fileName = datePath + "/" + fileName;

            // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
            //filename是上传到oss的文件路径和文件名称,只写文件名称的话就是放到根目录了
            ossClient.putObject(bucketName, fileName, inputStream);

            // 关闭OSSClient。
            ossClient.shutdown();

            //返回上传的路径，由于没有现成的方法，所以要手动拼接一下
            String url = "https://"+bucketName+"."+endpoint+"/"+fileName;
            return url;
        }catch (Exception e){
            e.getStackTrace();
            return null;
        }
    }

    @Override
    public String uploadPictureRename(MultipartFile file, String name) {
        //用工具类获取值
        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
        String endpoint = ConstantPropertiesUtil.END_POINT;
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = ConstantPropertiesUtil.ACCESS_KEY_ID;
        String accessKeySecret = ConstantPropertiesUtil.ACCESS_KEY_SECRET;
        String bucketName = ConstantPropertiesUtil.BUCKET_NAME;

        try {
            // 创建OSSClient实例。
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            //获取上传文件的输入流
            InputStream inputStream = file.getInputStream();

            //获取文件名称,这边做了重命名的操作,格式不对的在外面就给拦下了
            String fileName = file.getOriginalFilename();
            if(fileName.endsWith(".jpg")) name+=".jpg";
            else if(fileName.endsWith(".jpeg")) name+=".jpeg";
            else if(fileName.endsWith(".png")) name+=".png";
            else if(fileName.endsWith(".gif")) name+=".gif";
            fileName = name;

            // 设置设置 HTTP 头 里边的 Content-Type,这是为了让返回的url打开后是浏览而非下载
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(getcontentType(fileName.substring(fileName.lastIndexOf("."))));

            //1.在文件名称里面添加唯一随机值
            String uuid = UUID.randomUUID().toString().replaceAll("-","");
            fileName = uuid + fileName;

            //2.把文件按照日期进行分类
            //路径例：2021/11/17/01.jpg
            //获取当前日期(用引入的joda-time依赖)
            String datePath = new DateTime().toString("yyyy/MM/dd");
            //拼接
            fileName = datePath + "/" + fileName;

            // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
            //filename是上传到oss的文件路径和文件名称,只写文件名称的话就是放到根目录了
            ossClient.putObject(bucketName, fileName, inputStream,objectMetadata);

            // 关闭OSSClient。
            ossClient.shutdown();

            //返回上传的路径，由于没有现成的方法，所以要手动拼接一下
            String url = "https://"+bucketName+"."+endpoint+"/"+fileName;
            return url;
        }catch (Exception e){
            e.getStackTrace();
            return null;
        }
    }

    //返回应该出现的Content-Type的值，防止一访问url就下载的情况
    public static String getcontentType(String FilenameExtension) {
        if (FilenameExtension.equalsIgnoreCase(".bmp")) {
            return "image/bmp";
        }
        if (FilenameExtension.equalsIgnoreCase(".gif")) {
            return "image/gif";
        }
        if (FilenameExtension.equalsIgnoreCase(".jpeg") ||
                FilenameExtension.equalsIgnoreCase(".jpg") ||
                FilenameExtension.equalsIgnoreCase(".png")) {
            return "image/jpg";
        }
        if (FilenameExtension.equalsIgnoreCase(".html")) {
            return "text/html";
        }

        if (FilenameExtension.equalsIgnoreCase(".txt")) {
            return "text/plain";
        }
        if (FilenameExtension.equalsIgnoreCase(".vsd")) {
            return "application/vnd.visio";
        }
        if (FilenameExtension.equalsIgnoreCase(".pptx") ||
                FilenameExtension.equalsIgnoreCase(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (FilenameExtension.equalsIgnoreCase(".docx") ||
                FilenameExtension.equalsIgnoreCase(".doc")) {
            return "application/msword";
        }
        if (FilenameExtension.equalsIgnoreCase(".xml")) {
            return "text/xml";
        }
        return "image/jpg";
    }
}
