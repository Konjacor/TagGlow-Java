package com.vivo50.common.exceptionhandle;


import com.vivo50.common.Result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //指定出现什么异常会执行这个方法
    @ExceptionHandler(Exception.class)
    @ResponseBody//为了返回数据
    public R error(Exception e) {
        e.printStackTrace();
        return R.error().message("执行了全局异常处理..");
    }

//    //特定异常
//    //指定出现什么异常会执行这个方法
//    @ExceptionHandler(ArithmeticException.class)
//    @ResponseBody//为了返回数据
//    public R error(ArithmeticException e) {
//        e.printStackTrace();
//        return R.error().message("执行了ArithmeticException异常处理..");
//    }

    @ExceptionHandler(CustomerException.class)
    @ResponseBody//为了返回数据
    public R error(CustomerException e) {
        e.printStackTrace();
        log.error(e.getMessage());
        return R.error().code(e.getCode()).message(e.getMsg());
    }

}
