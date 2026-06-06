package com.medical.sms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.sms.entity.HealthAlert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthAlertMapper extends BaseMapper<HealthAlert> {
}
