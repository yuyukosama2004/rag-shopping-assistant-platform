package com.biyesheji.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiEvaluationRequest {

    @NotBlank(message = "query must not be blank")
    @Size(max = 500, message = "query must not exceed 500 characters")
    private String query;

    @AssertFalse(message = "generate_answer is not supported by the read-only evaluation endpoint")
    @JsonProperty("generate_answer")
    private boolean generateAnswer;
}
