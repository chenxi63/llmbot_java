package com.qianniu.llmbot.product_service;

import com.qianniu.llmbot.product_entity.Message;
import com.qianniu.llmbot.product_entity.User;
import com.qianniu.llmbot.product_entity.UserRegisterResponseDTO;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;


/*********************************************
 * User Service中用户user的相关行为方法定义
 * 1) 新user注册、按照各字段查询user、判断相关字段是否存在；
 * 2）充值操作后会员角色字段、会员到期时间字段更新(未在Contrller中定义具体的请求响应函数)；登录成功的上次登录时间字段更新等；
 * **********************************************/

@Component
@Transactional
public class UserService {
    @Value("${roledays.member_days}")
    private int memberDays;

    @Value("${roledays.supermember_days}")
    private int supermemberDays;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);//日志记录器，日志输出时会自动标记类名（如 UserService），便于过滤和排查问题。
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); //用于密码的编码器变量

    @Autowired
    JdbcTemplate jdbcTemplate;

    RowMapper<User> userRowMapper = new BeanPropertyRowMapper<>(User.class);//将数据库查询结果集ResultSet的每一行自动映射到User实体实例中

    //按照id查询单个user，queryForObject只用于处理单行结果
    public User getUserById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", userRowMapper, id);
    }

    //按照uuid查询单个user,queryForObject只用于处理单行结果
    public User getUserByUuid(String uuid) {
        return jdbcTemplate.queryForObject("SELECT * FROM users WHERE uuid = ?", userRowMapper, uuid);
    }

    //按照email查询单个user，queryForObject只用于处理单行结果
    public User getUserByEmail(String email) {
        return jdbcTemplate.queryForObject("SELECT * FROM users WHERE email = ?", userRowMapper, email);
    }

    //按照email查询uuid
    public String getUuidByEmail(String email) {
        return jdbcTemplate.queryForObject("SELECT uuid FROM users WHERE email = ?",String.class, email);
    }

    //按照email充值设置会员role角色，用于recharge
    public int updateRoleByEmail(int role, String email) {
        long expiryTime = 0;

        //非NORMAL时才进行会员加期计算
        if (role > 0) {
            long currentTime = System.currentTimeMillis() / 1000; //系统函数默认为毫秒，数据库存储为秒
            int daysToAdd = switch (role) {
                case 1 -> memberDays;      // 普通会员
                case 2 -> supermemberDays; // 超级会员
                default -> 0;  //ADMIN(3)默认为0
            };

            //ADMIN(3)时不执行会员时间加期
            if (daysToAdd > 0) {
                expiryTime = currentTime + (daysToAdd * 86400L); // 天数转秒
            }
        }
        updateUpdatedAt(email);//记录更新时间
        return jdbcTemplate.update("UPDATE users SET role = ?, membershipExpiry = ? WHERE email = ?", role, expiryTime, email);
    }

    //按照email更新tokenVersion(+1)，用于logout
    public int updateTokenVersionByEmail(String email) {
        return jdbcTemplate.update("UPDATE users SET tokenVersion = tokenVersion + 1 WHERE email = ?", email);
    }

    //按照email查询tokenVersion, 用于login后的token注入、token版本号校验
    public Integer getTokenVersionByEmail(String email) {
        return jdbcTemplate.queryForObject("SELECT tokenVersion FROM users WHERE email = ?", Integer.class, email);
    }

    //根据uuid更新collectModels字段
    public void updateCollectModelsByUuid(String uuid, String collectModels) {
        jdbcTemplate.update("UPDATE users SET collectModels = ? WHERE uuid = ?", collectModels, uuid);
    }

    //按照phone查询单个user，queryForObject只用于处理单行结果
    public User getUserByPhone(String phone) {
        return jdbcTemplate.queryForObject("SELECT * FROM users WHERE phone = ?", userRowMapper, phone);
    }

    //按照name查询多个user，query处理多行结果
    public List<User> getUserByName(String name) {
        return jdbcTemplate.query("SELECT * FROM users WHERE name = ?", userRowMapper, name);
    }

    //按照role查询多个user，query处理多行结果
    public List<User> getUserByRole(int role) {
        return jdbcTemplate.query("SELECT * FROM users WHERE role = ?", userRowMapper, role);
    }

    //email是否已经存在
    public boolean isEmailExist(String email) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email );
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    //查询phone是否存在
    public boolean isPhoneExist(String phone) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ?", Integer.class, phone);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    // 用于登录成功后记录时间（当前时间）
    public int updateLastLogin(long userId) {
        return jdbcTemplate.update("UPDATE users SET lastLogin = UNIX_TIMESTAMP() WHERE id = ?", userId);
    }

    // 用于记录字段变化的时间
    public int updateUpdatedAt(String email) {
        return jdbcTemplate.update("UPDATE users SET updatedAt = UNIX_TIMESTAMP() WHERE email = ?", email);
    }


    //新user注册，返回新用户user的脱敏信息
    public UserRegisterResponseDTO registerUser(String email, String phone, String name, String password)
    {
        logger.info("try register by {}...", email);

        if (StringUtils.isEmpty(name)) {
            name = email; // 如果用户名为空，默认使用邮箱
        }
        String encodePassword = passwordEncoder.encode(password); //对明码加密

        User user = new User();
        user.setUuid(UUID.randomUUID().toString());  //随机唯一uuid
        user.setEmail(email);
        user.setPhone(phone);
        user.setName(name);
        user.setPasswordHash(encodePassword);
        user.setRole(0); // 默认普通用户
        long now = System.currentTimeMillis() / 1000; //系统函数默认为毫秒，数据库存储为秒
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastLogin(0L); // 未登录
        user.setMembershipExpiry(0L); // 非会员

        KeyHolder holder = new GeneratedKeyHolder();
        int affectedRows = jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (uuid,email, phone, name, passwordHash, role, createdAt, updatedAt, lastLogin, membershipExpiry) " +
                            "VALUES (?,?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, user.getUuid());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getName());
            ps.setString(5, user.getPasswordHash());
            ps.setInt(6, user.getRole());
            ps.setLong(7, user.getCreatedAt());
            ps.setLong(8, user.getUpdatedAt());
            ps.setLong(9, user.getLastLogin());
            ps.setLong(10, user.getMembershipExpiry());
            return ps;
        }, holder);

        if (affectedRows != 1 || holder.getKey() == null) {
            throw new RuntimeException("用户注册失败");
        }
        user.setId(holder.getKey().longValue());

        return UserRegisterResponseDTO.fromUser(user);//返回注册后脱敏user结果
    }


}
