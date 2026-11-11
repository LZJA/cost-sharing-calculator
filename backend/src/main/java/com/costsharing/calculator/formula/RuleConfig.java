package com.costsharing.calculator.formula;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RuleConfig {

    private String name;

    private List<StepConfig> steps = new ArrayList<>();
}
