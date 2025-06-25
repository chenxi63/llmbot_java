package com.qianniu.llmbot.model_service;

import com.qianniu.llmbot.model_entity.BDQianFan;
import com.qianniu.llmbot.model_entity.BD_TextModel;
import com.qianniu.llmbot.product_entity.Message;
import com.qianniu.llmbot.product_service.MessageService;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*********************************************
 * 根据官方参数定义的百度千帆模型的请求方法WebClient
 * 1）将官方api key参数、timeout参数注入Header，同时设置Header中的相关字段(根据官网请求实例)；
 * 2）将请求url、基础参数注入body；查询历史记录message并构造聊天格式+最新的user prompt注入body；
 * 3）携带完整body发送请求，获得流式响应Chunk;
 * **********************************************/

@Component
public class BD_TextModelRequestService {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private final BDQianFan bdQianFan;
    private final WebClient webClient;
    private final MessageService messageService;

    @Autowired
    public BD_TextModelRequestService(BDQianFan bdQianFan, WebClient.Builder webClientBuilder, MessageService messageService) {
        this.bdQianFan = bdQianFan;
        this.webClient = configureWebClient(webClientBuilder);
        this.messageService = messageService;
    }

    //向第三方API模型的HTTP请求webClient的Header中，注入BDQianFan平台中的参数key、timeout(由yml配置读入)
    private WebClient configureWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + bdQianFan.getKey()) //注入百度千帆平台的Key
                // 配置响应超时（作用于整个WebClient实例）
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofMillis(bdQianFan.getTimeout()))  //注入百度千帆平台的TimeOut
                                .doOnConnected(conn ->
                                        conn.addHandlerLast(new ReadTimeoutHandler(bdQianFan.getTimeout(), TimeUnit.MILLISECONDS)) //双超时
                                )
                ))
                .build();
    }

    //历史message记录如果超过N条，保留最后N条，实际应该按照token数量考虑
    //注入模型的RecordNumbers固定参数(由数据库读入配置)，即最多历史记录条数，与模型的最大输入token数相关
    public List<Map<String, String>> getHistoryMessage(String conversation_id, int n) {
        // 1. 查询最新的N条消息记录，查询结果按message_id降序排列(由大到小)
        List<Message> messages = messageService.getLatestMessagesByConversationId(conversation_id, n);
        // 2. 重新调整结果按message_id升序排序(由小到大)
        messages.sort(Comparator.comparingLong(Message::getMessageId));
        // 3. 构建历史消息列表
        List<Map<String, String>> history = new ArrayList<>();

        // 遍历消息记录，添加用户和系统对话
        for (Message message : messages) {
            // 添加用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message.getQueryContent());
            history.add(userMsg);

            // 添加系统回复
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", message.getAnswerContent());
            history.add(assistantMsg);
        }
        return history;
    }

    //私有函数获得完整请求body：获取模型的基础字段的预定义参数 + "message"字段参数(最新user message+历史message)
    //注入模型的官方定义的请求参数(POST的body)，其中固定的基础参数(由数据库读入配置)，message是变化的参数需要即时传入
    private Map<String, Object> buildRequestBody(Map<String, Object>Params, List<Map<String, String>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.putAll(Params);
        requestBody.put("messages", messages);
        return requestBody;
    }

    //响应函数调用构建完整body:构建全部message，包括最新user message(userPrompt) + 历史message(chatHistory)，最后调用buildRequestBody获取完整body
    public Map<String, Object> buildCompleteRequest(Map<String, Object>Params, String userPrompt, List<Map<String, String>> chatHistory) {
        List<Map<String, String>> messages = new ArrayList<>();
        // 添加历史记录
        if (chatHistory != null) {
            messages.addAll(chatHistory);
        }
        messages.add( Map.of("role", "user", "content", userPrompt));    // 添加当前消息userPrompt
        return buildRequestBody(Params,messages); //将完整的message+基础参数形成完整的请求body
    }

    //响应函数调用：向第三方API模型的HTTP请求webClient的body和URL中，注入完整body + URL发送HTTP请求; 默认设置返回流式响应，HTTP请求中Authorization(Key)、Timeout已在初始化时预定义
    //注入bd_ES8k模型的官方定义的请求url固定参数(由数据库读入配置)
    public Flux<String> sendRequest(String url, Map<String, Object> body) {
        return webClient.post()  //POST请求
                .uri(url)   //向请求中注入文本模型的URL参数
                .bodyValue(body)  //向请求中注入body
                .retrieve()  //发送请求并获取响应
                .onStatus(    //处理错误状态码
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("API Error: " + error)))
                )
                .bodyToFlux(String.class)   //流式响应的异步方法，将响应体转换为字符串流（Flux）
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))  //最多重试 3 次,初始延迟 100ms
                        .filter(ex -> !(ex instanceof IllegalArgumentException)));
    }

}
