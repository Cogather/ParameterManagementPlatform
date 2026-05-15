/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.exception;

/**
 * 领域规则违反（充血模型内抛出）；由应用层或全局异常处理映射为对外错误信息。
 *
 * @since 2026-04-28
 */
public class DomainRuleException extends RuntimeException {

    /**
     * 创建领域规则异常。
     *
     * @param message 异常信息
     */
    public DomainRuleException(String message) {
        super(message);
    }

    /**
     * 创建领域规则异常（包含原因异常）。
     *
     * @param message 异常信息
     * @param cause   原因异常
     */
    public DomainRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
