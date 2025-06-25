package com.qianniu.llmbot.web;

import com.qianniu.llmbot.JWTtoken.JwtTokenUtil;
import com.qianniu.llmbot.product_entity.User;
import com.qianniu.llmbot.product_entity.UserLoginRequest;
import com.qianniu.llmbot.product_entity.UserRegisterResponseDTO;
import com.qianniu.llmbot.product_service.AuthService;
import com.qianniu.llmbot.product_service.UserService;
import io.jsonwebtoken.Claims;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


/*********************************************
 * User请求响应的特例——login&logout请求响应定义，开放路径不用鉴权方便用户随时访问
 * Login登录功能：
 * 1）使用邮箱+密码、手机号+密码二选一都可登录验证密码；
 * 2）验证成功后，将user中Email(Name)、会员角色role一同封装生成jwt token;
 * 3）最后返回登录login后的脱敏user信息；
 *
 * Logout登出功能：
 * 1）将tokenVersion更新(+1)，确保前面的token失效
 * **********************************************/

@RestController
@RequestMapping(value = "/api/user") //基础路径为"/api/user"，即后续所有的路径都自动添加 "/api/user/xxxxx"
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService; //相比@Autowired注入组件，可避免运行时被修改，生产环境适用
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    public AuthController(AuthService authService, JwtTokenUtil jwtTokenUtil, UserService userService) {
        this.authService = authService;
        this.jwtTokenUtil = jwtTokenUtil ;
        this.userService = userService;
    }

    //用户登录login请求：email账号+密码登录的加密鉴权，认证成功响应头中发送JWT token
    //POST请求，localhost:8080/api/user/login；请求体为{"email":"user@example.com", "phone":"+8613588288394", "password":"123456"}
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> logIn(@RequestBody @Valid UserLoginRequest request, @RequestHeader(value = "Authorization", required = false) String authHeader) {
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
                        "message", "已登录，无需新登录"
                ));
            }
        }

        try {
            // 邮箱或手机号登录
            if (StringUtils.isBlank(request.getEmail()) && StringUtils.isBlank(request.getPhone())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", HttpStatus.BAD_REQUEST.value(),
                        "message", "必须提供邮箱或手机号",
                        "errors", Map.of("credentials", "邮箱或手机号不能同时为空")
                ));
            }

            UserRegisterResponseDTO userDto;

            // 邮箱登录或手机号登录,获取最新的user信息(已脱敏)
            if (StringUtils.isNotBlank(request.getEmail())) {
                userDto = authService.loginByEmail(request.getEmail(), request.getPassword());
            } else {
                if (StringUtils.isBlank(request.getPhone())) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "code", HttpStatus.BAD_REQUEST.value(),
                            "message", "手机号不能为空",
                            "errors", Map.of("phone", "手机号不能为空")
                    ));
                }
                userDto = authService.loginByPhone(request.getPhone(), request.getPassword());
            }

            // 将最新获取的user信息封装生成token
            String token = jwtTokenUtil.generateToken(userDto.getEmail(),userDto.getRoleName(),userDto.getName(), userDto.getTokenVersion());

            //生产环境下应该直接注入响应Header
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Access-Control-Expose-Headers", "Authorization"); // 允许前端读取

            // 构建标准格式的响应体(删除显式token)
            Map<String, Object> responseBody = Map.of(
                    "success", true,
                    "code", HttpStatus.OK.value(),
                    "message", "登录成功",
                    "data", Map.of(
                            "user", userDto,
                            "tokenType", "Bearer"  // 保留tokenType信息，但不再返回token本身在body中
                    )
            );

            //返回响应内容
            return ResponseEntity.ok().headers(headers).body(responseBody);
        } catch (BadCredentialsException e) {
            logger.warn("登录失败: 用户名或密码错误 - {}",
                    StringUtils.isNotBlank(request.getEmail()) ? request.getEmail() : request.getPhone());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "code", HttpStatus.UNAUTHORIZED.value(),
                    "message", "登录失败",
                    "errors", Map.of("credentials", "用户名或密码错误")
            ));
        } catch (Exception e) {
            logger.error("登录失败: 服务器内部错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "服务器内部错误"
            ));
        }
    }


    //拦截器鉴权通过时，允许用户logout请求：tokenVsersion+1，但不需要生成新token返回，等待用户主动login
    //GET请求，localhost:8080/api/user/logout
    @GetMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> logOut(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String jwtToken = authHeader.substring(7);
        String jwtEmailName = jwtTokenUtil.getNameFromToken(jwtToken);

        //更新用户tokenVersion 版本号，后续访问旧版本token失效
        userService.updateTokenVersionByEmail(jwtEmailName);
        return ResponseEntity.ok().body("登出成功");
    }

}

