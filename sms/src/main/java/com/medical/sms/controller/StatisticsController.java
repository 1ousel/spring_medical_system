package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.sms.common.Result;
import com.medical.sms.entity.*;
import com.medical.sms.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final ElderMapper elderMapper;
    private final SysUserMapper userMapper;
    private final HealthAlertMapper alertMapper;
    private final AssessmentReportMapper reportMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/overview")
    public Result<?> overview() {
        Map<String, Object> data = new HashMap<>();

        // 基础统计
        data.put("elderCount", elderMapper.selectCount(null));
        data.put("doctorCount", userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "DOCTOR")));
        data.put("nurseCount", userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "NURSE")));
        data.put("deviceCount", deviceMapper.selectCount(null));
        data.put("alertCount", alertMapper.selectCount(null));
        data.put("assessCount", reportMapper.selectCount(null));

        // 老人健康状态分布
        List<Elder> elders = elderMapper.selectList(null);
        Map<String, Long> healthDist = elders.stream()
            .collect(Collectors.groupingBy(Elder::getHealthStatus, Collectors.counting()));
        data.put("healthDistribution", healthDist);

        // 老人性别分布
        Map<String, Long> genderDist = elders.stream()
            .collect(Collectors.groupingBy(Elder::getGender, Collectors.counting()));
        data.put("genderDistribution", genderDist);

        // 老人年龄段分布
        Map<String, Long> ageDist = new LinkedHashMap<>();
        ageDist.put("60-69岁", elders.stream().filter(e -> e.getAge() >= 60 && e.getAge() < 70).count());
        ageDist.put("70-79岁", elders.stream().filter(e -> e.getAge() >= 70 && e.getAge() < 80).count());
        ageDist.put("80-89岁", elders.stream().filter(e -> e.getAge() >= 80 && e.getAge() < 90).count());
        ageDist.put("90岁以上", elders.stream().filter(e -> e.getAge() >= 90).count());
        data.put("ageDistribution", ageDist);

        // 预警等级分布
        List<HealthAlert> alerts = alertMapper.selectList(null);
        Map<String, Long> alertLevelDist = alerts.stream()
            .collect(Collectors.groupingBy(HealthAlert::getLevel, Collectors.counting()));
        data.put("alertLevelDistribution", alertLevelDist);

        // 预警状态分布
        Map<String, Long> alertStatusDist = alerts.stream()
            .collect(Collectors.groupingBy(HealthAlert::getStatus, Collectors.counting()));
        data.put("alertStatusDistribution", alertStatusDist);

        // 预警类型分布
        Map<String, Long> alertTypeDist = alerts.stream()
            .collect(Collectors.groupingBy(HealthAlert::getAlertType, Collectors.counting()));
        data.put("alertTypeDistribution", alertTypeDist);

        // 评估类型分布
        List<AssessmentReport> reports = reportMapper.selectList(null);
        Map<String, Long> assessTypeDist = reports.stream()
            .collect(Collectors.groupingBy(AssessmentReport::getAssessType, Collectors.counting()));
        data.put("assessTypeDistribution", assessTypeDist);

        // 评估等级分布
        Map<String, Long> assessLevelDist = reports.stream()
            .filter(r -> r.getLevel() != null)
            .collect(Collectors.groupingBy(AssessmentReport::getLevel, Collectors.counting()));
        data.put("assessLevelDistribution", assessLevelDist);

        // 设备类型分布
        List<Device> devices = deviceMapper.selectList(null);
        Map<String, Long> deviceTypeDist = devices.stream()
            .collect(Collectors.groupingBy(Device::getType, Collectors.counting()));
        data.put("deviceTypeDistribution", deviceTypeDist);

        // 设备状态分布
        Map<String, Long> deviceStatusDist = devices.stream()
            .collect(Collectors.groupingBy(Device::getStatus, Collectors.counting()));
        data.put("deviceStatusDistribution", deviceStatusDist);

        // 各医生负责老人数
        Map<String, Long> doctorElderDist = elders.stream()
            .filter(e -> e.getDoctor() != null)
            .collect(Collectors.groupingBy(Elder::getDoctor, Collectors.counting()));
        data.put("doctorElderDistribution", doctorElderDist);

        return Result.success(data);
    }
}
