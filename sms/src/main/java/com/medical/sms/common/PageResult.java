package com.medical.sms.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private Long total;
    private List<T> records;

    public static <T> PageResult<T> of(Long total, List<T> records) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.records = records;
        return p;
    }
}
