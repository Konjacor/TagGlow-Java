package com.vivo50.common.exceptionhandle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor//生成有参数的构造器
@NoArgsConstructor//生成无参数的构造器
public class CustomerException extends RuntimeException {

    private Integer code;//状态码

    private String msg;//异常信息
}
