package com.qianniu.llmbot.product_service;

import com.qianniu.llmbot.product_entity.Message;
import com.qianniu.llmbot.product_entity.Model;
import com.qianniu.llmbot.product_entity.UserRegisterResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/*********************************************
 * Model Service中用户model的相关行为方法定义
 * 1) 新model注册、根据各字段查询model；
 * **********************************************/

@Component
@Transactional
public class ModelService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);//日志记录器，日志输出时会自动标记类名（如 UserService），便于过滤和排查问题。

    @Autowired
    JdbcTemplate jdbcTemplate;

    RowMapper<Model> modelRowMapper = new BeanPropertyRowMapper<>(Model.class);//将数据库查询结果集ResultSet的每一行自动映射到Model实体实例中

    //根据platform_name查询platforms表中的models信息
    public List<String> getModelsByPlatformName(String platformName) {
        String sql = "SELECT models FROM platforms WHERE platform_name = ?";
        return jdbcTemplate.queryForList(sql, String.class, platformName);
    }

    // 查询所有model的name，query处理多行结果
    public List<String> getModelNames() {
        String sql = "SELECT model_name FROM models";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    // 查询所有model，query处理多行结果
    public List<Model> getModelsByAll() {
        return jdbcTemplate.query("SELECT * FROM models ", modelRowMapper);
    }

    // 按照modelId查询单个model，queryForObject只用于处理单行结果
    public Model getModelByModelId(Long modelId) {
        return jdbcTemplate.queryForObject("SELECT * FROM models WHERE model_id = ?", modelRowMapper, modelId);
    }

    // 按照modelName查询单个model，queryForObject只用于处理单行结果
    public Model getModelByModelName(String modelName) {
        return jdbcTemplate.queryForObject("SELECT * FROM models WHERE model_name = ?", modelRowMapper, modelName);
    }

    // 按照modelType查询多个model，query处理多行结果
    public List<Model> getModelsByModelType(Integer modelType) {
        return jdbcTemplate.query("SELECT * FROM models WHERE model_type = ?", modelRowMapper, modelType);
    }

    // model_name是否已经存在
    public boolean isModelNameExist(String modelName) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM models WHERE model_name = ?", Integer.class, modelName);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    // 新model注册
    public Model registerModel(String modelName, int modelType, String modelUrl,
                               String modelParameters, String allowedRolesJson) {
        // 参数校验
        Objects.requireNonNull(modelName, "Model name cannot be null");
        Objects.requireNonNull(modelUrl, "Model URL cannot be null");

        // 创建并初始化Model对象
        Model model = new Model();
        model.setModelName(modelName);
        model.setModelType(modelType);
        model.setModelUrl(modelUrl);
        model.setModelParameters(modelParameters != null ? modelParameters : "{}");
        model.setModelAllowroles(allowedRolesJson != null ? allowedRolesJson : "[]");
        model.setCreatedDatetime(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 准备SQL语句（包含所有字段）
        final String sql = "INSERT INTO models (" +
                "model_name, model_type, model_url, " +
                "model_parameters, model_allowroles, created_datetime) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        // 执行插入操作
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int affectedRows = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, model.getModelName());
                ps.setInt(2, model.getModelType());
                ps.setString(3, model.getModelUrl());
                ps.setString(4, model.getModelParameters());
                ps.setString(5, model.getModelAllowroles());
                ps.setString(6, model.getCreatedDatetime());
                return ps;
            }, keyHolder);

            if (affectedRows != 1) {
                throw new RuntimeException("Failed to insert model, affected rows: " + affectedRows);
            }

            // 设置生成的ID
            model.setModelId(keyHolder.getKey().longValue());
            return model;

        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Model name already exists: " + modelName, e);
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while registering model: " + e.getMessage(), e);
        }
    }


}
