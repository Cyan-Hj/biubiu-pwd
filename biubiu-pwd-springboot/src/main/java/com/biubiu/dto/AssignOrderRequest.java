package com.biubiu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignOrderRequest {
    private Long playerId;
    private Long playerId2;
}
