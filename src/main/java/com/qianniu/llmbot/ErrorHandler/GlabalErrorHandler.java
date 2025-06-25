package com.qianniu.llmbot.ErrorHandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/*********************************************
 * 全局异常集中处理，主要处理路径请求响应方法函数，使用注解产生的异常
 * 包括响应函数请求参数的注解校验如@RequestBody @Valid@RequestParam、JWT响应方法函数权限注解校验@PreAuthorize
 * **********************************************/


//用于响应函数中的参数注解校验的Error异常处理
/*用于RestController响应函数中接受HTTP请求携带参数的校验
 * 1、POST请求携带参数body，其中通过定义的请求实体如RegisterRequestStudent来定义参数request(@RequestBody @Valid RegisterRequestUser request)，通过request来接受请求body中各参数；
 *    RegisterRequestStudent中定义的各字段变量的名称、类型，必须与请求参数携带的body中的字段名称、类型保持一致，才能够通过注解灵活映射读入参数；
 *    从body参数映射到request变量的过程中，通过在RegisterRequestStudent实体中定义字段变量的同时使用注解进行校验，如 @NotBlank @Email @Size(min = 6, max = 20, message = "密码长度需6-20位")等；
 *    如果body的参数不符合校验规则，则不会映射到request，也不会进行后续的响应函数。同时，通过定义的ValidErrorHandler进行错误处理，以便提示输入body中字段参数错误的原因
 *
 * 2、GET请求通过URL携带参数，如/email?email=bob@163.com,此时不是通过请求实体来定义参数并映射，而是直接定义变量进行映射url中参数
 *    没有请求实体，在响应函数中直接定义相关参数变量，同时直接使用校验注解如@RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email；
 *    响应函数中自定义的参数变量，要确保与请求url中携带字段名称相同、类型相同，才能进行校验、映射；
 *    响应函数中定义+注解变量，即属于方法级注解校验，需要在RestController类前添加@Validated开启方法级校验；
 *    如果不符合校验规则，则url中携带参数不会映射到自定义变量，也不会进行后续的响应函数。同时，通过定义的ValidErrorHandler进行错误处理，以便提示输入body中字段参数错误的原因
 *
 * */

//全局参数校验异常处理，即对请求响应函数中，各请求参数是否符合校验注解的异常进行处理
@RestControllerAdvice
public class GlabalErrorHandler {
    final Logger logger = LoggerFactory.getLogger(getClass());

    //处理@RequestBody校验，即POST请求校验
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        logError(error.getField(), error.getRejectedValue(), error.getDefaultMessage());
        return Map.of("error", error.getDefaultMessage());
    }

    //处理@RequestParam校验，即GET请求校验
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, String> handleParamValidation(ConstraintViolationException ex) {
        ConstraintViolation<?> violation = ex.getConstraintViolations().iterator().next();
        String fieldName = violation.getPropertyPath().toString();
        logError(fieldName, violation.getInvalidValue(), violation.getMessage());
        return Map.of("error", violation.getMessage());
    }

    // 处理缺失必需参数的情况
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Map<String, String> handleMissingParam(MissingServletRequestParameterException ex) {
        String errorMsg = String.format("缺少必需的请求参数: %s", ex.getParameterName());
        logError(ex.getParameterName(), null, errorMsg);
        return Map.of("error", errorMsg);
    }

    // 处理role权限不足异常
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "操作被拒绝，权限不足!");
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body);
    }

    // 统一日志记录方法
    private void logError(String field, Object value, String reason) {
        if (value == null) {
            logger.error("参数错误 - 字段: {}, 原因: {}", field, reason);
        } else {
            logger.error("参数校验失败 - 字段: {}, 错误值: {}, 原因: {}", field, value, reason);
        }
    }

}

