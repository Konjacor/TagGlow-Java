## DB下载

- 下载MySQL5.7

## DBMS下载

- 通过网盘分享的文件：navicat15
  链接: https://pan.baidu.com/s/101YOzbl97bXq5P5xCzU3zQ?pwd=d2yh 提取码: d2yh
- 下载好后可以在里面配置前面下好的MySQL5.7，配置好后就可以用图形化界面操作数据库了。
- 配置好后可以用现有的sql文件创建数据库，sql文件在\sqls\tagglow.sql

## 业务逻辑代码编写

- 基本的业务逻辑代码在/service/src/main/java/com/vivo50/service/controller/中编写，其中/service/src/main/java/com/vivo50/service/controller/UserController.java中有示例代码，代码风格尽量保持一致，尽量写好注释和日志输出，注意返回值类型是统一的返回类。

## 配置文件

- 位置在\service\src\main\resources\application.properties中

## 运行服务

- 运行\service\src\main\java\com\vivo50\service\ServiceApplication.java即可

## 接口测试

- 运行服务后，在浏览器url栏中输入：http://localhost:端口号/swagger-ui.html后可访问接口测试界面，在里面可以测试接口运行的效果。

## AI服务

- AI服务接口在src/main/java/com/vivo50/service/service/VivoAiService.java，在这里面写关于AI服务的service接口，实现类在src/main/java/com/vivo50/service/service/impl/VivoAiServiceImpl.java，在这里面实现AI服务的service接口。
- AI服务的prompt和systemPrompt(人设)作为常量在src/main/java/com/vivo50/service/constant/VivoAiPromptConstant.java中统一管理。
- 在写controller层接口时可以注入VivoAiService来使用AI服务的service层接口功能。
- 示例在src/main/java/com/vivo50/service/controller/NoteController.java中的saveNote方法。

## AI服务

- AI服务接口在src/main/java/com/vivo50/service/service/VivoAiService.java，在这里面写关于AI服务的service接口，实现类在src/main/java/com/vivo50/service/service/impl/VivoAiServiceImpl.java，在这里面实现AI服务的service接口。
- AI服务的prompt和systemPrompt(人设)作为常量在src/main/java/com/vivo50/service/constant/VivoAiPromptConstant.java中统一管理。
- 在写controller层接口时可以注入VivoAiService来使用AI服务的service层接口功能。
- 示例在src/main/java/com/vivo50/service/controller/NoteController.java中的saveNote方法。