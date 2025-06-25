package com.qianniu.llmbot.product_service;

import com.qianniu.llmbot.product_entity.Message;
import com.qianniu.llmbot.product_entity.User;
import jakarta.persistence.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*********************************************
 * Message Service中用户message的相关行为方法定义
 * 1) 新message注册，注册函数方法仅内部使用，即Chat响应函数内调用，外部无法调用(未在Controller中定义请求响应函数方法)；
 * 2)按照各字段查询message;
 * **********************************************/

@Component
@Transactional
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);//日志记录器，日志输出时会自动标记类名（如 UserService），便于过滤和排查问题。

    @Autowired
    JdbcTemplate jdbcTemplate;

    RowMapper<Message> messageRowMapper = new BeanPropertyRowMapper<>(Message.class);//将数据库查询结果集ResultSet的每一行自动映射到Message实体实例中

    //按照messageId查询单个message，queryForObject只用于处理单行结果
    public Message getMessageByMessageId(Long message_id) {
        return jdbcTemplate.queryForObject("SELECT * FROM messages WHERE message_id = ?", messageRowMapper, message_id);
    }

    //按照botName查询多个message，query处理多行结果
    public List<Message> getMessageByBotName(String bot_name) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE bot_name = ?", messageRowMapper, bot_name);
    }

    //按照userId查询多个message，query处理多行结果
    public List<Message> getMessageByUserId(String user_id) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE user_id = ?", messageRowMapper, user_id);
    }

    //按照conversationId查询多个message，query处理多行结果,botname_userid
    public List<Message> getMessageByConversationId(String conversation_id) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE conversation_id = ?", messageRowMapper, conversation_id);
    }

    //按照queryContentType查询多个message，query处理多行结果,0=文本,1=图片,2=语音
    public List<Message> getMessageByQueryContentType(Integer query_content_type) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE query_content_type = ?", messageRowMapper, query_content_type);
    }

    //按照answerContentType查询多个message，query处理多行结果,0=文本,1=图片,2=语音
    public List<Message> getMessageByAnswerContentType(Integer answer_content_type) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE answer_content_type = ?", messageRowMapper, answer_content_type);
    }

    //按照upTotalTokenNumber查询多个message
    public List<Message> getMessageByUpTotalTokenNumber(Integer up_total_token_number) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE total_token_number > ?", messageRowMapper, up_total_token_number);
    }

    //按照upQueryTokenNumber查询多个message
    public List<Message> getMessageByUpQueryTokenNumber(Integer up_query_token_number) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE query_token_number > ?", messageRowMapper, up_query_token_number);
    }

    //按照upAnswerTokenNumber查询多个message
    public List<Message> getMessageByUpAnswerTokenNumber(Integer up_answer_token_number) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE answer_token_number > ?", messageRowMapper, up_answer_token_number);
    }

    //按照conversation_id查询最新的N条记录，时间排序以自增主键message_id为依据(值越大约新)
    public List<Message> getLatestMessagesByConversationId(String conversation_id, int N) {
        return jdbcTemplate.query("SELECT * FROM messages WHERE conversation_id = ? ORDER BY message_id DESC LIMIT ?", messageRowMapper, conversation_id, N);
    }



    //新message注册，传入11个外部参数，然后内部再生成1个At参数
    //在API请求响应函数中执行(问&答聊天记录)，不在MessageController中通过客户端的路径请求注册新message
    public Message messageRegister(String botName, String userId, String userName,
                                   String conversationId,Integer totalTokenNumber,
                                   String queryContent, Integer queryContentType, Integer queryTokenNumber,
                                   String answerContent, Integer answerContentType, Integer answerTokenNumber) {
        Message message = new Message();
        message.setBotName(botName);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setConversationId(conversationId);
        message.setTotalTokenNumber(totalTokenNumber != null ? totalTokenNumber : 0);
        message.setCreatedAt(System.currentTimeMillis() / 1000);// System.currentTimeMillis()为13位毫秒级，转换为秒级时间戳
        message.setQueryContent(queryContent != null ? queryContent : "");
        message.setQueryContentType(queryContentType != null ? queryContentType : 0);
        message.setQueryTokenNumber(queryTokenNumber != null ? queryTokenNumber : 0);
        message.setAnswerContent(answerContent != null ? answerContent : "");
        message.setAnswerContentType(answerContentType != null ? answerContentType : 0);
        message.setAnswerTokenNumber(answerTokenNumber != null ? answerTokenNumber : 0);

        // 使用JdbcTemplate插入数据并获取自增主键
        KeyHolder holder = new GeneratedKeyHolder();  //获取数据库自动生成的主键
        //jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");   // 1. 临时禁用外键检查
        //logger.info("开发模式，写入message表暂时禁用外键约束检查(尚未构建user表)");

        //写入message表，外部12个字段+表内1个自增主键
        try {
            if (1 != jdbcTemplate.update((conn) -> {
                var ps = conn.prepareStatement(
                        "INSERT INTO messages (" +
                                "bot_name, user_id, user_name, conversation_id, total_token_number," +
                                "query_content, query_content_type, query_token_number, " +
                                "answer_content, answer_content_type, answer_token_number, " +
                                "created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, message.getBotName());
                ps.setString(2, message.getUserId());
                ps.setString(3, message.getUserName());
                ps.setString(4, message.getConversationId());
                ps.setInt(5, message.getTotalTokenNumber());
                ps.setString(6, message.getQueryContent());
                ps.setInt(7, message.getQueryContentType());
                ps.setInt(8, message.getQueryTokenNumber());
                ps.setString(9, message.getAnswerContent());
                ps.setInt(10, message.getAnswerContentType());
                ps.setInt(11, message.getAnswerTokenNumber());
                ps.setLong(12, message.getCreatedAt());
                return ps;
            }, holder)) {
                throw new RuntimeException("Message insert failed.");
            }
        } finally {
            //jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");   // 2. 无论插入成功与否，最终重新启用外键检查
        }

        message.setMessageId(holder.getKey().longValue());// 设置自增主键
        return message;
    }

    //异步存储消息（适合流式场景）
    @Async
    public CompletableFuture<Message> asyncMessageRegister(String botName, String userId, String userName,
                                                           String conversationId,Integer totalTokenNumber,
                                                           String queryContent, Integer queryContentType, Integer queryTokenNumber,
                                                           String answerContent, Integer answerContentType, Integer answerTokenNumber) {
        return CompletableFuture.completedFuture(
                messageRegister(botName, userId, userName, conversationId, totalTokenNumber,
                        queryContent, queryContentType, queryTokenNumber,
                        answerContent, answerContentType, answerTokenNumber)
        );
    }

    //批量存储List<Message>消息（适合需要事务的场景）
    @Transactional
    public void batchRegisterMessages(List<Message> messages) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO messages (" +
                        "bot_name, user_id, user_name, conversation_id, total_token_number," +
                        "query_content, query_content_type, query_token_number, " +
                        "answer_content, answer_content_type, answer_token_number, " +
                        "created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Message m = messages.get(i);
                        ps.setString(1, m.getBotName());
                        ps.setString(2, m.getUserId());
                        ps.setString(3, m.getUserName());
                        ps.setString(4, m.getConversationId());
                        ps.setInt(5, m.getTotalTokenNumber());
                        ps.setString(6, m.getQueryContent());
                        ps.setInt(7, m.getQueryContentType());
                        ps.setInt(8, m.getQueryTokenNumber());
                        ps.setString(9, m.getAnswerContent());
                        ps.setInt(10, m.getAnswerContentType());
                        ps.setInt(11, m.getAnswerTokenNumber());
                        ps.setLong(12, m.getCreatedAt());
                    }

                    @Override
                    public int getBatchSize() {
                        return messages.size();
                    }
                }
        );
    }

}
