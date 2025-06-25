package com.qianniu.llmbot.ErrorHandler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorChunk {
    private String errorType;
    private Integer errorCode;
    private String errorMessage;
    private String errorDetail;
    private String rawChunk;
    private Long timestamp;
}
