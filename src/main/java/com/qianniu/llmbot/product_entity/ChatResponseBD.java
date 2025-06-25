package com.qianniu.llmbot.product_entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/*********************************************
 * Chatbot响应函数实现对原始流式chunk的封装
 * 1）根据百度千帆平台官方定义的流式响应chunk,抽取其中的关键信息(content等)，分别封装构建自定义的Chunk,其中首、尾Chunk包含自定义信息；
 * 2）这里不能采取请求参数的自动校验，流式响应下会报错，可以在请求响应中手动进行校验，主要是校验content;
 * **********************************************/

//对原始流式响应的封装，类似DTO脱敏信息返回:对每个chunkjson进行抽取，并重新构建面向客户端返回的chunk
//******注意同名的函数不要引用错依赖包*************
public class ChatResponseBD {
    //BaseInfo
    private String botName;  //来自API响应
    private String userId; //来自请求参数
    private String userName;  //来自请求参数
    private Integer contentType = 0;   //来自请求参数

    //TokenInfo
    private Long createAT;  //来自API响应
    private Integer totalTokens;  //来自API响应
    private Integer promptTokens;   //来自API响应
    private Integer answerTokens;  //来自API响应

    private String content;  //来自API响应
    private String status;   //业务逻辑处理

    private static final ObjectMapper mapper = new ObjectMapper();


    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getContentType() { return contentType; }
    public void setContentType(Integer contentType) { this.contentType = contentType; }
    public Long getCreateAT() { return createAT; }
    public void setCreateAT(Long createAT) { this.createAT = createAT; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getAnswerTokens() { return answerTokens; }
    public void setAnswerTokens(Integer answerTokens) { this.answerTokens = answerTokens; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static String getCreatedDateTime(Long createAT) { //将Unix时间戳转换为本地时区的日期时间字符串，便于前端显式
        return Instant.ofEpochSecond(createAT)  // 关键修正：使用秒级API
                .atZone(ZoneId.of("Asia/Shanghai"))  // 明确指定时区
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));  // 自定义格式
    }

    /**将API流式响应的原始ChunkJson，结合请求参数Request进行处理，构造返回客户端的新ChunkJson**/

    // 判断是否为Last Chunk,包含finish_reason的为Last Chunk
    public boolean isLastChunk(String chunk) {
        try {
            JsonNode node = mapper.readTree(chunk);
            return node.path("choices").get(0).has("finish_reason")
                    && "stop".equals(node.path("choices").get(0).path("finish_reason").asText());
        } catch (Exception e) {
            return false;
        }
    }

    // 提取API原始ChunkJson中的某些关键字段，作为新ChunkJson的核心信息
    private JsonNode extractApiChunk(JsonNode apiNode) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", apiNode.path("id").asText());
        node.put("object", apiNode.path("object").asText());
        node.put("content", apiNode.path("choices").get(0).path("delta").path("content").asText());
        node.put("finish_reason", apiNode.path("choices").get(0).path("finish_reason").asText());
        return node;
    }

    // 构建新Chunk的FirstChunk，包含的BaseInfo+ APIChunkJson提取核心字段部分
    public String buildFirstChunk(ChatRequest request, String firstApiChunk, String botName, String uUid, String nickName) {
        try {
            JsonNode apiNode = mapper.readTree(firstApiChunk);
            ObjectNode chunk = mapper.createObjectNode();  //自定义全新的chunk,用于最终返回客户端，包含BaseInfo、APIChunkJson两部分信息

            // 向定义chunk中添加BaseInfo部分
            ObjectNode baseInfo = chunk.putObject("BaseInfo");
            baseInfo.put("botName", apiNode.path("model").isMissingNode() ?
                    botName :
                    apiNode.path("model").asText());  //注意此时的botName来自API响应中的model字段，而不是请求参数的携带的;如果响应中没有model字段，则使用请求中的botName
            baseInfo.put("userID", uUid);
            baseInfo.put("userName", nickName);

            // 向自定义chunk中添加APIChunkJson部分
            chunk.set("APIChunkJson", extractApiChunk(apiNode));

            return mapper.writeValueAsString(chunk) + "\n"; // 添加换行，符返回最终经过格式封装的chunk
        } catch (Exception e) {
            throw new RuntimeException("First Chunk 构建失败！", e);
        }

    }

    // 构建新Chunk的MiddleChunk，包含APIChunkJson提取核心字段部分
    public String buildMiddleChunk(String apiChunk) {
        try {
            ObjectNode chunk = mapper.createObjectNode();
            chunk.set("APIChunkJson", extractApiChunk(mapper.readTree(apiChunk))); //自定义chunk返回客户端，其中只包括APIChunkJson部分
            return mapper.writeValueAsString(chunk) + "\n"; // 添加换行符,返回最终经过格式封装的chunk
        } catch (Exception e) {
            throw new RuntimeException("Middle Chunk 构建失败！", e);
        }
    }

    // 构建新Chunk的LastChunk，包含APIChunkJson提取核心字段部分+TokenInfo部分
    public String buildLastChunk(String lastApiChunk) {
        try {
            JsonNode apiNode = mapper.readTree(lastApiChunk);
            ObjectNode chunk = mapper.createObjectNode();//自定义全新的chunk,用于最终返回客户端，包含APIChunkJson、TokenInfo两部分信息

            // APIChunkJson部分（提取核心字段）
            chunk.set("APIChunkJson", extractApiChunk(apiNode));

            // TokenInfo部分（从usage字段提取）
            ObjectNode tokenInfo = chunk.putObject("TokenInfo");
            if (apiNode.has("usage")) {
                JsonNode usage = apiNode.path("usage");
                tokenInfo.put("total_tokens", usage.path("total_tokens").asInt(0));  // 默认值 0
                tokenInfo.put("promptTokens", usage.path("prompt_tokens").asInt(0)); // 原 input_tokens → prompt_tokens
                tokenInfo.put("answerTokens", usage.path("completion_tokens").asInt(0)); // 原 output_tokens → completion_tokens
                tokenInfo.put("createTime", getCurrentDateTime()); // 直接使用当前时间戳
            }

            return mapper.writeValueAsString(chunk) + "\n"; // 添加换行，符返回最终经过格式封装的chunk
        } catch (Exception e) {
            throw new RuntimeException("Last Chunk构建失败!", e);
        }
    }

    // 辅助方法：获取当前时间（ISO 8601格式）
    private String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy年MM月dd日 HH:mm")
                .withZone(ZoneId.systemDefault()); // 使用系统默认时区

        return formatter.format(Instant.now());
    }

}