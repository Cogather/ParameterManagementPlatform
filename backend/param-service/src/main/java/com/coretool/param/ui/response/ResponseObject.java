package com.coretool.param.ui.response;

import com.coretool.param.constants.CommonConst;

public class ResponseObject<T> {
    private boolean success;
    private String message;
    private T data;

    /**
     * 获取是否成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置是否成功。
     *
     * @param success 是否成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取提示信息。
     *
     * @return 提示信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置提示信息。
     *
     * @param message 提示信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取业务数据载荷。
     *
     * @return 业务数据载荷
     */
    public T getData() {
        return data;
    }

    /**
     * 设置业务数据载荷。
     *
     * @param data 业务数据载荷
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 设置数据并返回自身（链式调用）。
     *
     * @param data 业务数据载荷
     * @return 当前对象
     */
    public ResponseObject<T> setDataAndReturn(T data) {
        this.data = data;
        return this;
    }

    /**
     * 返回成功响应（message=OK）。
     *
     * @return 当前对象
     */
    public ResponseObject<T> success() {
        this.success = true;
        this.message = CommonConst.OK;
        return this;
    }

    /**
     * 返回成功响应并设置数据（message=OK）。
     *
     * @param data 业务数据载荷
     * @return 当前对象
     */
    public ResponseObject<T> success(T data) {
        this.success = true;
        this.message = CommonConst.OK;
        this.data = data;
        return this;
    }

    /**
     * 返回成功响应并设置提示信息。
     *
     * @param message 提示信息
     * @return 当前对象
     */
    public ResponseObject<T> success(String message) {
        this.success = true;
        this.message = message;
        return this;
    }

    /**
     * 返回失败响应（message=ERROR）。
     *
     * @return 当前对象
     */
    public ResponseObject<T> failure() {
        this.success = false;
        this.message = CommonConst.ERROR;
        return this;
    }

    /**
     * 返回失败响应并设置提示信息。
     *
     * @param message 提示信息
     * @return 当前对象
     */
    public ResponseObject<T> failure(String message) {
        this.success = false;
        this.message = message;
        return this;
    }
}

