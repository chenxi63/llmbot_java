package com.qianniu.llmbot.Filter;

import com.qianniu.llmbot.JWTtoken.JwtTokenUtil;
import com.qianniu.llmbot.product_entity.User;
import com.qianniu.llmbot.product_entity.UserRegisterResponseDTO;
import com.qianniu.llmbot.product_service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/*********************************************
 * JWT Token校验过滤器设置与处理，面向被设置鉴权的路径请求，自动进行JWT token的校验
 * 1）初步资格鉴定：提取token、提取Name、Name非空+未过期。
 *    未校验token中的Name与Request请求参数Name是否相符，因为如果在Filter中读取了Request请求参数，则需要重新构造请求参数以传递后续的响应函数；
 *    在Chat流式响应中，手动校验token时，增加了校验Name参数是否相符；
 * 2）权限等级鉴定：提取role, 并注入Authentication已认证的主体及其凭证和权限信息，后续响应函数根据注解自动判断权限；
 * 3）将认证后的信息，注入到SecurityContext上下文中开启鉴定
 * **********************************************/


// SecurityConfig安全加密相关设置，即哪些路径请求、POST请求需要进行请求Header中的jwt令牌token的权限认证;jwt令牌token使用哈希加密
// 1）对安全设置下的某些路径的请求Headers携带jwt令牌token的校验逻辑(是否携带、提取账号信息、校验有效期)，在Filter过滤器中定义实现，
// 2）对jwt令牌token的处理(生成/提取/校验等)需要单独定义jwt相关的entity实体、service服务，实际的生成逻辑则在RestController在对应的loginin、signin路径实现
public class JwtAuthenticationFilter  extends OncePerRequestFilter {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Autowired
    private UserService userService;

    // 白名单路径：免过滤的放行路径，与SecurityConfig配置类中保持一致
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/info",
            "/api/user/login",
            "/api/user/register",
            "/api/chat",
            "/api/model/getnames",
            "/api/model/getplatform"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        try {
            // 跳过白名单路径
            if (EXCLUDED_PATHS.stream().anyMatch(path ->
                    request.getRequestURI().startsWith(path))) {
                chain.doFilter(request, response);
                return;
            }

            String token = jwtTokenUtil.extractToken(request);//从请求Header中提取token

            if (token == null) {
                // Token 为 null 时的处理
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"未提供认证令牌，请提供有效的Bearer Token\"}");
                return;
            }
            // 校验有效的token、权限
            processTokenAuthentication(token); //基础资格鉴定(含版本号)+权限等级鉴定(ROLE)
            chain.doFilter(request, response);

        } catch (Exception e) {
            // 处理其他异常情况
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"认证失败: " + e.getMessage() + "\"}");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    //完整的处理token,包括Name非空(未校验Name相符)、有效期、版本号，通过后提取roles作为会员角色权限
    private void processTokenAuthentication(String jwtToken) {
        String jwtEmailName= jwtTokenUtil.getNameFromToken(jwtToken); //从token中获取携带的Name(email)
        Claims claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
        Integer jwtTokenVersion = (Integer)claims.get("tokenVersion");//提取版本号


        //基础资格鉴定：Name(email)非空 且 token在有效期内
        if (jwtEmailName == null || jwtTokenUtil.isTokenExpired(jwtToken)) {
            throw new RuntimeException("无效的Token: 用户名不存在或Token已过期");
        }

        //这里增加鉴定版本号
        if (!jwtTokenVersion.equals(userService.getTokenVersionByEmail(jwtEmailName))) {
            throw new RuntimeException("无效的Token: token为旧版本已失效");
        }

        // 这里增加会员角色鉴定：提取role判断如果是MEMEBER或者SUPER_MEMEBER，判断会员时间是否过期；
        // 过期则将提取的role恢复成NORMAL,注入到请求头的Authentication以便后续响应函数自动识别
        List<String> roles = jwtTokenUtil.extractRolesFromToken(claims);
        User userOld = userService.getUserByEmail(jwtEmailName); //查询user信息
        if(roles.get(0).equals("ROLE_MEMBER") || roles.get(0).equals("ROLE_SUPER_MEMBER")) {
            //判断过期，系统函数默认为毫秒，数据库存储为秒
            if(System.currentTimeMillis() > (userOld.getMembershipExpiry() * 1000))
            {
                userService.updateRoleByEmail(0, jwtEmailName); //更新user信息，重置role为NORMAL用户、重置membershipExpiry会员到期时间为0
                userService.updateTokenVersionByEmail(jwtEmailName); //更新用户 tokenVersion 版本号
                User userNew = userService.getUserByEmail(jwtEmailName); //查询更新后的user信息
                UserRegisterResponseDTO userDto = UserRegisterResponseDTO.fromUser(userNew); //user信息脱敏

                roles.set(0, "ROLE_NORMAL"); //替换当前token中获取的role，后续响应函数会员角色鉴定使用

                // 将user信息封装生成token
                String token = jwtTokenUtil.generateToken(userDto.getEmail(),userDto.getRoleName(),userDto.getName(), userDto.getTokenVersion());

                //将新token返回客户端（实际未操作需用户重新login），生产环境下即时注入响应Header
                logger.info("会员到期，已恢复为普通用户！新token为：" + token);
                //response.setHeader("Authorization", "Bearer " + newToken);
                //response.setHeader("Access-Control-Expose-Headers", "Authorization");

            }
        }

        //将role注入到Authentication中，便于后续请求响应函数通过注解方法控制会员权限
        //会员到期中，role会被改为NORMAL注入上下文，本次请求后续响应函数会员权限校验时立刻阻止
        setAuthentication(jwtEmailName, roles);  //将鉴定后的信息注入上下文
    }

    //通过 setAuthentication() 方法将roles角色信息注入到 SecurityContext 后,后续响应函数即可通过注解启动自动会员校验
    private void setAuthentication(String username, List<String> roles) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> {
                        if (!role.startsWith("ROLE_")) {
                            return new SimpleGrantedAuthority("ROLE_" + role);
                        }
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toList());

            //放开权限路径，并将role注入到
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
