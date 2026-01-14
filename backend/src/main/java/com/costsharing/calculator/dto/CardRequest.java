package com.costsharing.calculator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卡片请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {

    @NotBlank(message = "卡片类型不能为空")
    private String type;

    @NotBlank(message = "卡片名称不能为空")
    private String name;

    private String description;

    private String avatar;

    private String background;

    private Boolean enableBackground;
}
