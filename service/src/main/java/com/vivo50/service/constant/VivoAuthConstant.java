package com.vivo50.service.constant;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Konjacer
 * @create 2025-06-10 20:40
 */
@Component
public class VivoAuthConstant implements InitializingBean {

    @Value("${vivo.ai.appid}")
    private String appId;

    @Value("${vivo.ai.appkey}")
    private String appKey;

    public static String APP_ID;

    public static String APP_KEY;

    @Override
    public void afterPropertiesSet() throws Exception {
        APP_ID = appId;
        APP_KEY = appKey;
    }
}
