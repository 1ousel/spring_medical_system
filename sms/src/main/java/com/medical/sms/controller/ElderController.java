package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.sms.common.PageResult;
import com.medical.sms.common.Result;
import com.medical.sms.entity.Elder;
import com.medical.sms.mapper.ElderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/elder")
@RequiredArgsConstructor
public class ElderController {

    private final ElderMapper elderMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String healthStatus,
                          @RequestParam(required = false) String room) {
        LambdaQueryWrapper<Elder> wrapper = new LambdaQueryWrapper<Elder>()
                .like(StringUtils.hasText(name), Elder::getName, name)
                .eq(StringUtils.hasText(healthStatus), Elder::getHealthStatus, healthStatus)
                .like(StringUtils.hasText(room), Elder::getRoom, room)
                .orderByDesc(Elder::getCreateTime);
        IPage<Elder> result = elderMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(elderMapper.selectById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody Elder elder) {
        elderMapper.insert(elder);
        return Result.success(elder);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Elder elder) {
        elder.setId(id);
        elderMapper.updateById(elder);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        elderMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/count")
    public Result<?> count() {
        return Result.success(elderMapper.selectCount(null));
    }
}
