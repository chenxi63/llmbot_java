package com.qianniu.llmbot.JWTtoken;

import com.qianniu.llmbot.product_service.UserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/*********************************************
 * JWT Token的生成与处理
 * 1）yml配置参数读入：jwt.secret、jwt.expiration；
 * 2）Token的生成：用户登录login成功才会生成token,其中会注入权限字段roles
 * 3）Token的处理：提取Name、提取载荷claim(含权限字段roles)
 * **********************************************/

@Component
public class JwtTokenUtil {
    private final UserService userService;

    @Value("${jwt.secret}") // 从yml配置文件读取密钥
    private String secret;

    @Value("${jwt.expiration}") // ADMIn管理员Token过期时间，默认1小时
    private Long expiration;

    @Value("${jwt.expiration_admin}") // Token过期时间
    private Long expiration_admin;

    final Logger logger = LoggerFactory.getLogger(getClass());

    public JwtTokenUtil(UserService userService) {
        this.userService = userService;
    }

    public String getSecret() {
        return secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    // yml中密钥必须至少32字符（HS256要求256位）
    private SecretKey getSecretKey() {
        // 添加null检查
        if (secret == null) {
            logger.info("JWT密钥未配置！请检查application.yml中的jwt.secret设置");
            throw new IllegalStateException("JWT密钥未配置！请检查application.yml中的jwt.secret设置");
        }

        if (secret.length() < 32) {
            logger.info("密钥长度不足，至少需要32字符");
            throw new IllegalArgumentException("密钥长度不足，至少需要32字符");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    //用户登录后生成token，将传入的user信息生成唯一的jwt令牌token返回请求客户端
    public String generateToken(String emailName, String roleName, String nickName, Integer tokenVersion) {
        SecretKey key = getSecretKey();// 确保yml配置密钥有效
        Map<String, Object> claims = new HashMap<>();

        claims.put("roles", List.of("ROLE_" + roleName));
        claims.put("nickName", nickName);

        // 判断是否为管理员ADMIN角色，根据角色设置不同的有效期，ADMIN的有效期更短
        boolean isAdmin = roleName.equals("ADMIN");
        long expirationTime = isAdmin ? expiration_admin : expiration;

        if (tokenVersion != null) {
            claims.put("tokenVersion", tokenVersion);
        }

        return Jwts.builder()
                .setClaims(claims)
                .subject(emailName) // 新版API直接设置subject
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime * 1000)) //YML配置、数据库中存储为秒，系统函数默计算默认为毫秒
                .signWith(key, SignatureAlgorithm.HS256) // 自动推断算法
                .compact();
    }

    //从请求头Header中提取token
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // 从 Token 中提取Name,这里传入的是email，作为唯一标识
    public String getNameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 从token中提取roles权限字段
    public List<String> extractRolesFromToken(Claims claims) {
        try {
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            logger.warn("无法从Token中提取角色信息: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // 检查 Token 是否过期
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 获取 Token 的过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 通用方法：从 Token 中提取指定信息
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 解析 Token 的所有 Claims（有效载荷）
    public Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token已过期", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Token格式错误", e);
        } catch (SignatureException e) {
            throw new RuntimeException("Token签名无效", e);
        } catch (Exception e) {
            throw new RuntimeException("解析Token失败: " + e.getMessage(), e);
        }
    }

}

