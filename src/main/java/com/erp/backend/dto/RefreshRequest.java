package com.erp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {
    @NotBlank(message = "refreshToken darf nicht leer sein")
    private String refreshToken;
}
