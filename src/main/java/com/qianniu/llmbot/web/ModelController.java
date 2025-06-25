package com.qianniu.llmbot.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qianniu.llmbot.product_entity.Model;
import com.qianniu.llmbot.product_entity.ModelRegisterRequest;
import com.qianniu.llmbot.product_entity.User;
import com.qianniu.llmbot.product_service.ModelService;
import com.qianniu.llmbot.product_service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/*********************************************
 * Model相关的各种请求响应函数定义
 * 1）model所有相关的请求响应均需要鉴权，即登录成功后携带jwt token；除此外查询响应，还需要权限等级校验(已在Filter过滤器中定义)；
 * **********************************************/

@RestController
@RequestMapping(value = "/api/model") //基础路径为 "/api/model"，即后续所有的路径都自动添加 "/api/model/xxxxx"
@Validated // 启用方法级参数校验，即GET请求校验，POST请求在响应函数使用@RequestBody @Valid
public class ModelController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModelService modelService; //相比@Autowired注入组件，可避免运行时被修改，生产环境适用
    public ModelController(ModelService modelService) {
        this.modelService=modelService;
    }

    //进入Home即查询所有model并加载渲染列表，不需要登录的公开路径，在SecurityConfig配置文件、JwtAuthenticationFilter过滤器中均放开路径
    //localhost:8080/api/model/getplatform?platform=banduqianfan
    @GetMapping(value = "/getplatform", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlatformsModels(@RequestParam String platform) {
        try {
            List<String> platformModels = modelService.getModelsByPlatformName(platform);
            if (platformModels.isEmpty()) {
                return ResponseEntity.notFound().build(); // 返回404
            }
            return ResponseEntity.ok(platformModels); // 返回200+models
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "模型不存在",
                    "path", "/api/model/getnames"
            );
            logger.warn("查询模型失败 -  错误: {}",errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //进入Home即查询所有model并加载渲染列表，不需要登录的公开路径，在SecurityConfig配置文件、JwtAuthenticationFilter过滤器中均放开路径
    //localhost:8080/api/model/getnames
    @GetMapping(value = "/getnames", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getModelNames() {
        try {
            List<String> modelNames = modelService.getModelNames();
            if (modelNames.isEmpty()) {
                return ResponseEntity.notFound().build(); // 返回404
            }
            return ResponseEntity.ok(modelNames); // 返回200+models
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "模型不存在",
                    "path", "/api/model/getnames"
            );
            logger.warn("查询模型失败 -  错误: {}",errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //查询所有model具体参数信息
    //localhost:8080/api/model/getbyall
    @PreAuthorize("hasAnyRole('SUPER_MEMBER')")
    @GetMapping(value = "/getbyall", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getModelsByType() {
        try {
            List<Model> models = modelService.getModelsByAll();
            if (models.isEmpty()) {
                return ResponseEntity.notFound().build(); // 返回404
            }
            return ResponseEntity.ok(models); // 返回200+models
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "模型不存在",
                    "path", "/api/model/getbyall"
            );
            logger.warn("查询模型失败 -  错误: {}",errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //按照model_id查询model,url中附带id参数
    //localhost:8080/api/model/getbymodelid?id=2，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasAnyRole('SUPER_MEMBER')")
    @GetMapping(value = "/getbymodelid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getModelByModelId(@RequestParam long id) {
        try {
            Model model = modelService.getModelByModelId(id);
            if (model == null) {
                return ResponseEntity.notFound().build(); // 返回404
            }
            return ResponseEntity.ok(model); // 返回200+model
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "模型ID " + id + " 不存在",
                    "path", "/api/model/getbymodelid?id=" + id
            );
            logger.warn("查询模型失败 - ID: {}, 错误: {}", id, errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //按照model_name查询model,url中附带name参数
    //localhost:8080/api/model/getbymodelname?name=ernie-speed-128k，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasAnyRole('SUPER_MEMBER')")
    @GetMapping(value = "/getbymodelname", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getModelByName(@RequestParam @NotBlank(message = "模型名不能为空") @Size(min = 2, max = 100, message = "模型名长度必须在2-100个字符之间") String name) {
        try {
            Model model = modelService.getModelByModelName(name);
            if (model == null) {
                return ResponseEntity.notFound().build(); // 返回404
            }
            return ResponseEntity.ok(model); // 返回200+model
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "模型名称 '" + name + "' 不存在",
                    "path", "/api/model/getbymodelname?name=" + name
            );
            logger.warn("查询模型失败 - 名称: {}, 错误: {}", name, errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //按照model_type查询model,url中附带type参数
    //localhost:8080/api/model/getbymodeltype?type=0，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasAnyRole('SUPER_MEMBER')")
    @GetMapping(value = "/getbymodeltype", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getModelsByType(@RequestParam @NotNull(message = "role不能为空")@Min(value = 0, message = "role值不能小于1") @Max(value = 3, message = "role不能大于3") int type) {
        try {
            List<Model> models = modelService.getModelsByModelType(type);
            if (models.isEmpty()) {
                // 构造错误响应（和 catch 块一致）
                Map<String, Object> errorResponse = Map.of(
                        "error", "Not Found",
                        "message", "类型为 " + getTypeName(type) + " 的模型不存在",
                        "path", "/api/model/getbymodeltype?type=" + type
                );
                logger.warn("查询模型失败 - 类型: {}, 错误: {}", getTypeName(type), errorResponse);
                return ResponseEntity.status(404).body(errorResponse);
            }
            return ResponseEntity.ok(models); // 返回200+models
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "类型为 " + getTypeName(type) + " 的模型不存在",
                    "path", "/api/model/getbymodeltype?type=" + type
            );
            logger.warn("查询模型失败 - 类型: {}, 错误: {}", getTypeName(type), errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    // 模型类型名称（便于前端显示）
    private static String getTypeName(int type) {
        switch (type) {
            case 1: return "图像生成模型";
            case 2: return "语音生成模型";
            case 3: return "视频生成模型";
            default: return "文本生成模型";
        }
    }

    //新用户注册，UserRegisterRequest会先校验新注册的用户信息是否有效，通过后才会传入userService.registerUser进行注册，并返回脱敏user
    //指名该路径为非流式响应，全局配置更改为默认流式响应
    //POST请求为localhost:8080/api/model/register；请求体为{}
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerModel(@RequestBody @Valid ModelRegisterRequest request) {
        try {
            // 检查模型名称是否已存在
            if (modelService.isModelNameExist(request.getModelName())) {
                logger.warn("注册失败：模型名称 {} 已存在", request.getModelName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("modelName", "该模型名称已被使用"));
            }

            String allowRolesJson = request.getModelAllowRoles() != null ?
                    request.getModelAllowRoles() : "[]";

            // 3. 注册模型
            Model model = modelService.registerModel(
                    request.getModelName(),
                    request.getModelType(),
                    request.getModelUrl(),
                    request.getModelParameters(),
                    request.getModelAllowRoles()
            );

            // 4. 返回标准化响应
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", "MODEL_REGISTERED",
                    "data", model
            ));

        } catch (DataAccessException e) {
            logger.error("数据库操作失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "DATABASE_ERROR",
                            "message", "模型注册失败，请稍后重试"
                    ));
        } catch (Exception e) {
            logger.error("未知错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "SERVER_ERROR",
                    "message", "服务器内部错误"
            ));
        }
    }
}
