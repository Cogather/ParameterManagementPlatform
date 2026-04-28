package com.coretool.param.ui.exception;

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

