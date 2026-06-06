package com.medical.sms.service;

import com.medical.sms.config.PythonAlgorithmProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonAlgorithmService {

    private final PythonAlgorithmProperties properties;
    private final RestTemplate restTemplate;

    private Process process;
    private volatile boolean startedByJava;
    private volatile String lastError;

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean processAlive = process != null && process.isAlive();
        boolean httpHealthy = checkHealth();
        boolean running = httpHealthy || processAlive;

        status.put("running", running);
        status.put("httpHealthy", httpHealthy);
        status.put("processAlive", processAlive);
        status.put("startedByJava", startedByJava);
        status.put("baseUrl", properties.getBaseUrl());
        status.put("pid", processAlive ? process.pid() : null);
        if (lastError != null) {
            status.put("lastError", lastError);
        }
        return status;
    }

    public synchronized Map<String, Object> start() {
        Map<String, Object> result = new HashMap<>();

        if (checkHealth()) {
            result.put("success", true);
            result.put("message", startedByJava ? "算法服务已在运行（由 Java 启动）" : "算法服务已在运行（外部进程）");
            result.putAll(getStatus());
            return result;
        }

        if (process != null && process.isAlive()) {
            result.put("success", false);
            result.put("message", "Python 进程正在启动，请稍候...");
            result.putAll(getStatus());
            return result;
        }

        File workDir = resolveWorkDir();
        File scriptFile = resolveScript(workDir);
        if (!scriptFile.isFile()) {
            lastError = "算法脚本不存在: " + scriptFile.getAbsolutePath();
            result.put("success", false);
            result.put("message", lastError);
            result.putAll(getStatus());
            return result;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    properties.getCommand(),
                    scriptFile.getAbsolutePath()
            );
            builder.directory(workDir);
            builder.environment().put("ALGORITHM_PORT", extractPort(properties.getBaseUrl()));
            builder.environment().put("PYTHONUNBUFFERED", "1");
            builder.environment().put("YOLO_VERBOSE", "False");

            File logFile = new File(workDir, "python-algorithm.log");
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            builder.redirectError(ProcessBuilder.Redirect.appendTo(logFile));

            process = builder.start();
            startedByJava = true;
            lastError = null;

            log.info("Python 算法服务已启动, pid={}, workDir={}, log={}",
                    process.pid(), workDir.getAbsolutePath(), logFile.getAbsolutePath());

            boolean ready = waitForHealth(properties.getStartupTimeoutSeconds());
            result.put("success", ready);
            result.put("message", ready ? "算法服务启动成功" : "算法服务进程已启动，但健康检查超时，请稍后重试");
            result.putAll(getStatus());
            if (!ready) {
                lastError = "健康检查超时（" + properties.getStartupTimeoutSeconds() + "s）";
                result.put("lastError", lastError);
            }
            return result;
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("启动 Python 算法服务失败", e);
            result.put("success", false);
            result.put("message", "启动失败: " + e.getMessage());
            result.putAll(getStatus());
            return result;
        }
    }

    public synchronized Map<String, Object> stop() {
        Map<String, Object> result = new HashMap<>();
        boolean wasStartedByJava = startedByJava;
        boolean hadProcess = process != null && process.isAlive();

        if (checkHealth()) {
            requestGracefulShutdown();
        }

        if (process != null) {
            waitForProcessExit(8);
            if (process.isAlive()) {
                log.warn("Python 进程未响应优雅关闭，强制终止 pid={}", process.pid());
                process.destroyForcibly();
                waitForProcessExit(3);
            }
            process = null;
        }

        startedByJava = false;
        boolean stillRunning = checkHealth();

        result.put("success", !stillRunning);
        if (stillRunning) {
            result.put("message", "Python 服务仍在运行（可能由外部手动启动），Java 无法停止外部进程");
        } else if (wasStartedByJava || hadProcess) {
            result.put("message", "算法服务已停止");
        } else {
            result.put("message", "算法服务当前未运行");
        }
        result.putAll(getStatus());
        return result;
    }

    public boolean isRunning() {
        return checkHealth() || (process != null && process.isAlive());
    }

    public String getBaseUrl() {
        return properties.getBaseUrl();
    }

    @PreDestroy
    public void onDestroy() {
        if (properties.isStopOnShutdown() && startedByJava) {
            log.info("Spring 关闭，停止由 Java 启动的 Python 算法服务");
            stop();
        }
    }

    private void requestGracefulShutdown() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(
                    properties.getBaseUrl() + "/api/shutdown",
                    new HttpEntity<>(Map.of(), headers),
                    Map.class
            );
            log.info("已向 Python 服务发送 shutdown 请求");
        } catch (Exception e) {
            log.warn("Python 优雅关闭请求失败: {}", e.getMessage());
        }
    }

    private boolean checkHealth() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    properties.getBaseUrl() + "/api/health",
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("success"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForHealth(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (checkHealth()) {
                return true;
            }
            if (process != null && !process.isAlive()) {
                lastError = "Python 进程异常退出，退出码: " + process.exitValue();
                return false;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void waitForProcessExit(int timeoutSeconds) {
        if (process == null) {
            return;
        }
        try {
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private File resolveWorkDir() {
        File dir = new File(properties.getWorkDir());
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), properties.getWorkDir());
        }
        return dir.getAbsoluteFile();
    }

    private File resolveScript(File workDir) {
        File script = new File(properties.getScript());
        if (!script.isAbsolute()) {
            script = new File(workDir, properties.getScript());
        }
        return script.getAbsoluteFile();
    }

    private String extractPort(String baseUrl) {
        try {
            java.net.URI uri = java.net.URI.create(baseUrl);
            if (uri.getPort() > 0) {
                return String.valueOf(uri.getPort());
            }
            return "5000";
        } catch (Exception e) {
            return "5000";
        }
    }
}
