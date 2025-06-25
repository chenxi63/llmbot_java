package com.qianniu.llmbot.product_service;

import com.qianniu.llmbot.product_entity.User;
import com.qianniu.llmbot.product_entity.UserRegisterResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/*********************************************
 * User Service中用户登录lonin的单独定义AuthService
 * 1) 定义两种login方法——loginByEmail、loginByPhone；
 * 2）登录方法中先进行密码验证(加密)，验证成功后更新user表中的LastLogin时间字段、返回脱敏user信息；
 * **********************************************/

//用户登录login时，用于查询user表中的加密账号，并进行加密对比鉴定
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);//日志记录器，日志输出时会自动标记类名（如 UserService），便于过滤和排查问题。

    private final UserService userService;
    private final PasswordEncoder passwordEncoder; // Spring Security的加密器

    public AuthService(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserRegisterResponseDTO loginByEmail(String email, String rawPassword) {
        try {
            User user = userService.getUserByEmail(email);//通过email账号查询目标user信息

            // 密码验证
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                throw new BadCredentialsException("邮箱或密码错误！");
            }

            UserRegisterResponseDTO response = UserRegisterResponseDTO.fromUser(user);
            userService.updateLastLogin(user.getId()); //更新登录时间
            return response;

        } catch (EmptyResultDataAccessException e) {
            throw new BadCredentialsException("该邮箱未注册！");
        }
    }

    public UserRegisterResponseDTO loginByPhone(String phone, String rawPassword) {
        try {
            User user = userService.getUserByPhone(phone);

            // 密码验证
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                throw new BadCredentialsException("手机号或密码错误！");
            }

            UserRegisterResponseDTO response = UserRegisterResponseDTO.fromUser(user);
            userService.updateLastLogin(user.getId()); //更新登录时间
            return response;

        } catch (EmptyResultDataAccessException e) {
            throw new BadCredentialsException("该手机号未注册！");
        }

    }
}

