package com.biubiu.dto;

import java.math.BigDecimal;

public interface PlayerIncomeSummary {
    Long getPlayerId();
    String getNickname();
    BigDecimal getTotalIncome();
}
