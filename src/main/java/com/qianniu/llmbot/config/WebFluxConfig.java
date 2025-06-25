package com.qianniu.llmbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.config.WebFluxConfigurer;


/****************************************************
* 客户端HTTP请求的流式响应配置
* *********************************************/


//配置HTTP 请求/响应消息的详细日志记录功能，用于调试构造流式响应返回客户端
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // 启用详细日志（可选）
        configurer.defaultCodecs().enableLoggingRequestDetails(true);
    }

}