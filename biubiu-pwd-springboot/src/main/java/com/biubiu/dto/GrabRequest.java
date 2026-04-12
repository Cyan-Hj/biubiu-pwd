package com.biubiu.dto;

import lombok.Data;

@Data
public class GrabRequest {
    private String grabType;
    private String action;
    private Long targetPlayerId;
    private Long partnerId;
    private String partnerPhone;
}
