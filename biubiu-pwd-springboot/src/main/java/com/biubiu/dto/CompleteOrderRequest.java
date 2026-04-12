package com.biubiu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CompleteOrderRequest {
    private BigDecimal actualHours;

    // 新的多张截图列表
    private List<String> screenshotUrls;

    // 兼容旧版本
    private String startScreenshotUrl;
    private String endScreenshotUrl;
}
