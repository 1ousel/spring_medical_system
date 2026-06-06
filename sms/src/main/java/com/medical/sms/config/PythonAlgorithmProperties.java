package com.medical.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "algorithm.python")
public class PythonAlgorithmProperties {

    /** Python 可执行文件，如 python 或 python3 */
    private String command = "python";

    /** 算法脚本绝对路径或相对 workDir 的路径 */
    private String script = "app_fall_detection.py";

    /** 启动 Python 时的工作目录（项目根目录） */
    private String workDir = "..";

    /** Python 服务 base URL */
    private String baseUrl = "http://localhost:5000";

    /** 启动后等待健康检查的超时时间（秒） */
    private int startupTimeoutSeconds = 180;

    /** Spring Boot 关闭时是否自动停止由 Java 启动的 Python 进程 */
    private boolean stopOnShutdown = true;
}
