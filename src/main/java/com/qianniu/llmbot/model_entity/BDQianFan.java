package com.qianniu.llmbot.model_entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*********************************************
 * 百度千帆平台API Key
 * 1）在访问百度千帆平台的模型API时，需要注入到HTTP请求Header中，否则不允许访问；
 * 2）key参数、timeout参数、modelname_es8k模型名称参数、modelname_es128k模型名称参数，在yml文件中配置
 * **********************************************/

//百度千帆控制台实体，核心字段为api key(token令牌)、timeout(超时)，yml文件中预定义
@Component
@ConfigurationProperties(prefix = "apiplatform.baiduqianfan")
public class BDQianFan {
    private String key;
    private int timeout;
    private String modelname_es8k;
    private String modelname_es128k;


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

    public String getModelname_es8k() {
        return modelname_es8k;
    }

    public void setModelname_es8k(String modelname_es8k) {
        this.modelname_es8k = modelname_es8k;
    }

    public String getModelname_es128k() {
        return modelname_es128k;
    }

    public void setModelname_es128k(String modelname_es128k) {
        this.modelname_es128k = modelname_es128k;
    }

}
