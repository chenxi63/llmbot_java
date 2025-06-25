package com.qianniu.llmbot.model_entity;


import com.qianniu.llmbot.product_service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/*********************************************
 * 百度千帆平台的文本模型的官方API参数
 * 1）模型api的官方请求参数——body定义, url与api单独在Header注入
 * 2）根据模型的特征(文本模型)、输入tokens数量自定义的相关参数(非官方)；
 * 3）所有参数需要先通过modelRegister写入到数据库中，在启动时通过初始化自动读取相关参数，其中API参数Map通过转换成String后存储在model表中的"model_parameters"字段中
 * **********************************************/

//百度文本模型实体
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) // 每次注入时创建新实例
public class BD_TextModel {
    //模型官方定义的请求携带7个基础参数，即body中的参数，由数据库读入配置
    private String model;
    private boolean stream;
    private boolean includeUsage;
    private double temperature;
    private double topP;
    private double penaltyScore;
    private int maxTokens;

    //模型官方定义的url，单独注入请求Header，不是body参数不需要注入body
    private String url;
    //根据官方的输入token数量自定义的参数，不需要注入body
    private int recordNumbers;

    //根据官方介绍自定义的模型类型参数(0文本-1语音-2图片-3视频)，不需要注入body
    private int contentType;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);//日志记录器，日志输出时会自动标记类名（如 UserService），便于过滤和排查问题。

    @Autowired
    BDQianFan bdQianFan;

    // 辅助方法 - 安全获取参数值
    private String getStringParam(Map<String, Object> params, String key) {
        return params.containsKey(key) ? params.get(key).toString() : null;
    }

    private Boolean getBooleanParam(Map<String, Object> params, String key) {
        return params.containsKey(key) ? Boolean.parseBoolean(params.get(key).toString()) : null;
    }

    private Double getDoubleParam(Map<String, Object> params, String key) {
        try {
            return params.containsKey(key) ? Double.parseDouble(params.get(key).toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getIntParam(Map<String, Object> params, String key) {
        try {
            return params.containsKey(key) ? Integer.parseInt(params.get(key).toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean isIncludeUsage() {
        return includeUsage;
    }

    public void setIncludeUsage(boolean includeUsage) {
        this.includeUsage = includeUsage;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTopP() {
        return topP;
    }

    public void setTopP(double topP) {
        this.topP = topP;
    }

    public double getPenaltyScore() {
        return penaltyScore;
    }

    public void setPenaltyScore(double penaltyScore) {
        this.penaltyScore = penaltyScore;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getRecordNumbers() {
        return recordNumbers;
    }

    public void setRecordNumbers(int recordNumbers) {
        this.recordNumbers = recordNumbers;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }


    // 可以添加便捷方法获取stream_options
    public Object getStreamOptions() {
        return Map.of("include_usage", includeUsage);
    }

    //构造完整格式的请求body基础参数（不包括messages）
    public Map<String, Object> getBaseRequestParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("model", model);
        params.put("stream", stream);
        params.put("stream_options", getStreamOptions());
        params.put("temperature", temperature);
        params.put("top_p", topP);
        params.put("penalty_score", penaltyScore);
        params.put("max_tokens", maxTokens);
        return params;
    }
}
