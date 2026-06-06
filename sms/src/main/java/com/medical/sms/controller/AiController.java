package com.medical.sms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat/stream")
    public void chatStream(@RequestBody ChatRequest req, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();
        HttpURLConnection conn = null;

        try {
            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", "deepseek-chat");
            body.put("stream", true);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "你是智慧医养大数据公共服务平台的AI助手。你可以帮助管理人员进行系统操作答疑、数据统计分析建议，也可以为医生提供老人健康管理辅助方案。请用中文回答，回答尽量专业且简洁。"));

            if (req.getHistory() != null) {
                for (ChatMessage msg : req.getHistory()) {
                    messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                }
            }
            messages.add(Map.of("role", "user", "content", req.getMessage()));
            body.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("DeepSeek请求: model=deepseek-chat, messageCount={}", messages.size());

            // 发起流式请求
            conn = (HttpURLConnection) new URL(baseUrl + "/v1/chat/completions").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                InputStream errStream = conn.getErrorStream();
                String errBody = errStream != null ? new String(errStream.readAllBytes(), StandardCharsets.UTF_8) : "unknown";
                log.error("DeepSeek API返回错误 status={}, body={}", status, errBody);
                sendSSE(writer, "{\"error\":\"AI服务返回错误: " + status + "\"}");
                sendSSE(writer, "[DONE]");
                return;
            }

            // 逐行读取SSE并转发
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        sendSSE(writer, "[DONE]");
                        break;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        String content = node.path("choices").path(0).path("delta").path("content").asText("");
                        if (!content.isEmpty()) {
                            String chunk = objectMapper.writeValueAsString(Map.of("content", content));
                            sendSSE(writer, chunk);
                        }
                    } catch (Exception e) {
                        log.debug("跳过无法解析的chunk: {}", data);
                    }
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek 流式调用失败", e);
            sendSSE(writer, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            sendSSE(writer, "[DONE]");
        } finally {
            if (conn != null) conn.disconnect();
            writer.flush();
        }
    }

    private void sendSSE(PrintWriter writer, String data) {
        writer.write("data: " + data + "\n\n");
        writer.flush();
    }

    @Data
    public static class ChatRequest {
        private String message;
        private List<ChatMessage> history;
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
