package com.biubiu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompleteOrderRequest {
    private BigDecimal actualHours;

    private String startScreenshotUrl;
    private String endScreenshotUrl;
}
