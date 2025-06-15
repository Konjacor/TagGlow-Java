package com.vivo50.service.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vivo50.common.Result.R;
import com.vivo50.service.entity.User;
import com.vivo50.service.service.OssService;
import com.vivo50.service.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author CanoeLike
 * @since 2025-06-04
 */
@RestController
@RequestMapping("/service/user")
@CrossOrigin
@Slf4j
public class UserController {
    @Autowired
    UserService userService;//注入UserService类的对象依赖，方便后面调用，主要用于对user表的增删改查

    @Autowired
    OssService ossService;//注入OssService类的对象以来，方便后面调用，主要用于上传图片

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public R login(@RequestBody User userInput){
        log.info("执行用户登录接口");
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username",userInput.getUsername());
        User userSelected = userService.getOne(wrapper);
        if(userSelected==null) return R.error().message("用户名不存在！");
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean check = passwordEncoder.matches(userInput.getPassword(),userSelected.getPassword());
        return check?R.ok().data("user",userSelected):R.error().message("密码错误！");
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public  R register(@RequestBody User user){
        log.info("执行用户注册接口");
        //先查输入的注册用户名是否存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username",user.getUsername());
        User userSelected = userService.getOne(wrapper);
        if(userSelected!=null) return R.error().message("用户名已存在！");//如果能查到输入的注册用户名，说明当前用户名不可用，直接返回错误
        //加盐+MD5加密密码并将当前用户数据存到数据库中
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
        String passwordEncoded = passwordEncoder.encode(user.getPassword());
        user.setPassword(passwordEncoded);
        boolean check = userService.save(user);
        return check?R.ok().data("user",user):R.error().message("数据添加失败！");
    }

    @ApiOperation("更新用户签名")
    @PostMapping("/updateSignature/{userId}")
    public R updateSignature(@PathVariable String userId, @RequestBody User user){
        log.info("执行更新用户签名的接口");
        User userSelected = userService.getById(userId);
        userSelected.setSignature(user.getSignature());
        boolean check = userService.updateById(userSelected);
        return check?R.ok().message("更新成功！"):R.error().message("更新失败，后端接口错误！");
    }

    @ApiOperation("更新用户头像")
    @PostMapping("/updateAvatar/{userId}")
    public R updateAvatar(@PathVariable String userId, MultipartFile file){
        log.info("执行更新用户头像的接口");
        User userSelected = userService.getById(userId);
        String url = ossService.uploadPicture(file);
        if(url==null) return R.error().message("后端图片上传失败！");
        userSelected.setAvatar(url);
        boolean check = userService.updateById(userSelected);
        return check?R.ok().message("更新成功！"):R.error().message("更新失败，后端接口错误！");
    }

    @ApiOperation("通过id获取详细用户信息")
    @GetMapping("/getUserById/{userId}")
    public R getUserById(@PathVariable String userId){
        log.info("执行通过id获取详细用户信息的接口");
        User userSelected = userService.getById(userId);
        return R.ok().data("user",userSelected);
    }
}

