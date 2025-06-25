package com.qianniu.llmbot.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianniu.llmbot.ErrorHandler.ChunkErrorHandler;
import com.qianniu.llmbot.JWTtoken.JwtTokenUtil;
import com.qianniu.llmbot.model_entity.AL_TextModel;
import com.qianniu.llmbot.model_service.AL_TextModelRequestService;
import com.qianniu.llmbot.product_entity.*;
import com.qianniu.llmbot.product_service.MessageService;
import com.qianniu.llmbot.product_service.ModelService;
import com.qianniu.llmbot.product_service.UserService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.*;

/*********************************************
 * 阿里百炼平台的模型API请求响应函数定义
 * 1）流式响应下，请求参数的校验不能通过注解操作，需要手动校验content;
 * 2）在请求参数校验成功下，进行jwt token鉴定(token有效+权限登录)，需要手动操作鉴定；
 * 3）将接收的参数封装后，向第三方API发送请求，获得流式响应Chunk;将Chunk封装后返回到客户端；
 * 4）从流式响应Chunk中抽取相关信息，构建message存储到数据库中；
 * **********************************************/


//******注意不同模型下的同名操作函数不要引用错依赖包*************
//********流式响应下，请求参数ChatRequest校验、JWT令牌token鉴权都需要手动操作。请求参数注解校验、JWT令牌Filter拦截器校验不再适用**********
@RestController
@RequestMapping(value = "/api/chat") //基础路径为 "/api/chat"，即后续所有的路径都自动添加 "/api/chat/xxxxx"
public class LLMbotController_AL {
    final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    ChatResponseAL chatResponseAL = new ChatResponseAL();

    private final AL_TextModel al_Text_Model;
    private final AL_TextModelRequestService al_Text_ModelRequestService;

    private final MessageService messageService;
    private final ModelService modelService;

    @Autowired
    private ChunkErrorHandler chunkErrorHandler;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Autowired
    public LLMbotController_AL(AL_TextModel al_Text_Model,AL_TextModelRequestService al_Text_ModelRequestService, ModelService modelService, MessageService messageService) {
        this.al_Text_Model = al_Text_Model;
        this.al_Text_ModelRequestService = al_Text_ModelRequestService;
        this.modelService = modelService;
        this.messageService = messageService;
    }

    /*请求-响应逻辑：
     * 1、先提取请求中的content、提取历史聊天记录，共同构造向第三方APi发送的请求内容；
     * 2、向API发送请求，获得流式响应Chunk;
     * 3、将流式响应各Chunk，重新设计封装再以流式响应方式返回客户端；
     * 4、抽取请求参数、响应Chunk中的相关字段，作为message存储到数据库中；
     * */

