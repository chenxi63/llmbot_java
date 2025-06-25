package com.qianniu.llmbot.web;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*********************************************
 * 公共路径请求响应函数，不需要鉴权，用于开发测试
 * **********************************************/

@RestController
@RequestMapping(value = "/api/info")
public class ApiController {

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE) //localhost:8080/api
    public Map<String, String> apiInfo() {
        return Map.of(
                "version", "1.0",
                "developer", "chenxi",
                "message", "llmbot"
        );
    }

}


