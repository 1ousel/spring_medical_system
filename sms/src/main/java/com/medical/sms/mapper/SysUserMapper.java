package com.medical.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.sms.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    Long countByUsernamePhysical(@Param("username") String username);
}
