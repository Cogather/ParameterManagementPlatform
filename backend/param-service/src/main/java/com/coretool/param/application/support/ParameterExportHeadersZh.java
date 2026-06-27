/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数 Excel 导出/导入模板中文表头。
 *
 * @since 2026-06-11
 */
public final class ParameterExportHeadersZh {

    private ParameterExportHeadersZh() {}

    /**
     * 导入模板表头（含取值区间、变更说明列组，与导入解析一致）。
     *
     * @return 表头列名列表
     */
    public static List<String> listForImport() {
        List<String> headers = new ArrayList<>(52);
        headers.addAll(identityHeaders());
        headers.addAll(basicInfoHeadersForImport());
        headers.addAll(detailInfoHeaders());
        headers.addAll(changeDescriptionHeaders());
        return List.copyOf(headers);
    }

    /**
     * 导出表头（不含变更说明；仅「取值范围」文本列，不含 JSON「取值区间」列）。
     *
     * @return 表头列名列表
     */
    public static List<String> listForExport() {
        List<String> headers = new ArrayList<>(45);
        headers.addAll(identityHeaders());
        headers.addAll(basicInfoHeadersForExport());
        headers.addAll(detailInfoHeaders());
        return List.copyOf(headers);
    }

    /**
     * 导入表头别名（兼容旧调用，等同 {@link #listForImport()}）。
     *
     * @return 表头列名列表
     */
    public static List<String> list() {
        return listForImport();
    }

    private static List<String> identityHeaders() {
        return List.of("参数ID", "归属命令", "参数编码", "序号");
    }

    private static List<String> basicInfoHeadersForImport() {
        return List.of(
                "参数名称（中）",
                "参数名称（英）",
                "取值区间",
                "取值范围",
                "BIT 占用",
                "参数默认值",
                "参数推荐值",
                "引入版本",
                "单位（中文）",
                "单位（英文）");
    }

    private static List<String> basicInfoHeadersForExport() {
        return List.of(
                "参数名称（中）",
                "参数名称（英）",
                "取值范围",
                "BIT 占用",
                "参数默认值",
                "参数推荐值",
                "引入版本",
                "单位（中文）",
                "单位（英文）");
    }

    private static List<String> detailInfoHeaders() {
        return List.of(
                "取值说明（中）",
                "取值说明（英）",
                "应用场景（中）",
                "应用场景（英）",
                "适用网元",
                "业务分类",
                "生效方式（中）",
                "生效方式（英）",
                "项目组",
                "归属模块",
                "参数含义（中）",
                "参数含义（英）",
                "影响说明（中）",
                "影响说明（英）",
                "配置举例（中）",
                "配置举例（英）",
                "是否发布",
                "不发布原因",
                "关联参数描述（中）",
                "关联参数描述（英）",
                "所属特性",
                "影响级别（中文）",
                "影响级别（英文）",
                "关联 License",
                "内部功能描述",
                "产品形态ID",
                "平台代际",
                "应用区域",
                "备注",
                "数据状态");
    }

    private static List<String> changeDescriptionHeaders() {
        return List.of(
                "变更类型",
                "变更原因（中）",
                "变更影响（中）",
                "变更原因（英）",
                "变更影响（英）",
                "导出 delta",
                "不导出原因");
    }
}
