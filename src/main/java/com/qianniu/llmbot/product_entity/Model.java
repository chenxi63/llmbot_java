package com.qianniu.llmbot.product_entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/*********************************************
 * Model实体自定义各字段，用于模型API的数据库存储
 * 1）不同平台的不同模型，共性字段独立定义；
 * 2）各模型的差异性字段，通过map转string存储在"model_parameters"字段下
 * **********************************************/

@Entity
@Table(name = "models")
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "model_name", nullable = false, unique = true, length = 100)
    private String modelName;

    @Column(name = "model_type", columnDefinition = "TINYINT DEFAULT 0")
    private Integer modelType = 0;  // 0=文本,1=图像,2=音频,3=视频

    @Column(name = "model_url", nullable = false, length = 255)
    private String modelUrl;

    @Column(name = "model_parameters")
    private String modelParameters;

    @Column(name = "model_allowroles")
    private String modelAllowroles;

    @Column(name = "created_datetime", nullable = false)
    protected String createdDatetime;

    // 枚举定义
    public enum ModelType {
        TEXT(0), IMAGE(1), AUDIO(2), VIDEO(3);

        private final int value;

        ModelType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ModelType fromValue(int value) {
            for (ModelType type : ModelType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid model type value: " + value);
        }
    }

    // Getters and Setters
    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

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

    public String getModelAllowroles() {
        return modelAllowroles;
    }

    public void setModelAllowroles(String modelAllowroles) {
        this.modelAllowroles = modelAllowroles;
    }

    public String getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(String createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    // 辅助方法：获取枚举类型
    public ModelType getModelTypeEnum() {
        return ModelType.fromValue(modelType);
    }

    public void setModelTypeEnum(ModelType modelType) {
        this.modelType = modelType.getValue();
    }

    // equals, hashCode 和 toString 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model model = (Model) o;
        return Objects.equals(modelId, model.modelId) &&
                Objects.equals(modelName, model.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, modelName);
    }

    @Override
    public String toString() {
        return "Model{" +
                "modelId=" + modelId +
                ", modelName='" + modelName + '\'' +
                ", modelType=" + modelType +
                ", modelUrl='" + modelUrl + '\'' +
                ", modelParameters='" + modelParameters + '\'' +
                ", modelAllowroles='" + modelAllowroles + '\'' +
                ", createdDatetime='" + createdDatetime + '\'' +
                '}';
    }
}