package com.qianniu.llmbot.product_entity;

import jakarta.validation.constraints.*;

/*********************************************
 * Chatbot的请求参数定义，适用所有不同的模型api
 * 1）核心参数是content，即用户输入的user prompt，用于向模型发出问题；其余参数是用户登录后返回的标准信息；
 * 2）这里不能采取请求参数的自动校验，流式响应下会报错，可以在请求响应中手动进行校验，主要是校验content;
 * **********************************************/


//请求实体，Chat功能下的Request实体定义，即客户端发送的请求参数body
//Chat响应为流式响应，需要手动校验参数，对于非用户直接输入的参数(内部构造)也可以不用校验
public class ChatRequest {
    private String modelName; //用户手动输入
    private String content; //用户手动输入(需要响应函数手动校验)
    private Integer contentType = 0;//鉴权查询结果
    private Integer isNewChat = 0; // 默认带历史记录对话
    private Integer hisMsgNumber; //前端传递的要求历史记录数量

    // Getter和Setter
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getContentType() { return contentType; }
    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public Integer getIsNewChat() { return isNewChat; }
    public void setIsNewChat(Integer isNewChat) {
        this.isNewChat = isNewChat;
    }

    public Integer getHisMsgNumber() { return hisMsgNumber; }
    public void setHisMsgNumber(Integer hisMsgNumber) {
        this.hisMsgNumber = hisMsgNumber;
    }

    // ENUM定义
    public enum ContentType {
        TEXT(0), IMAGE(1), VOICE(2), Video(3);

        private final int code;
        ContentType(int code) { this.code = code; }
        public int getCode() { return code; }
    }
}
