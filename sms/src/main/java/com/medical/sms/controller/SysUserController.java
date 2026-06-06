package com.medical.sms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.sms.common.PageResult;
import com.medical.sms.common.Result;
import com.medical.sms.entity.SysUser;
import com.medical.sms.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getRealName, keyword)
                        .or()
                        .like(SysUser::getUsername, keyword))
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping
    public Result<?> save(@RequestBody SysUser user) {
        String rawPassword = StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        userMapper.insert(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        user.setPassword(null);
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/toggle")
    public Result<?> toggleStatus(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setEnabled(!user.getEnabled());
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/change-password")
    public Result<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        if (!passwordEncoder.matches(body.get("oldPwd"), user.getPassword())) {
            return Result.error("当前密码错误");
        }
        user.setPassword(passwordEncoder.encode(body.get("newPwd")));
        userMapper.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/doctors")
    public Result<?> getDoctors() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "DOCTOR")
                .eq(SysUser::getEnabled, true);
        var list = userMapper.selectList(wrapper);
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }
}
