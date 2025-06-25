package com.qianniu.llmbot.ErrorHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.net.ConnectException;


/*********************************************
* 流式响应的Chunk操作的异常处理
* **********************************************/

//专用于构建ChatResponse中构建Chunk的Error异常处理
@Slf4j
@Component
public class ChunkErrorHandler {
    private final ObjectMapper objectMapper;

    // 错误类型枚举
    public enum ErrorType {
        CHUNK_BUILD_FAILED,
        API_CONNECTION_ERROR,
        JSON_PROCESSING_ERROR,
        UNKNOWN_ERROR
    }

    // 构造器注入
    public ChunkErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 全局异常处理方法
     * @param throwable 原始异常
     * @param rawChunk 引发异常的原始chunk数据（可为null）
     * @return 错误响应chunk的JSON字符串
     */
    public String handleError(Throwable throwable, String rawChunk) {
        ErrorChunk errorChunk = buildErrorChunk(throwable, rawChunk);
        try {
            return objectMapper.writeValueAsString(errorChunk);
        } catch (JsonProcessingException e) {
            log.error("生成错误chunk时发生序列化异常", e);
            return fallbackErrorResponse();
        }
    }

    /**
     * 转换为错误流（保持响应流不中断）
     * @param throwable 异常
     * @param rawChunk 原始chunk
     * @return 包含错误chunk的Flux
     */
    public Flux<String> handleAsFlux(Throwable throwable, String rawChunk) {
        return Flux.just(handleError(throwable, rawChunk));
    }

    // 构建错误chunk对象
    private ErrorChunk buildErrorChunk(Throwable throwable, String rawChunk) {
        ErrorType errorType = classifyError(throwable);
        logError(errorType, throwable, rawChunk);

        return ErrorChunk.builder()
                .errorType(errorType.name())
                .errorCode(getHttpStatus(errorType).value())
                .errorMessage(getUserFriendlyMessage(errorType))
                .errorDetail(throwable.getMessage())
                .rawChunk(rawChunk)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 错误分类
    private ErrorType classifyError(Throwable throwable) {
        if (throwable instanceof JsonProcessingException) {
            return ErrorType.JSON_PROCESSING_ERROR;
        } else if (throwable instanceof IllegalStateException) {
            return ErrorType.CHUNK_BUILD_FAILED;
        } else if (throwable.getCause() instanceof ConnectException) {
            return ErrorType.API_CONNECTION_ERROR;
        }
        return ErrorType.UNKNOWN_ERROR;
    }

    // 错误日志记录
    private void logError(ErrorType errorType, Throwable throwable, String rawChunk) {
        String template = "流式响应处理失败 [type={}] | chunk={}";
        switch (errorType) {
            case JSON_PROCESSING_ERROR:
                log.error(template, errorType, rawChunk, throwable);
                break;
            case API_CONNECTION_ERROR:
                log.warn(template, errorType, rawChunk);
                break;
            default:
                log.error(template, errorType, rawChunk, throwable);
        }
    }

    // 获取对应的HTTP状态码
    private HttpStatus getHttpStatus(ErrorType errorType) {
        return switch (errorType) {
            case API_CONNECTION_ERROR -> HttpStatus.BAD_GATEWAY;
            case JSON_PROCESSING_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    // 用户友好错误信息
    private String getUserFriendlyMessage(ErrorType errorType) {
        return switch (errorType) {
            case CHUNK_BUILD_FAILED -> "数据块构建失败";
            case API_CONNECTION_ERROR -> "上游服务连接异常";
            case JSON_PROCESSING_ERROR -> "数据格式错误";
            default -> "系统内部错误";
        };
    }

    // 兜底错误响应
    private String fallbackErrorResponse() {
        return """
            {"errorType":"SYSTEM_ERROR","errorCode":500,"errorMessage":"无法生成错误信息"}
            """;
    }
}
