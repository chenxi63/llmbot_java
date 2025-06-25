package com.qianniu.llmbot.product_entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*********************************************
 * User登录Login实体自定义各字段，用于接收登录user的各字段，并进行自动校验；
 * 校验成功的登录信息，才会进行鉴权等后续操作；
 * **********************************************/

public class UserLoginRequest {
    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(
            regexp = "^\\+\\d{10,15}$",
            message = "手机号格式应为+国家码号码，例如：+8613912345678"
    )
    private String phone;


    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需8-20位")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
