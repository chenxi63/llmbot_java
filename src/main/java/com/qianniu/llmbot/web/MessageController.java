package com.qianniu.llmbot.web;


import com.qianniu.llmbot.product_entity.Message;
import com.qianniu.llmbot.product_service.MessageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;  // 导入 List 接口
import java.time.LocalDateTime;
import java.util.Map;

/*********************************************
 * Message相关的各种请求响应函数定义
 * 1）message所有相关的请求响应均需要鉴权，即登录成功后携带jwt token；除此外查询响应，还需要权限等级校验(已在Filter过滤器中定义)；
 * **********************************************/

@RestController
@RequestMapping(value = "/api/message") //基础路径为 "/api/message"，即后续所有的路径都自动添加 "/api/message/xxxxx"
@Validated // 启用方法级参数校验，即GET请求校验，POST请求在响应函数使用@RequestBody @Valid
public class MessageController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private final MessageService messageService; //相比@Autowired注入组件，可避免运行时被修改，生产环境适用
    public  MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    //按照message_id查询message,url中附带id参数；指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbymsgid?id=5，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbymsgid",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByMsgId(@RequestParam @NotNull long id) {
        try {
            Message message = messageService.getMessageByMessageId(id);
            return ResponseEntity.ok(message);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Message not found with message_id: " + id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An error occurred while fetching the message: " + e.getMessage()));
        }
    }

    //按照bot_name查询message,url中附带name参数；指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbybotname?name=ernie-speed-128k，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbybotname", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByBotName(@RequestParam @NotBlank(message = "bot_name不能为空") @Size(max = 100, message = "bot_name长度不能超过100个字符") String name) {
        try {
            List<Message> messages = messageService.getMessageByBotName(name);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "No messages found for bot: " + name,
                                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }

    //按照user_id查询message,url中附带id参数;指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbyuserid?id=xxx，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyuserid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByUserID(@RequestParam @NotBlank(message = "user_id不能为空") String id)
    {
        try {
            List<Message> messages = messageService.getMessageByUserId(id);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with user_id: " + id,
                                "timestamp",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }

    //按照conversation_id查询message,url中附带id参数; 指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbycvsnid?id=ernie-speed-128k_xxx，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbycvsnid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByConversationID(@RequestParam @NotBlank(message = "conversation_id不能为空") @Size(max = 200, message = "conversation_id长度不能超过200个字符")String id)
    {
        try {
            List<Message> messages = messageService.getMessageByConversationId(id);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with conversation_id: " + id,
                                "timestamp",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }

    //按照total_token_number查询message,url中附带number参数;指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbyttnumber?number=500，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyttnumber", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByTotalTokenNumber(@RequestParam @NotNull(message = "total_token_number不能为空") int number)
    {
        try {
            List<Message> messages = messageService.getMessageByUpTotalTokenNumber(number);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with total_token_number greater than: " + number,
                                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }

    //按照query_token_number查询message,url中附带id参数;指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbyqynumber?number=20，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyqynumber", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByQueryTokenNumber(@RequestParam @NotNull(message = "query_token_number不能为空") int number)
    {
        try {
            List<Message> messages = messageService.getMessageByUpQueryTokenNumber(number);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with query_token_number greater than: " + number,
                                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }

    //按照answer_token_number查询message,url中附带number参数;指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbyawnumber?number=500，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyawnumber", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByAnswerTokenNumber(@RequestParam @NotNull(message = "answer_token_number不能为空") int number)
    {
        try {
            List<Message> messages = messageService.getMessageByUpAnswerTokenNumber(number);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with query_token_number greater than: " + number,
                                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }

    //按照conversation_id查询最新的N条message,url中附带id参数、N参数;指名该路径为非流式响应，全局配置更改为默认流式响应
    //localhost:8080/api/message/getbycvsnid+n?id=ernie-speed-128k_1&n=2，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbycvsnid+n", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMessageByConversationIDAndLatestN(@RequestParam @NotBlank(message = "conversation_id不能为空") @Size(max = 200, message = "conversation_id长度不能超过200个字符")String id, @RequestParam @NotNull(message = "最新记录条数N不能为空")int n)
    {
        try {
            List<Message> messages = messageService.getLatestMessagesByConversationId(id, n);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with conversation_id:" + id + " + Latest Number of: " + n,
                                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", messages,
                    "count", messages.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }


    // 选择模型时即加载对应历史记录，只需登录即可不需要会员角色
    // 按照conversation_id查询最新的N条message,url中附带id参数，按照前端需求的格式提取后返回客户端；N默认为5条,即前端最多显式5条历史记录，后端实际带记录数量由各model在数据库中参数定义;
    // localhost:8080/api/message/gethismsg?id=ernie-speed-128k_9a981217-5d3b-44f1-97fe-d545fbfbd6a8，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @GetMapping(value = "/gethismsg", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getHidMsg(@RequestParam @NotBlank(message = "conversation_id不能为空") @Size(max = 200, message = "conversation_id长度不能超过200个字符")String id)
    {
        int recordNumber = 5;//前端默认显式5轮历史对话记录

        try {
            List<Message> messages = messageService.getLatestMessagesByConversationId(id, recordNumber);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", HttpStatus.NOT_FOUND.value(),
                                "message", "Message not found with conversation_id:" + id + " + Latest Number of: " + recordNumber,
                                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        ));
            }

            // 先按时间戳升序排序（确保最旧的消息在最前面）
            messages.sort(Comparator.comparingLong(Message::getCreatedAt));

            // 将每条消息拆分为一问一答两条记录
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            for (Message message : messages) {
                // 添加用户提问
                formattedMessages.add(Map.of(
                        "senderby", message.getUserName(),
                        "text", message.getQueryContent()
                ));

                // 添加机器人回答
                formattedMessages.add(Map.of(
                        "senderby", message.getBotName(),
                        "text", message.getAnswerContent()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "data", formattedMessages,
                    "count", formattedMessages.size(), // 现在是10条（5问5答）
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "An error occurred while fetching messages: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    ));
        }
    }
}
