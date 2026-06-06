package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.sms.common.PageResult;
import com.medical.sms.common.Result;
import com.medical.sms.entity.AssessmentReport;
import com.medical.sms.mapper.AssessmentReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentReportMapper reportMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Long elderId,
                          @RequestParam(required = false) String elderName,
                          @RequestParam(required = false) String assessType) {
        LambdaQueryWrapper<AssessmentReport> wrapper = new LambdaQueryWrapper<AssessmentReport>()
                .eq(elderId != null, AssessmentReport::getElderId, elderId)
                .like(StringUtils.hasText(elderName), AssessmentReport::getElderName, elderName)
                .eq(StringUtils.hasText(assessType), AssessmentReport::getAssessType, assessType)
                .orderByDesc(AssessmentReport::getCreateTime);
        IPage<AssessmentReport> result = reportMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(reportMapper.selectById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody AssessmentReport report) {
        reportMapper.insert(report);
        return Result.success(report);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody AssessmentReport report) {
        report.setId(id);
        reportMapper.updateById(report);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        reportMapper.deleteById(id);
        return Result.success();
    }
}
