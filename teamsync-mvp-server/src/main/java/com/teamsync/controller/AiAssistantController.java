package com.teamsync.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.teamsync.common.Result;
import com.teamsync.common.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    @Value("${dify.api-base-url}")
    private String difyBaseUrl;

    @Value("${dify.api-key}")
    private String difyApiKey;

    /**
     * 发送消息到 Dify AI Agent（流式 SSE 模式）
     */
    @PostMapping("/chat")
    public void chat(@RequestBody java.util.Map<String, String> request, HttpServletResponse response) throws IOException {
        String query = request.get("query");
        String conversationId = request.getOrDefault("conversation_id", "");

        // 参数校验
        if (query == null || query.isBlank()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":400,\"msg\":\"消息不能为空\"}");
            return;
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"用户未登录\"}");
            return;
        }

        // SSE 响应头 - 禁用缓存和缓冲
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        PrintWriter writer = response.getWriter();

        JSONObject inputs = JSONUtil.createObj();
        inputs.set("userId", userId.toString());

        JSONObject body = JSONUtil.createObj()
                .set("inputs", inputs)
                .set("query", query)
                .set("response_mode", "streaming")
                .set("conversation_id", conversationId)
                .set("user", userId.toString());

        HttpURLConnection conn = null;
        try {
            URL url = new URL(difyBaseUrl + "/chat-messages");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + difyApiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                writer.write("data: " + JSONUtil.createObj()
                        .set("event", "error")
                        .set("code", responseCode)
                        .set("msg", "AI 服务请求失败: " + responseCode) + "\n\n");
                writer.flush();
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line + "\n");
                    writer.flush();

                    // 检测结束或错误事件，主动关闭连接
                    if (line.startsWith("data: ")) {
                        try {
                            JSONObject event = JSONUtil.parseObj(line.substring(6));
                            String eventType = event.getStr("event");
                            if ("message_end".equals(eventType) || "error".equals(eventType)) {
                                return;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                writer.write("data: " + JSONUtil.createObj()
                        .set("event", "error")
                        .set("code", 500)
                        .set("msg", "AI 服务调用异常: " + e.getMessage()) + "\n\n");
                writer.flush();
            } catch (Exception ignored) {
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 查询会话历史
     */
    @GetMapping("/conversations")
    public Result<?> getConversations() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        try (HttpResponse httpResponse = HttpRequest.get(difyBaseUrl + "/conversations")
                .header("Authorization", "Bearer " + difyApiKey)
                .form("user", userId.toString())
                .form("limit", 20)
                .timeout(10000)
                .execute()) {

            if (!httpResponse.isOk()) {
                return Result.error("获取会话列表失败: " + httpResponse.getStatus());
            }

            JSONObject result = JSONUtil.parseObj(httpResponse.body());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取会话列表异常: " + e.getMessage());
        }
    }

    @PostMapping("/conversations/delete")
    public Result<?> deleteConversation(@RequestBody java.util.Map<String, String> request) {
        String conversationId = request.get("conversation_id");
        if (conversationId == null || conversationId.isBlank()) {
            return Result.error("会话ID不能为空");
        }

        try (HttpResponse httpResponse = HttpRequest.delete(difyBaseUrl + "/conversations/" + conversationId)
                .header("Authorization", "Bearer " + difyApiKey)
                .timeout(10000)
                .execute()) {

            if (!httpResponse.isOk()) {
                return Result.error("删除会话失败: " + httpResponse.getStatus());
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除会话异常: " + e.getMessage());
        }
    }
}
