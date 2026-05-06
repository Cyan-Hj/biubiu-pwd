package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerServiceResponse {
    private Long id;
    private String phone;
    private String nickname;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
