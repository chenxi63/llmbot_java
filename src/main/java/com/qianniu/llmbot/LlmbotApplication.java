package com.qianniu.llmbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/*响应函数中的同步与异步方法：
 * 1、传统RestController响应函数是同步的，响应返回前会始终占用Tomcat线程，只能用于快速完成、低耗时的响应逻辑，如数据库查询、简单计算；
 *
 * 2、异步响应机制：返回Callable <类型>对象、返回DeferredResult<类型对象>，适合中等并发(数百并发)，高并发下线程切换开销大；
 *   ①返回Callable <类型>对象：简单异步，不占用tomcat线程；只适合短任务的并发处理，如数据库查询、简单计算
 *     -响应函数返回public Callable<T>，要返回的数据(字符串或Map/List数据集合)会封装在Callable<T>中；
 *     -Callable<T>会自动提交到内部的任务执行器TaskExecutor，不必手动操作Thread线程
 *     -响应函数内部执行return时，才会触发结果返回客户端
 *
 *   ②返回DeferredResult<类型>对象：长任务异步/跨线程通信，不占用tomcat线程；适合长任务、外部任务(其他线程/消息队列/事件/远程调用/文件处理)等
 *     -DeferredResult是一个结果容器，可以在任意线程中向该容器的对象实例注入结果——deferredResult.setResult
 *     -当通过deferredResult.setResult手动向容器对象实例注入结果时，主线程立即返回结果到客户端
 *
 * 3、面向客户端HTTP请求任务(长任务)的同步响应机制RestTemplate：专用于响应函数中处理HTTP请求（第三方API），同步机制会占用tomcat线程；
 *
 * 4、面向客户端HTTP请求任务(长任务)的专用异步响应机制WebClient：高并发、非阻塞HTTP请求，适合大规模高并发(数万并发连接)；
 * */


@SpringBootApplication
public class LlmbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmbotApplication.class, args);
    }

    // 配置WebClient组件，用于响应函数中的H高并发TTP请求类，包括发送GET/POST请求、处理响应数据等
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

}
