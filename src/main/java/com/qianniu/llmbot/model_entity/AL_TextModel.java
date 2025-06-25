package com.qianniu.llmbot.model_entity;


import com.qianniu.llmbot.product_service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/*********************************************
 * 阿里百炼平台的文本模型的官方API参数
 * 1）模型api的官方请求参数——body定义, url与api单独在Header注入
 * 2）根据模型的特征(文本模型)、输入tokens数量自定义的相关参数(非官方)；
 * 3）所有参数需要先通过modelRegister写入到数据库中，在启动时通过初始化自动读取相关参数，其中API参数Map通过转换成String后存储在model表中的"model_parameters"字段中
 * **********************************************/

//阿里文本模型实体
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) // 每次注入时创建新实例
public class AL_TextModel {
    //模型官方定义的请求携带8个基础参数，即body中的参数，由数据库读入配置
    private String model;
    private boolean stream;
    private boolean incrementalOutput;
    private double temperature;
    private double topP;
    private double repetitionPenalty;
    private String resultFormat;
    private boolean enableSearch;
    private int maxTokens;

    //模型官方定义的url，单独注入请求Header，不是body参数不需要注入body
    private String url;
    //根据官方的输入token数量自定义的参数，不需要注入body
    private int recordNumbers;

    //根据官方介绍自定义的模型类型参数(0文本-1语音-2图片-3视频)，不需要注入body
    private int contentType;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);//日志记录器，日志输出时会自动标记类名（如 UserService），便于过滤和排查问题。

    @Autowired
    ALBaiLian alBaiLian;

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

    public boolean isIncrementalOutput() {
        return incrementalOutput;
    }

    public void setIncrementalOutput(boolean incrementalOutput) {
        this.incrementalOutput = incrementalOutput;
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

    public double getRepetitionPenalty() {
        return repetitionPenalty;
    }

    public void setRepetitionPenalty(double repetitionPenalty) {
        this.repetitionPenalty = repetitionPenalty;
    }

    public String getResultFormat() {
        return resultFormat;
    }

    public void setResultFormat(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    public boolean isEnableSearch() {
        return enableSearch;
    }

    public void setEnableSearch(boolean enableSearch) {
        this.enableSearch = enableSearch;
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

    // 构造完整格式的请求基础参数（不包括 input.messages）
    public Map<String, Object> getBaseRequestParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("model", model);

        // 构造 parameters 部分
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stream", stream);
        parameters.put("incremental_output", incrementalOutput);
        parameters.put("temperature", temperature);
        parameters.put("top_p", topP);
        parameters.put("repetition_penalty", repetitionPenalty);
        parameters.put("result_format", resultFormat);
        parameters.put("enable_search", enableSearch);
        parameters.put("max_tokens", maxTokens);

        params.put("parameters", parameters);
        return params;
    }
}
