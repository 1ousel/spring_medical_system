package com.medical.sms.config;

import com.medical.sms.entity.SysUser;
import com.medical.sms.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
    }

    private void initUsers() {
        List<Object[]> defaults = List.of(
            new Object[]{"admin",      "系统管理员", "ADMIN",     "信息部",    "13000000001", "admin@sms.com"},
            new Object[]{"doctor1",    "王建国",    "DOCTOR",    "内科",      "13900000001", "wang@sms.com"},
            new Object[]{"doctor2",    "李晓梅",    "DOCTOR",    "康复科",    "13900000002", "li@sms.com"},
            new Object[]{"doctor3",    "张宇",      "DOCTOR",    "中医科",    "13900000003", "zhang@sms.com"},
            new Object[]{"doctor4",    "陈明辉",    "DOCTOR",    "全科",      "13900000004", "chen@sms.com"},
            new Object[]{"doctor5",    "刘海燕",    "DOCTOR",    "外科",      "13900000005", "liuhy@sms.com"},
            new Object[]{"nurse1",     "张小红",    "NURSE",     "A区护理",   "13700000001", "nzhang@sms.com"},
            new Object[]{"nurse2",     "李婷婷",    "NURSE",     "B区护理",   "13700000002", "ltt@sms.com"},
            new Object[]{"nurse3",     "王丽娜",    "NURSE",     "C区护理",   "13700000003", "wln@sms.com"},
            new Object[]{"caregiver1", "赵大勇",    "CAREGIVER", "A区护工",   "13600000001", "zdy@sms.com"},
            new Object[]{"caregiver2", "钱小芳",    "CAREGIVER", "B区护工",   "13600000002", "qxf@sms.com"}
        );

        for (Object[] u : defaults) {
            String username = (String) u[0];
            // 使用物理查询，绕过@TableLogic，避免软删除后重复插入
            Long count = userMapper.countByUsernamePhysical(username);
            if (count > 0) {
                continue;
            }
            try {
                SysUser user = new SysUser();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setRealName((String) u[1]);
                user.setRole((String) u[2]);
                user.setDept((String) u[3]);
                user.setPhone((String) u[4]);
                user.setEmail((String) u[5]);
                user.setEnabled(true);
                user.setDeleted(0);
                userMapper.insert(user);
                log.info("初始化用户: {}", username);
            } catch (Exception e) {
                log.debug("用户 {} 已存在，跳过", username);
            }
        }
    }
}
