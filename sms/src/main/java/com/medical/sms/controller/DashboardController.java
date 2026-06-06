package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.sms.common.Result;
import com.medical.sms.entity.AssessmentReport;
import com.medical.sms.entity.Elder;
import com.medical.sms.entity.HealthAlert;
import com.medical.sms.entity.SysUser;
import com.medical.sms.mapper.AssessmentReportMapper;
import com.medical.sms.mapper.ElderMapper;
import com.medical.sms.mapper.HealthAlertMapper;
import com.medical.sms.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ElderMapper elderMapper;
    private final SysUserMapper userMapper;
    private final HealthAlertMapper alertMapper;
    private final AssessmentReportMapper reportMapper;

    @GetMapping("/stats")
    public Result<?> stats() {
        Map<String, Object> data = new HashMap<>();

        data.put("elderCount", elderMapper.selectCount(null));

        data.put("doctorCount", userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "DOCTOR")
        ));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        data.put("todayAlertCount", alertMapper.selectCount(
            new LambdaQueryWrapper<HealthAlert>().ge(HealthAlert::getCreateTime, startOfToday)
        ));

        data.put("monthAssessCount", reportMapper.selectCount(
            new LambdaQueryWrapper<AssessmentReport>().ge(AssessmentReport::getCreateTime, startOfMonth)
        ));

        data.put("charts", buildChartData());

        return Result.success(data);
    }

    private Map<String, Object> buildChartData() {
        Map<String, Object> charts = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime trendStart = today.minusDays(6).atStartOfDay();
        List<HealthAlert> trendAlerts = alertMapper.selectList(
            new LambdaQueryWrapper<HealthAlert>().ge(HealthAlert::getCreateTime, trendStart)
        );

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dates.add(today.minusDays(i).format(dayFmt));
        }

        List<String> trendTypes = List.of("跌倒检测", "心率异常", "血压异常");
        List<Map<String, Object>> trendSeries = new ArrayList<>();
        for (String type : trendTypes) {
            int[] counts = new int[7];
            for (HealthAlert alert : trendAlerts) {
                if (!type.equals(alert.getAlertType()) || alert.getCreateTime() == null) {
                    continue;
                }
                long dayIndex = java.time.temporal.ChronoUnit.DAYS.between(
                    today.minusDays(6), alert.getCreateTime().toLocalDate());
                if (dayIndex >= 0 && dayIndex < 7) {
                    counts[(int) dayIndex]++;
                }
            }
            List<Integer> data = Arrays.stream(counts).boxed().collect(Collectors.toList());
            trendSeries.add(Map.of("name", type, "data", data));
        }
        charts.put("alertTrend", Map.of("dates", dates, "series", trendSeries));

        List<HealthAlert> allAlerts = alertMapper.selectList(null);
        Map<String, Long> typeDist = allAlerts.stream()
            .collect(Collectors.groupingBy(HealthAlert::getAlertType, Collectors.counting()));
        List<Map<String, Object>> pieData = typeDist.entrySet().stream()
            .map(e -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", e.getKey());
                item.put("value", e.getValue());
                return item;
            })
            .collect(Collectors.toList());
        charts.put("alertTypePie", pieData);

        List<Elder> elders = elderMapper.selectList(null);
        Map<String, Long> ageDist = new LinkedHashMap<>();
        ageDist.put("60-69岁", elders.stream().filter(e -> e.getAge() >= 60 && e.getAge() < 70).count());
        ageDist.put("70-79岁", elders.stream().filter(e -> e.getAge() >= 70 && e.getAge() < 80).count());
        ageDist.put("80-89岁", elders.stream().filter(e -> e.getAge() >= 80 && e.getAge() < 90).count());
        ageDist.put("90岁以上", elders.stream().filter(e -> e.getAge() >= 90).count());
        charts.put("ageDistribution", ageDist);

        return charts;
    }
}
