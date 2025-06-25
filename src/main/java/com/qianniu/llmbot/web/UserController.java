package com.qianniu.llmbot.web;

import com.qianniu.llmbot.JWTtoken.JwtTokenUtil;
import com.qianniu.llmbot.product_entity.User;
import com.qianniu.llmbot.product_entity.UserRegisterRequest;
import com.qianniu.llmbot.product_entity.UserRegisterResponseDTO;
import com.qianniu.llmbot.product_service.UserService;
import io.jsonwebtoken.Claims;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*********************************************
 * User相关的各种请求响应函数定义
 * 1）user注册register的请求为开放路径，方便用户随时注册；
 * 2）其他的请求路径均需要鉴权，即登录成功后携带jwt token；除此外查询响应，还需要权限等级校验(已在Filter过滤器中定义)；
 * 3）所有请求响应均为普通的非流式响应，需要明确标识区分开；
 * **********************************************/

@RestController
@RequestMapping(value = "/api/user") //基础路径为 "/api/user"，即后续所有的路径都自动添加 "/api/user/xxxxx"
@Validated // 启用方法级参数校验，即GET请求校验，POST请求在响应函数使用@RequestBody @Valid
public class UserController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserService userService; //相比@Autowired注入组件，可避免运行时被修改，生产环境适用
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    private JwtTokenUtil jwtTokenUtil;


    //按照id查询user,url中附带id参数
    //localhost:8080/api/user/getbyid?id=2，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserById(@RequestParam long id) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.notFound().build(); // 返回404
            }

            UserRegisterResponseDTO response = UserRegisterResponseDTO.fromUser(user);
            Map<String, Object> successResponse = Map.of(
                    "success", true,
                    "code", 200,
                    "message", "请求成功",
                    "data", response
            );
            return ResponseEntity.ok(successResponse); // 返回200+标准格式
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "用户ID " + id + " 不存在",
                    "path", "/api/user/get/byid?id=" + id
            );
            logger.warn("查询用户失败 - ID: {}, 错误: {}", id, errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //按照uuid查询user,url中附带uuid参数
    //localhost:8080/api/user/getbyuuid?uuid=xxxx，@RequestParam用于从URL中获取参数值(?后面的参数)并绑定到方法参数。
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyuuid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByUuid(@RequestParam String uuid) {
        try {
            User user = userService.getUserByUuid(uuid);
            if (user == null) {
                return ResponseEntity.notFound().build(); // 返回404
            }

            UserRegisterResponseDTO response = UserRegisterResponseDTO.fromUser(user);
            Map<String, Object> successResponse = Map.of(
                    "success", true,
                    "code", 200,
                    "message", "请求成功",
                    "data", response
            );
            return ResponseEntity.ok(successResponse); // 返回200+标准格式
        } catch (Exception e) {
            // 构造错误响应
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "用户Uuid " + uuid + " 不存在",
                    "path", "/api/user/get/byuuid?uuid=" + uuid
            );
            logger.warn("查询用户失败 - Uuid: {}, 错误: {}", uuid, errorResponse);
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //按照email查询user,url中附带email参数
    //localhost:8080/api/user/getbyemail?email=user_alex@163.com
    @GetMapping(value = "/getbyemail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByEmail(@RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email) {
        try {
            User user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.notFound().build(); // 返回404
            }

            UserRegisterResponseDTO response = UserRegisterResponseDTO.fromUser(user);
            Map<String, Object> successResponse = Map.of(
                    "success", true,
                    "code", 200,
                    "message", "请求成功",
                    "data", response
            );
            return ResponseEntity.ok(successResponse); // 返回200+标准格式
        } catch (EmptyResultDataAccessException e) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "邮箱 " + email + " 对应的用户不存在",
                    "path", "/api/user/get/byemail?email=" + email
            );
            logger.warn("查询用户失败 - 邮箱: {}, 错误: {}", email, errorResponse);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    //按照uuid更新collectModels
    //localhost:8080/api/user/updatcltmls
    @PostMapping(value = "/updatcltmls", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setCollectModelsByUuid(@RequestBody Map<String, String> request) {
        try {
            String uuid = request.get("uuid");
            String collectModels = request.get("collectModels");

            userService.updateCollectModelsByUuid(uuid, collectModels);//更新模型搜藏列表

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", 200,
                    "message", "collectModels 更新成功"
            ));
        } catch (EmptyResultDataAccessException e) {
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "code", 404,
                    "message", "用户Uuid " +  request.get("uuid") + " 不存在",
                    "path", "/api/user/updatcltmls"
            );
            logger.warn("更新collectModels失败 - Uuid: {}, 错误: {}",  request.get("uuid"), e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    //按照phone查询user,url中附带phone参数；注意+86中的+号会被编码为空格，需要使用%2B代替
    //localhost:8080/api/user/getbyphone?phone=%2B8613512345676
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyphone", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByPhone(@RequestParam @NotBlank(message = "手机号不能为空") String phone) {
        try {
            User user = userService.getUserByPhone(phone);
            if (user == null) {
                return ResponseEntity.notFound().build(); // 返回404
            }

            UserRegisterResponseDTO response = UserRegisterResponseDTO.fromUser(user);
            Map<String, Object> successResponse = Map.of(
                    "success", true,
                    "code", 200,
                    "message", "请求成功",
                    "data", response
            );
            return ResponseEntity.ok(successResponse); // 返回200+标准格式
        } catch (EmptyResultDataAccessException e) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "手机号 " + phone.substring(3) + " 对应的用户不存在",
                    "path", "/api/user/get/byphone?phone=" + phone
            );
            logger.warn("查询用户失败 - 手机号: {}, 错误: {}", phone.substring(3), errorResponse);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    //按照name查询user,url中附带name参数
    //localhost:8080/api/user/getbyname?name=bob
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyname", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByName(@RequestParam @NotBlank(message = "用户名不能为空") @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间") String name) {
        List<User> users = userService.getUserByName(name);
        if (users.isEmpty()) { // 检查是否为空
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "用户名 " + name + " 对应的用户不存在",
                    "path", "/api/user/get/byname?name=" + name
            );
            logger.warn("查询用户失败 - 用户名: {}, 错误: {}", name, errorResponse);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        // 对每个用户进行脱敏处理
        List<UserRegisterResponseDTO> safeUsers = users.stream()
                .map(UserRegisterResponseDTO::fromUser)  // 使用DTO进行脱敏转换
                .collect(Collectors.toList());

        // 标准成功响应格式
        Map<String, Object> successResponse = Map.of(
                "success", true,
                "code", 200,
                "message", "请求成功",
                "data", safeUsers  // 返回脱敏后的用户列表
        );
        return ResponseEntity.ok(successResponse);
    }

    //按照role查询user,url中附带role参数
    //localhost:8080/api/user/getbyrole?role=0
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/getbyrole", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserByRole(@RequestParam @NotNull(message = "role不能为空")@Min(value = 0, message = "role值不能小于1") @Max(value = 3, message = "role不能大于3")  int role) {
        List<User> users = userService.getUserByRole(role);
        if (users.isEmpty()) { // 检查是否为空
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "会员等级： " + getRoleName(role) + " 对应的用户不存在",
                    "path", "/api/user/get/byrole?role=" + role
            );
            logger.warn("查询用户失败 - 会员等级: {}, 错误: {}", getRoleName(role), errorResponse);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        // 对每个用户进行脱敏处理
        List<UserRegisterResponseDTO> safeUsers = users.stream()
                .map(UserRegisterResponseDTO::fromUser)  // 使用DTO进行脱敏转换
                .collect(Collectors.toList());

        // 标准成功响应格式
        Map<String, Object> successResponse = Map.of(
                "success", true,
                "code", 200,
                "message", "请求成功",
                "data", safeUsers  // 返回脱敏后的用户列表
        );
        return ResponseEntity.ok(successResponse);
    }

    // 角色名称（便于前端显示）
    private static String getRoleName(int role) {
        switch (role) {
            case 1: return "普通会员";
            case 2: return "超级会员";
            case 3: return "管理员";
            default: return "普通用户";
        }
    }


    //新用户注册，UserRegisterRequest会先校验新注册的用户信息是否有效，通过后才会传入userService.registerUser进行注册，并返回脱敏user
    //指名该路径为非流式响应，全局配置更改为默认流式响应
    //POST请求为localhost:8080/api/user/register；请求体为{"email":"user@example.com", "phone":"+8613912345678"(非必填), "name":"bob", "password":"12345678"}
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@RequestBody @Valid UserRegisterRequest request, @RequestHeader(value = "Authorization", required = false) String authHeader) {
       //开放路径手动校验，如果已经携带token且有效(Name非空&有效期内)，则不需要注册直接跳转首页
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            String jwtToken = authHeader.substring(7); //提取完整token
            String jwtEmailName = jwtTokenUtil.getNameFromToken(jwtToken); //从token中获取携带的Name(email)

            Claims claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
            Integer jwtTokenVersion = (Integer)claims.get("tokenVersion");//提取版本号

            //如果token有效且版本号确认，则不需要登录，其他情况需要登录，尤其logout后旧版本的token需要登录更新
            if (!(jwtEmailName == null || jwtEmailName.trim().isEmpty() || jwtTokenUtil.isTokenExpired(jwtToken)) && jwtTokenVersion.equals(userService.getTokenVersionByEmail(jwtEmailName))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", HttpStatus.BAD_REQUEST.value(),
                        "message", "已登录，无需新注册"
                ));
            }
        }

        try {
            if (userService.isEmailExist(request.getEmail())) {
                logger.warn("注册失败：邮箱 {} 已被占用", request.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "success", false,
                                "code", HttpStatus.CONFLICT.value(),
                                "message", "注册失败",
                                "errors", Map.of("email", "该邮箱已被注册")
                        ));
            }

            // 处理 phone 字段，如果为空或空白则设为 null
            String phone = StringUtils.isNotBlank(request.getPhone()) ? request.getPhone() : null;

            if (StringUtils.isNotBlank(request.getPhone())) {
                if (userService.isPhoneExist(request.getPhone())) {
                    logger.warn("注册失败：手机号 {} 已被占用", request.getPhone());
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of(
                                    "success", false,
                                    "code", HttpStatus.CONFLICT.value(),
                                    "message", "注册失败",
                                    "errors", Map.of("phone", "该手机号已被注册")
                            ));
                }
            }

            //如果请求中name参数为空，则默认为email
            String name = StringUtils.isNotBlank(request.getName()) ? request.getName() : request.getEmail();

            UserRegisterResponseDTO response = userService.registerUser( request.getEmail(), phone, name, request.getPassword()); //registerUser返回脱敏user
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "code", HttpStatus.OK.value(),
                    "message", "注册成功",
                    "data", response
            ));
        } catch (RuntimeException e) {
            logger.error("Registration failed for email: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", HttpStatus.BAD_REQUEST.value(),
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "Internal server error"
            ));
        }
    }

    //按照email为user充值,url中附带role参数，登录即可，不需要任何会员身份。更新role、tokenVersion, 然后生成新的token返回客户端，客户端用户不需要login即可自动替换旧版本token
    //localhost:8080/api/user/recharge?role=3
    @GetMapping(value = "/recharge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rechargeByEmail(@RequestParam @NotNull(message = "role不能为空")@Min(value = 0, message = "role值不能小于1") @Max(value = 3, message = "role不能大于3") int role, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String jwtToken = authHeader.substring(7); //提取完整token
        String jwtEmailName= jwtTokenUtil.getNameFromToken(jwtToken); //从token中获取携带的Name(email)

        try {
            userService.updateRoleByEmail(role, jwtEmailName); //更新role会员角色、membershipExpiry到期时间
            userService.updateTokenVersionByEmail(jwtEmailName);//更新tokenVersion 版本号
            User user = userService.getUserByEmail(jwtEmailName); //获取最新用户信息
            UserRegisterResponseDTO userDto = UserRegisterResponseDTO.fromUser(user); //user信息脱敏

            // 将user信息封装生成token
            String token = jwtTokenUtil.generateToken(userDto.getEmail(),userDto.getRoleName(),userDto.getName(), userDto.getTokenVersion());

            //生产环境下应该直接注入响应Header
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Access-Control-Expose-Headers", "Authorization"); // 允许前端读取

            // 构建标准格式的响应体(删除显式token)
            Map<String, Object> responseBody = Map.of(
                    "success", true,
                    "code", HttpStatus.OK.value(),
                    "message", "充值成功",
                    "data", Map.of(
                            "user", userDto,
                            "tokenType", "Bearer"  // 保留tokenType信息，但不再返回token本身在body中
                    )
            );
            return ResponseEntity.ok(responseBody);
        } catch (EmptyResultDataAccessException e) {
            Map<String, Object> errorResponse = Map.of(
                    "error", "Not Found",
                    "message", "邮箱 " + jwtEmailName + " 对应的用户不存在",
                    "path", "/api/user/recharge" + jwtEmailName
            );
            logger.warn("查询用户失败 - 邮箱: {}, 错误: {}", jwtEmailName, errorResponse);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

}
