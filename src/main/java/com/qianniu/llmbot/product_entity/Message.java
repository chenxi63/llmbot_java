package com.qianniu.llmbot.product_entity;

import jakarta.persistence.*;

import java.util.Objects;

/*********************************************
 * Message实体自定义各字段，用于聊天记录的数据库存储
 * 1）Message将问-答封装在一起，形成一条message；
 * 2）其中user_id与users表的uuid字段形成外键约束，即只有user_id先在users表中的uuid存在，用户先注册登录才能再聊天并存储记录；
 * 3）开发阶段，可以暂时停用外键约束，以便message能够自由写入；
 * **********************************************/

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "bot_name", nullable = false, length = 100)
    private String botName;

    @Column(name = "user_id", nullable = false)
    private String userId;  // 与user表中id一致

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;  // 与user表中name一致

    @Column(name = "conversation_id", nullable = false, length = 200)
    private String conversationId; // 格式: botname_userid

    @Column(name = "total_token_number")
    private Integer totalTokenNumber = 0;  // total的token数量

    // 查询部分
    @Column(name = "query_content", nullable = false, columnDefinition = "TEXT")
    private String queryContent;

    @Column(name = "query_content_type", nullable = false)
    private Integer queryContentType = 0;  // 0=文本,1=图片,2=语音

    @Column(name = "query_token_number")
    private Integer queryTokenNumber = 0;  // query的token数量

    // 回答部分
    @Column(name = "answer_content", nullable = false, columnDefinition = "TEXT")
    private String answerContent;

    @Column(name = "answer_content_type", nullable = false)
    private Integer answerContentType = 0;  // 0=文本,1=图片,2=语音，3=视频

    @Column(name = "answer_token_number")
    private Integer answerTokenNumber = 0;  // answer的token数量

    @Column(name = "created_at", nullable = false) // Unix时间戳(秒)
    private Long createdAt = 0L;

    // Getters and Setters
    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getTotalTokenNumber() {
        return totalTokenNumber;
    }

    public void setTotalTokenNumber(Integer totalTokenNumber) {
        this.totalTokenNumber = totalTokenNumber;
    }

    public String getQueryContent() {
        return queryContent;
    }

    public void setQueryContent(String queryContent) {
        this.queryContent = queryContent;
    }

    public Integer getQueryContentType() {
        return queryContentType;
    }

    public void setQueryContentType(Integer queryContentType) {
        this.queryContentType = queryContentType;
    }

    public Integer getQueryTokenNumber() {
        return queryTokenNumber;
    }

    public void setQueryTokenNumber(Integer queryTokenNumber) {
        this.queryTokenNumber = queryTokenNumber;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public Integer getAnswerContentType() {
        return answerContentType;
    }

    public void setAnswerContentType(Integer answerContentType) {
        this.answerContentType = answerContentType;
    }

    public Integer getAnswerTokenNumber() {
        return answerTokenNumber;
    }

    public void setAnswerTokenNumber(Integer answerTokenNumber) {
        this.answerTokenNumber = answerTokenNumber;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    // 内容类型枚举
    public enum ContentType {
        TEXT(0), IMAGE(1), AUDIO(2), VIDEO(3);

        private final int value;

        ContentType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ContentType fromValue(int value) {
            for (ContentType type : ContentType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid content type value: " + value);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Message[messageId=%s, botName=%s, userId=%s, userName=%s, conversationId=%s, totalTokenNumber=%s," +
                        "queryContent=%s, queryContentType=%s, queryTokenNumber=%s, " +
                        "answerContent=%s, answerContentType=%s, answerTokenNumber=%s, createdAt=%s]",
                messageId, botName, userId, userName, conversationId, totalTokenNumber,
                queryContent, queryContentType, queryTokenNumber,
                answerContent, answerContentType, answerTokenNumber, createdAt
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(messageId, message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}
