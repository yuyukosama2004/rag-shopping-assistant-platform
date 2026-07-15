package com.biyesheji.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiSettingSaveDTO {
    @NotNull @Min(0) @Max(1)
    private Integer enabled;

    @NotBlank @Size(max = 100)
    private String model;

    @NotNull @DecimalMin("0.00") @DecimalMax("2.00")
    private BigDecimal temperature;

    @NotNull @Min(100) @Max(4000)
    private Integer maxOutputTokens;

    @NotNull @Min(1) @Max(1000)
    private Integer perUserDailyLimit;

    @Size(max = 500)
    private String disclaimer;

    @Size(max = 4000)
    private String systemPrompt;
}
