package com.coretool.param.ui.exception;

/**
 * 异常类型「BizException」。
 *
 * @since 2026-04-28
 */

public class BizException extends RuntimeException {
    /**
     * 创建业务异常。
     *
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
    }
}
