package com.qianniu.llmbot.product_entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/*********************************************
 * User注册Register响应返回Response的实体自定义各字段，用于脱敏后的user信息返回客户端；
 * **********************************************/

//UserRegisterResponseDTO是User实体的变体，用于返回用户register注册成功后，返回的脱敏信息(如密码等)
//DTO专用于消息返回的脱敏实体，由于不需要查询数据库、响应处理等操作，因此不必定义对应的service
//普通User实体用于数据库操作，DTO实体用于接口传输，职责分离更安全。
public class UserRegisterResponseDTO {
    private String uuid;
    private String email;
    private String name;
    private String phone;
    private String roleName = "NORMAL";  // 0=普通用户,1=会员,2=超级会员,3=管理员
    private int tokenVersion;
    private String collectModels;
    private String createdDateTime;  // Unix时间戳(秒)
    private String updatedDateTime;
    private String lastLoginTime;
    private String membershipExpiryTime;

    static final Logger logger = LoggerFactory.getLogger(UserRegisterResponseDTO.class);


    // 静态工厂方法：从User对象转换
    public static UserRegisterResponseDTO fromUser(User user) {
        UserRegisterResponseDTO dto = new UserRegisterResponseDTO();
        dto.setUuid(user.getUuid());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setRoleName(getRoleName(user.getRole())); //将role(0/1/2/3)转换成roleName
        dto.setTokenVersion(user.getTokenVersion());
        dto.setCollectModels(user.getCollectModels());
        dto.setCreatedDateTime(formatTimestamp(user.getCreatedAt()));  //将UNIX时间戳转换成正常格式
        dto.setUpdatedDateTime(formatTimestamp(user.getUpdatedAt()));
        dto.setLastLoginTime(formatTimestamp(user.getLastLogin()));
        dto.setmembershipExpiryTime(formatTimestamp(user.getMembershipExpiry()));
        return dto;
    }

    // Getter & Setter
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public String getCollectModels() { return collectModels;}

    public void setCollectModels(String collectModels) { this.collectModels = collectModels; }

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getUpdatedDateTime() {
        return updatedDateTime;
    }

    public void setUpdatedDateTime(String updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getmembershipExpiryTime() {
        return membershipExpiryTime;
    }

    public void setmembershipExpiryTime(String membershipExpiryTime) {
        this.membershipExpiryTime = membershipExpiryTime;
    }

    // 角色名称（便于前端显示）
    private static String getRoleName(int role) {
        switch (role) {
            case 1: return "MEMBER";
            case 2: return "SUPER_MEMBER";
            case 3: return "ADMIN";
            default: return "NORMAL";
        }
    }

    // 时间戳转标准格式（私有工具方法）
    private static String formatTimestamp(long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format(
                "UserRegisterResponseDTO[uuid=%s, email=%s, name=%s, role=%s, createdAt=%s]",
                uuid, email, name, roleName, createdDateTime
        );
    }
}


