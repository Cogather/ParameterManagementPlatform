package com.coretool.param.application.support;

import org.apache.commons.lang3.StringUtils;

/**
 * 从 HTTP 请求解析操作人（与前端 DELETE 上 {@code updaterId}/{@code creatorId} query 一致）。
 *
 * @since 2026-05-14
 */
public final class RequestOperatorIds {

    private RequestOperatorIds() {}

    /**
     * 优先 {@code updaterId}，其次 {@code creatorId}；均空白则 {@code null}。
     *
     * @param updaterId 更新人标识（可选）
     * @param creatorId 创建人标识（可选）
     * @return 非空则返回其一，否则 {@code null}
     */
    public static String firstNonBlank(String updaterId, String creatorId) {
        return StringUtils.firstNonBlank(updaterId, creatorId);
    }

    /**
     * 操作日志用操作人：请求优先，其次实体上已有更新人，仍缺省则用 {@code system}。
     *
     * @param fromRequest       请求传入的操作人（可为 {@code null}）
     * @param fallbackFromEntity 实体上的更新人（可为 {@code null}）
     * @return 非空白操作人标识
     */
    public static String operationLogOperator(String fromRequest, String fallbackFromEntity) {
        return StringUtils.defaultIfBlank(StringUtils.firstNonBlank(fromRequest, fallbackFromEntity), "system");
    }
}
