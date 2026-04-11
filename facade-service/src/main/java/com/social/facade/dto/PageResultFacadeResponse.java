package com.social.facade.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Facade层分页响应DTO - 包装类
 */
@Data
@NoArgsConstructor
public class PageResultFacadeResponse<T> implements Serializable {
    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;

    public static <T> PageResultFacadeResponse<T> of(List<T> records, Long total, Integer page, Integer size) {
        PageResultFacadeResponse<T> result = new PageResultFacadeResponse<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }
}
