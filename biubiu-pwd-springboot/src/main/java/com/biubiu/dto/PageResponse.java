package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;
}
