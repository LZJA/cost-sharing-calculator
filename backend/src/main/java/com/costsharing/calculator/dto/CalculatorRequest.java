package com.costsharing.calculator.dto;

import com.costsharing.calculator.formula.CalculatorConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CalculatorRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    private String description;

    private String avatar;

    private String background;

    private Boolean enableBackground;

    @NotNull(message = "规则配置不能为空")
    private CalculatorConfig config;
}
