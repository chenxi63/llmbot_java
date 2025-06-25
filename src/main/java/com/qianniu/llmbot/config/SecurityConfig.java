package com.qianniu.llmbot.config;

import com.qianniu.llmbot.Filter.JwtAuthenticationFilter;
import com.qianniu.llmbot.JWTtoken.JwtTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/***********************************************
* JWT鉴权访问路径的权限设置
 * 1）开放路径设置、鉴权路径设置，鉴权路径下的权限等级路径在各响应方法中具体定义；
* **********************************************/

// SecurityConfig安全加密相关设置，即哪些路径请求、POST请求需要进行请求Header中的jwt令牌token的权限认证;jwt令牌token使用哈希加密
// 1）对安全设置下的某些路径的请求Headers携带jwt令牌token的校验逻辑(是否携带、提取账号信息、校验有效期)，在Filter过滤器中定义实现，
// 2）对jwt令牌token的处理(生成/提取/校验等)需要单独定义jwt相关的entity实体、service服务，实际的生成逻辑则在RestController在对应的loginin、signin路径实现
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)  // 添加这行启用方法级安全,用于role的精细化路径权限控制
public class SecurityConfig {
    private final JwtTokenUtil jwtTokenUtil;

    public SecurityConfig(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    //配置哈希加密，调用passwordEncoder时发挥作用
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 默认使用 BCrypt 算法
    }

    //开放路径："/api/**"下的所有端点，其他端点需要认证; 需要在JWT Filter中同步开放白名单路径
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 强制所有请求使用 HTTPS
                //.requiresChannel(channel -> channel .anyRequest().requiresSecure() )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())  // 启用 CORS（会读取CORSConfig配置类）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 放行 OPTIONS 请求（允许CORS）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 放行其他公开端点,需要同步在JWT Filter中放开
                        .requestMatchers("/api/info", "/api/user/register", "/api/user/login", "/api/chat/**", "/api/user/login","/api/model/getnames","/api/model/getplatform").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                        .accessDeniedHandler(jwtAccessDeniedHandler()));

        return http.build();
    }



    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenUtil);
    }

    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"error\":\"需要认证\"}");
        };
    }

    @Bean
    public AccessDeniedHandler jwtAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("{\"error\":\"权限不足\"}");
        };

    }
}


