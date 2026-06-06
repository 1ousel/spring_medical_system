package com.medical.sms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("assessment_report")
public class AssessmentReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private String elderName;
    private String assessType;
    private LocalDate assessDate;
    private Long evaluatorId;
    private String evaluator;
    private Integer score;
    private String level;
    private String conclusion;
    private String details;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
