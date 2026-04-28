package com.coretool.param.domain.parameter;

/** 已基线后禁止写（spec-03 §5.4）。 */
public final class ParameterBaselinePolicy {

    public static final String STATUS_BASELINE_LOCKED = "已基线";

    private ParameterBaselinePolicy() {}

    /**
     * 判断数据状态是否为“已基线”锁定状态。
     *
     * @param dataStatus 数据状态
     * @return 是否已基线锁定
     */
    public static boolean isBaselineLocked(String dataStatus) {
        return STATUS_BASELINE_LOCKED.equals(dataStatus);
    }
}
