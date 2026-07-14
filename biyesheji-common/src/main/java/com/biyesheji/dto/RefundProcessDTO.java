package com.biyesheji.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundProcessDTO {
    @Size(max = 500) private String merchantNote;
}
