package com.biubiu.dto;

import com.biubiu.entity.FinancialRecord;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FinancialRecordResponse {
    private Long id;
    private FinancialRecord.Type type;
    private BigDecimal amount;
    private String orderNo;
    private String serviceContent;
    private String description;
    private LocalDateTime createdAt;
    private String playerNickname;
    private String orderType;
    private String remark;
    private BigDecimal totalAmount;
    private BigDecimal serviceHours;
    private BigDecimal actualHours;
    private BigDecimal pricePerHour;
    private String bossInfo;
}
