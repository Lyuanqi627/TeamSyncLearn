package com.teamsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsync.entity.AiResult;
import com.teamsync.mapper.AiResultMapper;
import com.teamsync.vo.AiResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AiEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AiEvaluationService.class);

    private final AiResultMapper aiResultMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${ai.model:qwen-plus}")
    private String model;

    public AiEvaluationService(AiResultMapper aiResultMapper) {
        this.aiResultMapper = aiResultMapper;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Async("aiTaskExecutor")
    public void evaluateAndSummarize(Long achievementId, Long userId, String goal, String content) {
        try {
            AiResultVO result;

            if (apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("your-")) {
                result = callAiApi(goal, content);
            } else {
                // Fallback when no API key is configured
                result = new AiResultVO();
                result.setDiligenceScore(75);
                result.setAiComment("AI评估服务未配置。已自动标记为默认评分。请配置 ai.api-key 以启用智能评估功能。");
                result.setAiSummary("学习成果已记录，待AI服务配置完成后可进行智能分析总结。");
            }

            // Validate score range
            if (result.getDiligenceScore() == null || result.getDiligenceScore() < 0 || result.getDiligenceScore() > 100) {
                result.setDiligenceScore(70);
            }

            AiResult aiResult = new AiResult();
            aiResult.setAchievementId(achievementId);
            aiResult.setUserId(userId);
            aiResult.setDiligenceScore(result.getDiligenceScore());
            aiResult.setAiComment(result.getAiComment());
            aiResult.setAiSummary(result.getAiSummary());
            aiResult.setCreatedAt(LocalDateTime.now());
            aiResultMapper.insert(aiResult);

            log.info("AI evaluation completed for achievement {}", achievementId);
        } catch (Exception e) {
            log.error("AI evaluation failed for achievement {}: {}", achievementId, e.getMessage());
        }
    }

    private AiResultVO callAiApi(String goal, String content) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", "你是一位严厉但鼓励人的学习导师。请评估学习成果并严格按照JSON格式返回。"),
                    Map.of("role", "user", "content", String.format(
                        "学习目标：%s\n成果内容：%s\n\n请严格按照以下JSON格式返回（不要markdown标记）：\n{\"diligenceScore\": 0-100的整数, \"aiComment\": \"点评内容\", \"aiSummary\": \"核心知识点总结\"}",
                        goal, content))
                ),
                "temperature", 0.3
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions", request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String jsonContent = (String) message.get("content");

                    // Clean markdown code block if present
                    jsonContent = jsonContent.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

                    return objectMapper.readValue(jsonContent, AiResultVO.class);
                }
            }

            throw new RuntimeException("Unexpected API response: " + responseBody);
        } catch (Exception e) {
            log.warn("AI API call failed, using fallback: {}", e.getMessage());
            AiResultVO fallback = new AiResultVO();
            fallback.setDiligenceScore(70);
            fallback.setAiComment("AI评估暂时不可用（" + e.getMessage() + "），已使用默认评分。");
            fallback.setAiSummary("AI总结暂时不可用，请稍后重试。");
            return fallback;
        }
    }
}
