package com.biubiu.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerServiceRequest {
    private Boolean enabled;

    @Size(max = 20, message = "昵称最多20个字符")
    private String nickname;
}