    // 该路径的请求，明确输出类型为流式响应TEXT_EVENT_STREAM_VALUE
    // 请求路径：localhost:8080/api/chat/almodel
    @PostMapping(value = "/almodel", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleALModelChat(@RequestBody ChatRequest chatRequest, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 流式响应手动校验ChatRequest中的content(user prompt)是否为空
        if (chatRequest.getContent() == null || chatRequest.getContent().trim().isEmpty()) {
            return Flux.just("错误：问题不能为空");
        }

        //提取数据库中对应请求中携带的model信息,并注入到bd_Text_model实例中
        String modelName = chatRequest.getModelName();
        Model model = modelService.getModelByModelName(modelName);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode modelParameters;
        String modelAllowroles;

        try {
            String modelParametersString = model.getModelParameters(); //提取model_parameters参数
            modelParameters = objectMapper.readTree(modelParametersString);
            modelAllowroles = model.getModelAllowroles();//提取model_allowroles参数
            al_Text_Model.setModel(model.getModelName());
            al_Text_Model.setContentType(model.getModelType());
            al_Text_Model.setUrl(model.getModelUrl());
            al_Text_Model.setStream(modelParameters.get("stream").asBoolean());
            al_Text_Model.setIncrementalOutput(modelParameters.get("incremental_output").asBoolean());
            al_Text_Model.setTemperature(modelParameters.get("temperature").asDouble());
            al_Text_Model.setTopP(modelParameters.get("top-p").asDouble());
            al_Text_Model.setRepetitionPenalty(modelParameters.get("repetition_penalty").asDouble());
            al_Text_Model.setResultFormat(modelParameters.get("result_format").asText());
            al_Text_Model.setEnableSearch(modelParameters.get("enable_search").asBoolean());
            al_Text_Model.setMaxTokens(modelParameters.get("max_tokens").asInt());
            al_Text_Model.setRecordNumbers(modelParameters.get("record-numbers").asInt());
        } catch (Exception e) {
            return Flux.just("错误：模型不存在: ",e.getMessage());
        }

        // 流式响应先手动鉴权token
        String jwtToken; //提取完整token
        String jwtEmailName; //从token中获取携带的Name(email)
        Claims claims;
        List<String> jwtRole; //提取role信息：ROLE_NORMAL、ROLE_MEMBER、ROLE_SUPER_MEMBER、ROLE_ADMIN
        String jwtNickName;
        String jwtUUid;
        Integer jwtTokenVersion;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Flux.just("错误：缺少认证信息，请提供有效的Bearer Token！");
        }else
        {
            jwtToken = authHeader.substring(7); //提取完整token
            jwtEmailName= jwtTokenUtil.getNameFromToken(jwtToken); //从token中获取携带的Name(email)
            claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
            jwtRole =(List<String>) claims.get("roles"); //提取role信息：ROLE_NORMAL、ROLE_MEMBER、ROLE_SUPER_MEMBER、ROLE_ADMIN
            jwtNickName = (String) claims.get("nickName");
            jwtUUid = userService.getUuidByEmail(jwtEmailName);
            jwtTokenVersion = (Integer)claims.get("tokenVersion");//提取版本号

            if (jwtEmailName == null || jwtEmailName.trim().isEmpty()) {
                return Flux.just("错误：Token无效！");
            }
            if(jwtTokenUtil.isTokenExpired(jwtToken)) //判断token是否过期
            {
                return Flux.just("错误：Token已过期！");
            }
            if (!jwtTokenVersion.equals(userService.getTokenVersionByEmail(jwtEmailName))) {
                return Flux.just("错误：Token为旧版本已失效！");
            }
            if (!modelAllowroles.contains(jwtRole.get(0))) {
                return Flux.just("错误：用户权限不足！");
            }

            //排除校验会员时间的情况，即访问来自NORMAL或ADMIN不需要校验会员时间、响应向所有role开放权限也不需要
            //会员超过对应时间，充值role、更新tokenVersion，并生成新token返回客户端，用户不需要login即可替换token
            if(!(jwtRole.get(0).equals("ROLE_NORMAL") || jwtRole.get(0).equals("ROLE_ADMIN") || modelAllowroles.contains("ROLE_NORMAL"))) {
                User userOld = userService.getUserByEmail(jwtEmailName); //获取当前user信息
                //根据当前user信息判断会员过期,系统函数默认为毫秒，数据库存储为秒
                if(System.currentTimeMillis() > (userOld.getMembershipExpiry() * 1000))
                {
                    userService.updateRoleByEmail(0, jwtEmailName); //更新user信息，重置role为NORMAL用户、重置membershipExpiry会员到期时间为0
                    userService.updateTokenVersionByEmail(jwtEmailName); //更新用户 tokenVersion 版本号
                    User userNew = userService.getUserByEmail(jwtEmailName); //查询更新后的user信息
                    UserRegisterResponseDTO userDto = UserRegisterResponseDTO.fromUser(userNew); //user信息脱敏

                    // 将user信息封装生成token，生产环境下应该直接注入响应Header
                    String token = jwtTokenUtil.generateToken(userDto.getEmail(),userDto.getRoleName(),userDto.getName(), userDto.getTokenVersion());

                    //将新token返回客户端，生产环境下应该即时注入响应Header
                    //response.setHeader("Authorization", "Bearer " + newToken);
                    //response.setHeader("Access-Control-Expose-Headers", "Authorization");
                    return Flux.just("会员到期，已恢复为普通用户！新token为：" + token);
                }
            }

        }

        final int[] tokenUsage = {0, 0, 0};  //用于LastChunk存储token使用量 prompt, completion, total
        StringBuilder botResponseContent = new StringBuilder();  //用于收集所有ChunkJson的容器
        List<Map<String, String>> chatHistory = new ArrayList<>(); //用户查询并封装聊天记录

        if (chatRequest.getIsNewChat() == null || chatRequest.getIsNewChat() != 1){

            // 确定要获取的历史记录数量
            int historyMsgNumber = al_Text_Model.getRecordNumbers(); // 默认使用数据库models表中的模型参数

            // 如果请求中指定了历史记录数量且小于配置的最大值，则使用请求的数量
            if (chatRequest.getHisMsgNumber() != null
                    && chatRequest.getHisMsgNumber() > 0
                    && chatRequest.getHisMsgNumber() < al_Text_Model.getRecordNumbers()) {
                historyMsgNumber = chatRequest.getHisMsgNumber();
            }
            //根据conversion_id读取最新的N条Message记录，并在其中提取信息封装成chatHistory
            //注意模型带历史记录的格式要求：其实必须为role = user或system，中间role = user或assistant、末尾必须为role = user
            //注入模型的RecordNumbers固定参数(由yml配置读入)，即最多历史记录条数，与模型的最大输入token数相关
            chatHistory = al_Text_ModelRequestService.getHistoryMessage(al_Text_Model.getModel() + "_" + jwtUUid, historyMsgNumber);

        }

        //从请求参数chatRequest中提取最新的user prompt,叠加chatHistory、基础预定义参数共同构成第三方API的POST请求的完整body
        Map<String, Object> buildRequestBody = al_Text_ModelRequestService.buildCompleteRequest(al_Text_Model.getBaseRequestParams(), chatRequest.getContent(), chatHistory);

        //向第三方API发送POST请求获取流式响应Chunk,并构建全新的Chunk用于返回客户端；同时收集所有响应块中有效信息，注入到message表中
        return al_Text_ModelRequestService.sendRequest(al_Text_Model.getUrl(), buildRequestBody)
                .index()
                .concatMap(tuple -> Mono.fromCallable(() -> { // 保证顺序处理
                            long index = tuple.getT1();
                            String apiChunk = tuple.getT2(); //获得每个chunkjson
                            extractAndSaveContent(apiChunk, botResponseContent);//先提取每个Chunk中的"content"内容并添加保存到botResponseContent

                            String responseChunk;
                            try {
                                if (index == 0) {  // 根据原始chunkjson的首、中、尾类型构建面向客户端响应的Chunk
                                    responseChunk = chatResponseAL.buildFirstChunk(chatRequest, apiChunk, al_Text_Model.getModel(), jwtUUid, jwtNickName);
                                } else if (chatResponseAL.isLastChunk(apiChunk)) {  // 提取token消耗数据
                                    JsonNode rootNode = objectMapper.readTree(apiChunk);
                                    if (rootNode.has("usage")) {
                                        JsonNode usage = rootNode.path("usage");
                                        tokenUsage[0] = usage.path("input_tokens").asInt(0); //注意：带历史记录的对话，prompt_tokens会包含历史记录tokens，而不是单纯的最新user prompt
                                        tokenUsage[1] = usage.path("output_tokens").asInt(0);
                                        tokenUsage[2] = usage.path("total_tokens").asInt(0);
                                    }
                                    responseChunk = chatResponseAL.buildLastChunk(apiChunk);// 先发送最后一个数据块
                                } else {
                                    responseChunk = chatResponseAL.buildMiddleChunk(apiChunk);
                                }
                                // 对响应块进行 Base64 编码，避免响应内容中的换行符导致前端错误解析(未接收完整就解析)
                                return Base64.getEncoder().encodeToString(responseChunk.getBytes(StandardCharsets.UTF_8));
                            } catch (Exception e) {
                                return chunkErrorHandler.handleError(e, apiChunk); // 使用当前chunk处理错误
                            }

                        }).subscribeOn(Schedulers.boundedElastic())// 确保非阻塞
                )
                .doOnComplete(() -> asyncSaveToDatabase(chatRequest, al_Text_Model.getModel(), jwtUUid, jwtNickName, botResponseContent, tokenUsage, al_Text_Model.getContentType()));  //异步存储
    }

