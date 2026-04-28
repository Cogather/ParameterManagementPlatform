package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

/**
 * 参数聚合根（spec-03）：基线锁定等不变量。
 */
public class Parameter {

    private final String dataStatus;

    /**
     * 创建参数聚合根（仅承载不变量与状态判断）。
     *
     * @param dataStatus 数据状态
     */
    public Parameter(String dataStatus) {
        this.dataStatus = dataStatus;
    }

    /**
     * 断言当前参数可写（若基线锁定则抛出领域异常）。
     */
    public void assertWritable() {
        if (ParameterBaselinePolicy.isBaselineLocked(dataStatus)) {
            throw new DomainRuleException("PARAM_BASELINE_LOCKED: 已基线禁止写入");
        }
    }
}
