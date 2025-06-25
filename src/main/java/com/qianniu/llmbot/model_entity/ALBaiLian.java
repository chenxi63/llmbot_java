package com.qianniu.llmbot.model_entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*********************************************
 * 阿里百炼平台API Key
 * 1）在访问阿里百炼平台的模型API时，需要注入到HTTP请求Header中，否则不允许访问；
 * 2）key参数、timeout参数、modelname_qwtb模型名称，在yml文件中配置
 * **********************************************/

//阿里百炼控制台API Key，核心字段为api key(token令牌)、timeout(超时)，yml文件中预定义
@Component
@ConfigurationProperties(prefix = "apiplatform.alibailian")
public class ALBaiLian {
    private String key;
    private int timeout;
    private String modelname_qwtb;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getModelname_qwtb() {
        return modelname_qwtb;
    }

    public void setModelname_qwtb(String modelname_qwtb) {
        this.modelname_qwtb = modelname_qwtb;
    }
}
