package com.medical.sms.controller;

import com.medical.sms.common.Result;
import com.medical.sms.entity.HealthAlert;
import com.medical.sms.mapper.HealthAlertMapper;
import com.medical.sms.service.PythonAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/algorithm")
@RequiredArgsConstructor
public class AlgorithmController {

    private final Random random = new Random();
    private final PythonAlgorithmService pythonAlgorithmService;
    private final RestTemplate restTemplate;
    private final HealthAlertMapper healthAlertMapper;

    @GetMapping("/service/status")
    public Result<Map<String, Object>> serviceStatus() {
        return Result.success(pythonAlgorithmService.getStatus());
    }

    @PostMapping("/service/start")
    public Result<Map<String, Object>> startService() {
        Map<String, Object> result = pythonAlgorithmService.start();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.getOrDefault("message", "启动失败"));
    }

    @PostMapping("/service/stop")
    public Result<Map<String, Object>> stopService() {
        return Result.success(pythonAlgorithmService.stop());
    }

    @PostMapping("/detect")
    public Result<Map<String, Object>> detect(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null || !body.containsKey("image")) {
            return Result.error("缺少图片参数");
        }

        if (pythonAlgorithmService.isRunning()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        pythonAlgorithmService.getBaseUrl() + "/api/detect",
                        request,
                        Map.class
                );

                Map<String, Object> pythonRes = response.getBody();
                if (pythonRes != null && Boolean.TRUE.equals(pythonRes.get("success"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> detections = (List<Map<String, Object>>) pythonRes.get("detections");
                    Map<String, Object> result = new HashMap<>();
                    result.put("detections", detections);
                    result.put("mock", false);

                    boolean hasFall = detections != null && detections.stream()
                            .anyMatch(d -> Boolean.TRUE.equals(d.get("alert")));
                    if (hasFall) {
                        try {
                            HealthAlert alert = createFallAlert(detections);
                            result.put("alertCreated", true);
                            result.put("alertId", alert.getId());
                            result.put("alertCode", alert.getAlertCode());
                        } catch (Exception ex) {
                            log.warn("创建跌倒预警失败，检测结果仍返回: {}", ex.getMessage());
                            result.put("alertCreated", false);
                            result.put("alertError", "预警写入失败: " + ex.getMessage());
                        }
                    }
                    return Result.success(result);
                }
            } catch (Exception e) {
                log.warn("Python 检测服务调用失败: {}", e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("detections", List.of());
                result.put("mock", false);
                result.put("serviceError", true);
                result.put("message", "算法服务调用失败，可能已卡死，请尝试「停止服务」后重新启动。原因: " + e.getMessage());
                return Result.success(result);
            }
        }

        List<Map<String, Object>> detections = List.of(
                Map.of("label", "老人", "x", 0.15, "y", 0.20, "w", 0.35, "h", 0.70, "confidence", 0.94, "alert", false),
                Map.of("label", "跌倒姿态", "x", 0.12, "y", 0.55, "w", 0.40, "h", 0.40, "confidence", 0.88, "alert", true),
                Map.of("label", "床铺", "x", 0.55, "y", 0.30, "w", 0.40, "h", 0.50, "confidence", 0.91, "alert", false)
        );
        Map<String, Object> result = new HashMap<>();
        result.put("detections", detections);
        result.put("mock", true);
        result.put("message", "Python 算法服务未启动，当前为演示数据。请先点击「启动算法服务」。");
        return Result.success(result);
    }

    @PostMapping("/image-classify")
    public Result<Map<String, Object>> imageClassify(@RequestBody(required = false) Map<String, Object> body) {
        List<Map<String, Object>> candidates = List.of(
                Map.of(
                        "category", "舌苔分析 - 淡白舌",
                        "healthLabel", "气血不足",
                        "healthTag", "warning",
                        "confidence", 87,
                        "description", "舌色偏淡，舌苔薄白，提示可能存在气血不足、脾虚的情况，建议进一步检查血常规，注意饮食调养。",
                        "probabilities", Arrays.asList(
                                Map.of("label", "淡白舌(气血不足)", "value", 87, "color", "#faad14"),
                                Map.of("label", "红舌(热证)", "value", 8, "color", "#f5222d"),
                                Map.of("label", "正常舌色", "value", 3, "color", "#52c41a"),
                                Map.of("label", "紫暗舌(血瘀)", "value", 2, "color", "#722ed1")
                        )
                ),
                Map.of(
                        "category", "面色分析 - 面色萎黄",
                        "healthLabel", "脾胃虚弱",
                        "healthTag", "warning",
                        "confidence", 82,
                        "description", "面色偏黄少华，提示脾胃运化功能减弱，营养吸收不佳，建议加强营养评估。",
                        "probabilities", Arrays.asList(
                                Map.of("label", "面色萎黄(脾虚)", "value", 82, "color", "#faad14"),
                                Map.of("label", "面色红润(正常)", "value", 10, "color", "#52c41a"),
                                Map.of("label", "面色苍白(贫血)", "value", 5, "color", "#1890ff"),
                                Map.of("label", "面色晦暗(肾虚)", "value", 3, "color", "#722ed1")
                        )
                ),
                Map.of(
                        "category", "舌苔分析 - 正常舌象",
                        "healthLabel", "健康",
                        "healthTag", "success",
                        "confidence", 92,
                        "description", "舌色淡红，苔薄白润，舌体大小适中，为正常舌象，提示身体状况良好。",
                        "probabilities", Arrays.asList(
                                Map.of("label", "正常舌象", "value", 92, "color", "#52c41a"),
                                Map.of("label", "淡白舌(气虚)", "value", 4, "color", "#faad14"),
                                Map.of("label", "红舌(热证)", "value", 3, "color", "#f5222d"),
                                Map.of("label", "紫暗舌(血瘀)", "value", 1, "color", "#722ed1")
                        )
                )
        );
        Map<String, Object> pick = new HashMap<>(candidates.get(random.nextInt(candidates.size())));
        pick.put("mock", true);
        return Result.success(pick);
    }

    @PostMapping("/segment")
    public Result<Map<String, Object>> segment(@RequestBody(required = false) Map<String, Object> body) {
        List<Map<String, Object>> organSegments = List.of(
                Map.of("region", "心脏区域", "color", "rgba(245,34,45,0.8)", "cx", 0.45, "cy", 0.40, "rx", 0.12, "ry", 0.15, "area", 18, "diagnosis", "心影大小正常", "type", "ellipse"),
                Map.of("region", "左肺", "color", "rgba(24,144,255,0.8)", "cx", 0.65, "cy", 0.40, "rx", 0.18, "ry", 0.30, "area", 28, "diagnosis", "左肺野清晰", "type", "ellipse"),
                Map.of("region", "右肺", "color", "rgba(82,196,26,0.8)", "cx", 0.30, "cy", 0.40, "rx", 0.18, "ry", 0.30, "area", 30, "diagnosis", "右肺野清晰", "type", "ellipse"),
                Map.of("region", "肋骨区域", "color", "rgba(250,173,20,0.8)", "cx", 0.50, "cy", 0.35, "rx", 0.35, "ry", 0.35, "area", 24, "diagnosis", "骨骼结构完整", "type", "ellipse")
        );

        List<Map<String, Object>> lesionSegments = List.of(
                Map.of("region", "疑似结节", "color", "rgba(245,34,45,0.8)", "cx", 0.35, "cy", 0.35, "r", 0.04, "area", 3, "diagnosis", "右肺上叶小结节，建议复查CT", "type", "circle"),
                Map.of("region", "钙化灶", "color", "rgba(250,173,20,0.8)", "cx", 0.60, "cy", 0.50, "r", 0.03, "area", 2, "diagnosis", "左肺下叶钙化灶，考虑陈旧性病变", "type", "circle"),
                Map.of("region", "正常组织", "color", "rgba(82,196,26,0.8)", "cx", 0.50, "cy", 0.40, "rx", 0.30, "ry", 0.35, "area", 95, "diagnosis", "大部分肺组织正常", "type", "ellipse")
        );
        Map<String, Object> segmentResult = new HashMap<>();
        segmentResult.put("organSegments", organSegments);
        segmentResult.put("lesionSegments", lesionSegments);
        segmentResult.put("mock", true);
        return Result.success(segmentResult);
    }

    @PostMapping("/tts")
    public Result<Map<String, Object>> tts(@RequestBody Map<String, Object> body) {
        String text = body != null ? (String) body.getOrDefault("text", "") : "";
        if (text.isBlank()) {
            return Result.error("播报内容不能为空");
        }
        if (!pythonAlgorithmService.isRunning()) {
            return Result.error("Python 算法服务未启动，请先点击「启动服务」");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("text", text);
            reqBody.put("rate", parseDouble(body.get("rate"), 1.0));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(reqBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonAlgorithmService.getBaseUrl() + "/api/tts",
                    request,
                    Map.class
            );

            Map<String, Object> pythonRes = response.getBody();
            if (pythonRes == null || !Boolean.TRUE.equals(pythonRes.get("success"))) {
                String err = pythonRes != null ? (String) pythonRes.getOrDefault("error", "合成失败") : "合成失败";
                return Result.error(err);
            }

            String audioBase64 = (String) pythonRes.get("audioBase64");
            if (audioBase64 == null || audioBase64.isBlank()) {
                return Result.error("语音合成失败：未返回音频数据");
            }

            Map<String, Object> ttsResult = new HashMap<>();
            ttsResult.put("text", text);
            ttsResult.put("audioBase64", audioBase64);
            return Result.success(ttsResult);
        } catch (Exception e) {
            log.warn("Python TTS 服务调用失败: {}", e.getMessage());
            return Result.error("语音合成失败: " + e.getMessage());
        }
    }

    private HealthAlert createFallAlert(List<Map<String, Object>> detections) {
        double maxConfidence = detections.stream()
                .filter(d -> Boolean.TRUE.equals(d.get("alert")))
                .mapToDouble(d -> ((Number) d.getOrDefault("confidence", 0)).doubleValue())
                .max()
                .orElse(0.0);

        HealthAlert alert = new HealthAlert();
        alert.setAlertCode("AL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%03d", random.nextInt(1000)));
        alert.setElderName("待确认老人");
        alert.setRoom("监控区域");
        alert.setAlertType("跌倒检测");
        alert.setLevel("high");
        alert.setDescription(String.format(
                "AI 检测到跌倒行为，最高置信度 %.0f%%，请立即确认老人安全状况",
                maxConfidence * 100
        ));
        alert.setDeviceId("AI-CAM");
        alert.setStatus("pending");
        alert.setCreateTime(LocalDateTime.now());
        alert.setUpdateTime(LocalDateTime.now());
        healthAlertMapper.insert(alert);
        log.info("已创建跌倒预警: {}", alert.getAlertCode());
        return alert;
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
