package com.biyesheji.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeSaveDTO {
    @NotBlank
    @Pattern(regexp = "FAQ|SHIPPING|AFTER_SALES|STORE")
    private String category;

    @NotBlank @Size(max = 100)
    private String title;

    @NotBlank @Size(max = 2000)
    private String content;

    @NotNull @Min(0) @Max(1)
    private Integer status;

    @NotNull @Min(0) @Max(10000)
    private Integer sortOrder;
}
