package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.sms.common.PageResult;
import com.medical.sms.common.Result;
import com.medical.sms.entity.Device;
import com.medical.sms.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceMapper deviceMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(StringUtils.hasText(type), Device::getType, type)
                .eq(StringUtils.hasText(status), Device::getStatus, status)
                .orderByDesc(Device::getCreateTime);
        IPage<Device> result = deviceMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(deviceMapper.selectById(id));
    }

    @PostMapping
    public Result<?> save(@RequestBody Device device) {
        deviceMapper.insert(device);
        return Result.success(device);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Device device) {
        device.setId(id);
        deviceMapper.updateById(device);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        deviceMapper.deleteById(id);
        return Result.success();
    }
}
