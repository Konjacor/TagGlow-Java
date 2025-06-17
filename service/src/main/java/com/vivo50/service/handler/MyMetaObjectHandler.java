package com.vivo50.service.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.vivo50.service.service.WeatherService;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Primary
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Autowired
    private WeatherService weatherService;

    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间和修改时间
        this.setFieldValByName("gmtCreate", new Date(), metaObject);
        this.setFieldValByName("gmtModified", new Date(), metaObject);
        this.setFieldValByName("isDeleted", 0, metaObject);
        Object weatherObj = getFieldValByName("weather", metaObject);
        // 从 position 字段获取经纬度信息
        Object posObj = getFieldValByName("position", metaObject);
        if (posObj instanceof String) {
            String[] latlon = ((String) posObj).split(",");
            if (latlon.length == 2) {
                try {
                    double lat = Double.parseDouble(latlon[0].trim());
                    double lon = Double.parseDouble(latlon[1].trim());
                    String weather = weatherService.fetchWeather(lat, lon);
                    this.setFieldValByName("weather", weather, metaObject);
                } catch (NumberFormatException e) {
                    this.setFieldValByName("weather", "位置格式错误", metaObject);
                }
            } else if(weatherObj==null){
                this.setFieldValByName("weather", "位置缺失", metaObject);
            }
        } else {
            this.setFieldValByName("weather", "未知位置", metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("gmtModified", new Date(), metaObject);
    }
}
