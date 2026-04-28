package com.coretool.param.ui.controller;

import org.apache.commons.lang3.StringUtils;

/**
 * spec-03 §4：参数列表与 available-* 使用 {@code commandTypeId}；历史实现使用 {@code commandTypeCode}（类型前缀，与
 * {@code parameter_code} 一致）。二者语义相同，本类做统一解析。
 */
public final class CommandTypeQueryParam {

    private CommandTypeQueryParam() {}

    /** 优先 {@code commandTypeId}，否则 {@code commandTypeCode}。可全空（表示不按类型筛）。 */
    /**
     * 解析类型筛选键（可空）。
     *
     * <p>优先使用 {@code commandTypeId}，否则使用 {@code commandTypeCode}；二者均为空表示不按类型筛选。
     *
     * @param commandTypeId   类型 ID（可选）
     * @param commandTypeCode 类型编码（可选）
     * @return 类型键（可为 null）
     */
    public static String optionalTypeKey(String commandTypeId, String commandTypeCode) {
        if (StringUtils.isNotBlank(commandTypeId)) {
            return commandTypeId.trim();
        }
        return commandTypeCode == null ? null : commandTypeCode.trim();
    }

    /** 列表/available-* 等必填其一时的类型键。 */
    /**
     * 解析必填的类型键（至少传入其一）。
     *
     * @param commandTypeId   类型 ID（可选）
     * @param commandTypeCode 类型编码（可选）
     * @return 类型键（非空）
     * @throws IllegalArgumentException 两者均为空时抛出
     */
    public static String requireTypeKey(String commandTypeId, String commandTypeCode) {
        String v = optionalTypeKey(commandTypeId, commandTypeCode);
        if (StringUtils.isBlank(v)) {
            throw new IllegalArgumentException("commandTypeId 与 commandTypeCode 须至少传其一（与 spec-03 一致）");
        }
        return v;
    }
}
