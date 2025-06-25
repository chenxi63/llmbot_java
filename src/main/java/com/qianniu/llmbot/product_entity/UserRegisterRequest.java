package com.qianniu.llmbot.product_entity;

import jakarta.validation.constraints.*;

/*********************************************
 * User注册Register实体自定义各字段，用于接收新注册user的各字段，并进行自动校验；
 * 校验成功的注册信息，才会存储到数据库；
 * **********************************************/

public class UserRegisterRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(
            regexp = "^\\+\\d{10,15}$",
            message = "手机号格式应为+国家码号码，例如：+8613912345678"
    )
    private String phone; // 手机号可为空,非必填

    private String name; //非必填，默认为email

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需8-20位")
    private String password;

    // Getter & Setter
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
