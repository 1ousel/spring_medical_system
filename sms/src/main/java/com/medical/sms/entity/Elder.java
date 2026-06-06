package com.medical.sms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("elder")
public class Elder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String gender;
    private Integer age;
    private String idCard;
    private String room;
    private Long doctorId;
    private String doctor;
    private String phone;
    private String emergencyContact;
    private String emergencyPhone;
    private LocalDate checkInDate;
    private String healthStatus;
    private String medicalHistory;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
