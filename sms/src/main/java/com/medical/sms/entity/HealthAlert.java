package com.medical.sms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("health_alert")
public class HealthAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alertCode;
    private Long elderId;
    private String elderName;
    private String room;
    private String alertType;
    private String level;
    private String description;
    private String deviceId;
    private String status;
    private String processRemark;
    private Long processUserId;
    private LocalDateTime processTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
