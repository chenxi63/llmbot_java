package com.qianniu.llmbot.product_entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*********************************************
 * Model注册Register请求的Request实体自定义各字段，用于接收新注册的model各字段，并进行自动校验；
 * 校验成功后，才能写入到数据库的完整model,自动增加主键、创建时间等参数；
 * **********************************************/

public class ModelRegisterRequest {
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    @NotNull(message = "模型类型不能为空")
    private Integer modelType;

    @NotBlank(message = "模型URL不能为空")
    @Size(max = 255, message = "模型URL长度不能超过255个字符")
    private String modelUrl;

    @NotBlank(message = "配置参数不能为空")
    private String modelParameters;  // 接收JSON字符串

    @NotBlank(message = "允许角色不能为空")
    private String modelAllowRoles;  // 接收JSON数组字符串

    // Getters and Setters
    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getModelType() {
        return modelType;
    }

    public void setModelType(Integer modelType) {
        this.modelType = modelType;
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }

    public String getModelParameters() {
        return modelParameters;
    }

    public void setModelParameters(String modelParameters) {
        this.modelParameters = modelParameters;
    }

    public String getModelAllowRoles() {
        return modelAllowRoles;
    }

    public void setModelAllowRoles(String modelAllowRoles) {
        this.modelAllowRoles = modelAllowRoles;
    }
}
