package com.costsharing.calculator.formula;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareConfig {

    private String member;

    /** 固定份额。basis 为 days/totalDays 时，实际份额还要乘以居住天数占比。 */
    private BigDecimal weight;

    /** 为空表示固定份额；days/totalDays 表示按居住天数占比。 */
    private String basis;

    public ShareConfig(String member, BigDecimal weight) {
        this.member = member;
        this.weight = weight;
    }
}
