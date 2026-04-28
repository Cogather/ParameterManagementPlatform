package com.coretool.param.ui.vo;

import lombok.Data;

/** GET .../parameters/baseline-count 的 data。 */
@Data
public class BaselineCountPayload {

    private long baselineCount;

    /**
     * 创建空载荷。
     */
    public BaselineCountPayload() {}

    /**
     * 创建载荷并设置基线数量。
     *
     * @param baselineCount 基线数量
     */
    public BaselineCountPayload(long baselineCount) {
        this.baselineCount = baselineCount;
    }
}
