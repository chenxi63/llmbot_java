package com.qianniu.llmbot.Filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;

/*********************************************
 * 客户端HTTP流式响应过滤器，针对所有流式请求路径，自动设置添加HTTP的请求Header中相关参数
 * **********************************************/


/*
* SSE（Server-Sent Events）是一种基于 HTTP 的服务器推送技术，允许服务器单向向客户端（如浏览器）实时推送数据。
* 它适用于需要服务器主动通知客户端的场景（如实时消息、股票行情、日志流等）。
* 仅服务器 → 客户端（客户端不能通过 SSE 向服务器发送数据）
* 基于HTTP无需 WebSocket 等复杂协议，兼容性更好
* 长连接服务器保持连接打开，持续推送数据
* 自动重连连接中断后，客户端会自动尝试重新连接
* 轻量级协议简单，适合文本数据（如 JSON、纯文本）
* Content-Type必须为 text/event-stream
* */

//StreamingHeadersFilter过滤器，针对定义的特定HTTP请求路径(/chat)设置响应头参数，即chat默认模式为SSE流式响应
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter(
        filterName = "streamingHeadersFilter",
        urlPatterns = "/*",
        asyncSupported = true  // 明确启用异步支持
)
public class StreamingHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (httpRequest.getRequestURI().startsWith("/api/chat"))
        {
            // 设置禁用缓冲的响应头，默认流式响应
            httpResponse.setHeader("X-Accel-Buffering", "no");
            httpResponse.setHeader("Cache-Control", "no-store");
            httpResponse.setHeader("Connection", "keep-alive");
            httpResponse.setHeader("Content-Type", "text/event-stream");  // 适用于SSE
        }

        // 继续过滤器链
        chain.doFilter(request, response);
    }

}