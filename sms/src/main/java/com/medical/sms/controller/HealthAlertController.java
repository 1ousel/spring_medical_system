package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.sms.common.PageResult;
import com.medical.sms.common.Result;
import com.medical.sms.entity.HealthAlert;
import com.medical.sms.mapper.HealthAlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/health-alert")
@RequiredArgsConstructor
public class HealthAlertController {

    private final HealthAlertMapper alertMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Long elderId,
                          @RequestParam(required = false) String level,
                          @RequestParam(required = false) String status) {
        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<HealthAlert>()
                .eq(elderId != null, HealthAlert::getElderId, elderId)
                .eq(StringUtils.hasText(level), HealthAlert::getLevel, level)
                .eq(StringUtils.hasText(status), HealthAlert::getStatus, status)
                .orderByDesc(HealthAlert::getCreateTime);
        IPage<HealthAlert> result = alertMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(alertMapper.selectById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody HealthAlert alert) {
        alertMapper.insert(alert);
        return Result.success(alert);
    }

    @PutMapping("/{id}/process")
    public Result<?> process(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        HealthAlert alert = new HealthAlert();
        alert.setId(id);
        alert.setStatus("handled");
        alert.setProcessRemark(body.get("remark"));
        alert.setProcessTime(LocalDateTime.now());
        alertMapper.updateById(alert);
        return Result.success();
    }

    @GetMapping("/today-count")
    public Result<?> todayCount() {
        long count = alertMapper.selectCount(
            new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getStatus, "pending")
        );
        return Result.success(count);
    }
}
