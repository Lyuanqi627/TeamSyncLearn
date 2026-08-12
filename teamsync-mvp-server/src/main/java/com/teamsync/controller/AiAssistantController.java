package com.teamsync.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsync.common.Result;
import com.teamsync.common.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    @Value("${dify.api-base-url}")
    private String difyBaseUrl;

    @Value("${dify.api-key}")
    private String difyApiKey;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 单文件缓存上限（10MB），防止异常的大文件撑爆内存。
     */
    private static final int MAX_CACHE_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Dify 文件代理缓存：key 为 /files/ 路径（去掉签名查询串），value 为文件字节与 Content-Type。
     * Dify 的 files[].url 是带签名临时链接，默认 5 分钟过期；历史消息（GET /messages）返回的是
     * 过期快照且不会重新签名。首次实时拉取时把字节缓存下来，之后即使签名过期也能直接返回缓存，
     * 保证刷新页面后历史图片仍能显示。accessOrder=true 实现 LRU，最多保留 100 条。
     */
    private final Map<String, FileCacheEntry> fileCache =
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, FileCacheEntry> eldest) {
                    return size() > 100;
                }
            });

    private static final class FileCacheEntry {
        final byte[] data;
        final String contentType;

        FileCacheEntry(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }
    }

    /**
     * 将 Dify 返回的 JSON 字符串解析为 Jackson 兼容的 Map。
     * 不能用 hutool 的 JSONObject 直接放进 Result 返回，否则 Jackson 无法序列化 JSONNull。
     */
    private Object parseDifyJson(String body) throws IOException {
        return objectMapper.readValue(body, new TypeReference<java.util.Map<String, Object>>() {});
    }

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
     * 查询会话历史（支持 last_id / limit 分页，Dify 按最近活跃排序）
     */
    @GetMapping("/conversations")
    public Result<?> getConversations(
            @RequestParam(required = false) String last_id,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        HttpRequest req = HttpRequest.get(difyBaseUrl + "/conversations")
                .header("Authorization", "Bearer " + difyApiKey)
                .form("user", userId.toString())
                .form("limit", limit);
        if (last_id != null && !last_id.isBlank()) {
            req.form("last_id", last_id);
        }

        try (HttpResponse httpResponse = req.timeout(10000).execute()) {

            if (!httpResponse.isOk()) {
                return Result.error("获取会话列表失败: " + httpResponse.getStatus());
            }

            return Result.success(parseDifyJson(httpResponse.body()));
        } catch (Exception e) {
            return Result.error("获取会话列表异常: " + e.getMessage());
        }
    }

    /**
     * 查询会话内消息历史（Dify 返回倒序，最新在前，前端负责反转展示）
     *
     * @param conversation_id 会话ID
     * @param first_id        当前已加载最旧一条消息的 id，用于向前翻页加载更早的消息
     * @param limit           每页条数
     */
    @GetMapping("/messages")
    public Result<?> getMessages(
            @RequestParam String conversation_id,
            @RequestParam(required = false) String first_id,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        HttpRequest req = HttpRequest.get(difyBaseUrl + "/messages")
                .header("Authorization", "Bearer " + difyApiKey)
                .form("user", userId.toString())
                .form("conversation_id", conversation_id)
                .form("limit", limit);
        if (first_id != null && !first_id.isBlank()) {
            req.form("first_id", first_id);
        }

        try (HttpResponse httpResponse = req.timeout(10000).execute()) {

            if (!httpResponse.isOk()) {
                return Result.error("获取消息失败: " + httpResponse.getStatus());
            }

            return Result.success(parseDifyJson(httpResponse.body()));
        } catch (Exception e) {
            return Result.error("获取消息异常: " + e.getMessage());
        }
    }

    /**
     * 重命名会话（auto_generate=true 时由 Dify 根据首条消息自动命名）
     */
    @PostMapping("/conversations/rename")
    public Result<?> renameConversation(@RequestBody java.util.Map<String, String> request) {
        String conversationId = request.get("conversation_id");
        if (conversationId == null || conversationId.isBlank()) {
            return Result.error("会话ID不能为空");
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        boolean autoGenerate = Boolean.parseBoolean(request.getOrDefault("auto_generate", "false"));
        JSONObject body = JSONUtil.createObj()
                .set("auto_generate", autoGenerate)
                .set("user", userId.toString());
        if (!autoGenerate) {
            String name = request.get("name");
            if (name == null || name.isBlank()) {
                return Result.error("会话名称不能为空");
            }
            body.set("name", name);
        }

        try (HttpResponse httpResponse = HttpRequest.post(difyBaseUrl + "/conversations/" + conversationId + "/name")
                .header("Authorization", "Bearer " + difyApiKey)
                .contentType("application/json")
                .body(body.toString())
                .timeout(10000)
                .execute()) {

            if (!httpResponse.isOk()) {
                return Result.error("重命名会话失败: " + httpResponse.getStatus());
            }

            return Result.success(parseDifyJson(httpResponse.body()));
        } catch (Exception e) {
            return Result.error("重命名会话异常: " + e.getMessage());
        }
    }

    @PostMapping("/conversations/delete")
    public Result<?> deleteConversation(@RequestBody java.util.Map<String, String> request) {
        String conversationId = request.get("conversation_id");
        if (conversationId == null || conversationId.isBlank()) {
            return Result.error("会话ID不能为空");
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "用户未登录");
        }

        // Dify 删除会话要求 user 放在 JSON body 中
        JSONObject body = JSONUtil.createObj().set("user", userId.toString());

        try (HttpResponse httpResponse = HttpRequest.delete(difyBaseUrl + "/conversations/" + conversationId)
                .header("Authorization", "Bearer " + difyApiKey)
                .contentType("application/json")
                .body(body.toString())
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

    /**
     * 代理 Dify 返回的文件（如思维导图 PNG）。Dify 的 files[].url 是相对路径
     * （如 /files/tools/xxx.png），浏览器无法直接访问，统一经此接口转发。
     * 该接口在 TokenInterceptor 的排除列表中（<img> 无法携带 Authorization 头），
     * 安全性由 resolveDifyFileUrl 的严格白名单保证，只允许访问 Dify 自身的 /files/ 公开文件。
     */
    @GetMapping("/file")
    public void proxyFile(@RequestParam String url, HttpServletResponse response) throws IOException {
        String target = resolveDifyFileUrl(url);
        if (target == null) {
            response.setStatus(400);
            return;
        }

        // 缓存命中：直接返回缓存的字节，不再请求 Dify。
        // 这样历史消息里已经过期的签名 URL（刷新页面后）也能正常显示图片。
        String cacheKey = fileCacheKey(url);
        if (cacheKey != null) {
            FileCacheEntry cached = fileCache.get(cacheKey);
            if (cached != null) {
                response.setContentType(cached.contentType);
                response.setHeader("Cache-Control", "public, max-age=3600");
                try (OutputStream out = response.getOutputStream()) {
                    out.write(cached.data);
                }
                return;
            }
        }

        try (HttpResponse upstream = HttpRequest.get(target).timeout(15000).execute()) {
            if (!upstream.isOk()) {
                response.setStatus(upstream.getStatus());
                return;
            }

            String contentType = upstream.header("Content-Type");
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=3600");

            byte[] bytes = upstream.bodyBytes();
            if (cacheKey != null && bytes.length <= MAX_CACHE_FILE_SIZE) {
                fileCache.put(cacheKey, new FileCacheEntry(bytes, contentType));
            }
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
            }
        }
    }

    /**
     * 从文件 URL 提取缓存键：去掉签名查询串后的 /files/ 路径，非 /files/ 路径返回 null 不缓存。
     * 同一文件的签名 URL 每次都可能不同（timestamp/nonce/sign 不同），但路径部分是稳定的，
     * 用它作为键即可让过期的 URL 命中首次拉取时缓存的字节。
     */
    private String fileCacheKey(String url) {
        int queryIdx = url.indexOf('?');
        String path = queryIdx >= 0 ? url.substring(0, queryIdx) : url;
        if (!path.startsWith("/files/")) {
            return null;
        }
        return path;
    }

    /**
     * 校验并解析 Dify 文件 URL -> 可请求的完整地址，非法输入返回 null。
     * 只放行 Dify 自身 /files/ 下的文件路径（相对）或与 Dify origin 同 host 的 URL，防止 SSRF。
     */
    private String resolveDifyFileUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        url = url.trim();

        if (url.startsWith("/")) {
            // 相对路径：只允许 Dify 文件路径，且不能含路径穿越。
            // Dify 的 files 是带签名查询串的 URL（?timestamp=&nonce=&sign=），只校验路径部分，完整 URL 原样代理。
            if (!url.startsWith("/files/") || url.contains("..")) {
                return null;
            }
            String path = url;
            int queryIdx = url.indexOf('?');
            if (queryIdx >= 0) {
                path = url.substring(0, queryIdx);
            }
            if (!path.matches("^/files/[\\w./-]+$")) {
                return null;
            }
            return difyOrigin() + url;
        }

        // 绝对 URL：仅允许与 Dify origin 同 host
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                String originHost = new URI(difyOrigin()).getHost();
                String urlHost = new URI(url).getHost();
                if (originHost != null && originHost.equalsIgnoreCase(urlHost)) {
                    return url;
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        return null;
    }

    /**
     * Dify 文件服务 origin：从 api-base-url（如 http://localhost/v1）去掉末尾 /v1。
     */
    private String difyOrigin() {
        String base = difyBaseUrl;
        if (base != null && base.endsWith("/v1")) {
            return base.substring(0, base.length() - 3);
        }
        return base == null ? "" : base;
    }
}