    // 从每个chunkjson中提取"content"的内容并添加保存到StringBuilder botResponseContent容器中
    private void extractAndSaveContent(String apiChunk, StringBuilder contentBuilder) {
        try {
            JsonNode rootNode = objectMapper.readTree(apiChunk);
            if (rootNode.has("output")) {
                JsonNode output = rootNode.path("output");
                if (output.has("choices")) {
                    JsonNode choices = output.path("choices");
                    for (JsonNode choice : choices) {
                        if (choice.has("message")) {
                            JsonNode message = choice.path("message");
                            String content = message.path("content").asText("");
                            if (!content.isEmpty()) {
                                contentBuilder.append(content);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析chunk失败: {}", apiChunk, e);
        }
    }

    //异步存储，从请求参数、响应参数抽取message字段，异步存储到数据库中
    private void asyncSaveToDatabase(ChatRequest request, String botName, String uUid, String nickName, StringBuilder content, int[] usage, int contentType) {
        Mono.fromRunnable(() -> {
            if (content.length() > 0) {
                messageService.asyncMessageRegister(
                        botName,
                        uUid,
                        nickName,
                        botName + "_" + uUid,
                        usage[2],
                        request.getContent(),
                        request.getContentType(),
                        usage[0], //注意带历史记录的query_token_number，不是单纯的user prompt
                        content.toString(),
                        contentType,
                        usage[1]
                );
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

}
