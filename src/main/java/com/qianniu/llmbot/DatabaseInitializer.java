package com.qianniu.llmbot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    @Autowired
    JdbcTemplate jdbcTemplate;

    //依赖注入时执行初始化table表，测试环境中使用；生产环境下，不会在后端代码中创建表，一般在服务器中单独创建或迁移导入
    //yml文件中设置路径端口为//localhost:3306/llmbotjdbc，即已经指定了使用的database为llmbotjdbc，不必SQL语句中手动指定(USE llmbotjdbc或llmbotjdbc.users)
    @PostConstruct
    public void init() {
        // 创建platforms表
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS platforms ("
                + "platform_id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY COMMENT '平台唯一ID', "
                + "platform_name VARCHAR(100) NOT NULL UNIQUE COMMENT '唯一平台名', "
                + "models VARCHAR(2000) COMMENT '平台上的模型名(与models表中的模型名保持一致)', "
                + "created_datetime VARCHAR(100) NOT NULL COMMENT '创建时间(标准格式)', "
                + "INDEX idx_platform_name (platform_name)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI平台配置表'");

        // 创建models表
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS models ("
                + "model_id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY COMMENT '模型唯一ID', "
                + "model_name VARCHAR(100) NOT NULL UNIQUE COMMENT '模型唯一名称', "
                + "model_type TINYINT DEFAULT 0 COMMENT '0=文本,1=图像,2=音频,3=视频', "
                + "model_url VARCHAR(255) NOT NULL COMMENT '请求URL', "
                + "model_parameters VARCHAR(200) NOT NULL COMMENT '模型参数配置(JSON格式)',"
                + "model_allowroles VARCHAR(100) NOT NULL COMMENT '允许访问的角色列表(JSON数组)',"
                + "created_datetime VARCHAR(100) NOT NULL COMMENT '创建时间(标准格式)', "
                + "INDEX idx_model_name (model_name)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表'");

        // 创建users表
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS users ("
                + "id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY COMMENT '自增主键', "
                + "uuid VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID格式的唯一标识', "
                + "email VARCHAR(100) NOT NULL UNIQUE COMMENT '用户邮箱唯一非空', "
                + "phone VARCHAR(20)  CHARACTER SET ascii COLLATE ascii_bin UNIQUE COMMENT '格式:+国家码手机号，如+8613912345678', "
                + "name VARCHAR(100) NOT NULL COMMENT '用户名，默认为email值', "
                + "passwordHash VARCHAR(255) NOT NULL COMMENT 'BCrypt哈希密码，长度固定60字符但预留扩展空间', "
                + "role TINYINT DEFAULT 0 COMMENT '0=普通用户,1=会员,2=超级会员,3=管理员', "
                + "tokenVersion INT NOT NULL DEFAULT 1 COMMENT 'token版本号', "
                + "collectModels VARCHAR(1000) COMMENT '收藏的模型名列表',"
                + "createdAt BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间(unix秒)', "
                + "updatedAt BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间(unix秒)', "
                + "lastLogin BIGINT DEFAULT 0 COMMENT '最后登录时间(unix秒)，0表示从未登录', "
                + "membershipExpiry BIGINT DEFAULT 0 COMMENT '会员到期时间(unix秒)，0表示普通用户(非会员)', "
                + "INDEX idx_email (email), "
                + "INDEX idx_phone (phone), "
                + "INDEX idx_role (role), "
                + "INDEX idx_membership (membershipExpiry), "
                + "INDEX idx_uuid (uuid)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        // 创建messages表
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS messages ("
                + "message_id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY, "
                + "bot_name VARCHAR(100) NOT NULL, "
                + "user_id VARCHAR(36) NOT NULL, "
                + "user_name VARCHAR(100) NOT NULL, "
                + "conversation_id VARCHAR(200) NOT NULL, "
                + "total_token_number INT DEFAULT 0, "
                + "query_content TEXT NOT NULL, "
                + "query_content_type TINYINT NOT NULL DEFAULT 0, "
                + "query_token_number INT DEFAULT 0, "
                + "answer_content TEXT NOT NULL, "
                + "answer_content_type TINYINT NOT NULL DEFAULT 0, "
                + "answer_token_number INT DEFAULT 0, "
                + "created_at BIGINT NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (user_id) REFERENCES users(uuid), "
                + "INDEX idx_bot (bot_name), "
                + "INDEX idx_user (user_id), "
                + "INDEX idx_conversation (conversation_id), "
                + "INDEX idx_created_at (created_at)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        
    }

}
