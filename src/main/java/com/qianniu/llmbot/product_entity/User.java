package com.qianniu.llmbot.product_entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

/*********************************************
 * User实体自定义各字段，用于user数据库存储
 * 1）uuid、emmail是核心非空唯一标识；role为会员角色；
 * 2）其中user_id与users表的uuid字段形成外键约束，即只有user_id先在users表中的uuid存在，用户先注册登录才能再聊天并存储记录；
 * 3）因外键约束限制，只有先删除messages表，才能删除users表;
 * **********************************************/

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", unique = true, length = 20, columnDefinition = "VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin")
    private String phone;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "passwordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "role", columnDefinition = "TINYINT DEFAULT 0")
    private Integer role = 0;  // 0=普通用户,1=会员,2=超级会员,3=管理员

    @Column(name = "tokenVersion", columnDefinition = "INT DEFAULT 0")
    private Integer tokenVersion = 0;  // token版本号

    @Column(name = "collectModels", nullable = false, length = 1000)
    private String collectModels;

    @Column(name = "createdAt", nullable = false)
    private Long createdAt = 0L;  // Unix时间戳(秒)

    @Column(name = "updatedAt", nullable = false)
    private Long updatedAt = 0L;  // Unix时间戳(秒)

    @Column(name = "lastLogin")
    private Long lastLogin = 0L;  // Unix时间戳(秒)，0表示从未登录

    @Column(name = "membershipExpiry")
    private Long membershipExpiry = 0L;  // Unix时间戳(秒)，0表示普通用户(非会员)

    // 枚举定义
    public enum Role {
        NORMAL(0), MEMBER(1), SUPER_MEMBER(2), ADMIN(3);

        private final int value;

        Role(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Role fromValue(int value) {
            for (Role role : Role.values()) {
                if (role.value == value) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Invalid role value: " + value);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public String getCollectModels() { return collectModels;}

    public void setCollectModels(String collectModels) { this.collectModels = collectModels; }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Long getMembershipExpiry() {
        return membershipExpiry;
    }

    public void setMembershipExpiry(Long membershipExpiry) {
        this.membershipExpiry = membershipExpiry;
    }

    // 辅助方法：获取role值对应String
    @Transient  // 表示这不是持久化字段
    @JsonIgnore // 不序列化此字段
    public Role getRoleEnum() {
        return Role.fromValue(role);
    }

    // 辅助方法：设置role值对应String
    @Transient  // 表示这不是持久化字段
    @JsonIgnore // 不序列化此字段
    public void setRoleEnum(Role role) {
        this.role = role.getValue();
    }

    // 辅助方法：判断role是否为管理员ADMIN
    @Transient  // 表示这不是持久化字段
    @JsonIgnore // 不序列化此字段
    public boolean isAdmin() {
        return role != null && role == Role.ADMIN.getValue();
    }

    // 辅助方法：判断role是否为会员
    @Transient  // 表示这不是持久化字段
    @JsonIgnore // 不序列化此字段
    public boolean isMember() {
        return role != null && role >= Role.MEMBER.getValue();
    }

    @Override
    public String toString() {
        return String.format(
                "User[id=%s, uuid=%s, email=%s, phone=%s, name=%s, role=%s, createdAt=%s, updatedAt=%s, lastLogin=%s, membershipExpiry=%s]",
                getId(), getUuid(), getEmail(), getPhone(), getName(), getRole(), getCreatedAt(), getUpdatedAt(), getLastLogin(), getMembershipExpiry()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
