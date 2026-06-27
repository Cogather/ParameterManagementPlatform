/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.infrastructure.util;

import java.util.List;

/**
 * Excel 导入/导出模板首行提示语常量。
 *
 * <p>用于统一各模块导出/模板下载的“说明：...”文案，避免多处散落导致漏改。
 *
 * @since 2026-04-28
 */
public final class ExcelInstructions {

    private ExcelInstructions() {}

    /** 通用：导入/导出按 ID 为空新增、有值修改（不改 ID）。 */
    public static final String ID_CREATE_UPDATE_HINT = "说明：新增时ID必须为空，修改时ID无需更改。";

    /**
     * 参数导出文件说明行（写入导出 XLSX 开头；导入解析时跳过至表头「参数ID」）。
     *
     * @return 说明行列表（每行写入 Excel 首列）
     */
    public static List<String> parameterExportInstructionLines() {
        return List.of(
                "说明：请从下方表头行起填写数据，勿删表头。",
                "1. 参数ID：新增时请留空；修改时保留导出值，系统将按 ID 更新。",
                "2. 全量导入会先删除对应命令下既有参数再写入；增量导入时已基线行会跳过。",
                "3. 取值范围单段写「最小值-最大值」，多段用英文逗号分隔。示例：0-255 或 1-10,20-30。");
    }

    /**
     * 参数导入模板说明行（与导出说明一致；导入仍支持变更说明列，可选填一条）。
     *
     * @return 说明行列表（每行写入 Excel 首列）
     */
    public static List<String> parameterImportInstructionLines() {
        return parameterExportInstructionLines();
    }

    /**
     * 参数导入/导出说明行（兼容旧调用，等同 {@link #parameterImportInstructionLines()}）。
     *
     * @return 说明行列表（每行写入 Excel 首列）
     */
    public static List<String> parameterImportExportInstructionLines() {
        return parameterImportInstructionLines();
    }

    /**
     * 参数导入/导出说明（单行拼接，仅供文档引用）。
     *
     * @deprecated 模板/导出请使用 {@link #parameterExportInstructionLines()} 或 {@link #parameterImportInstructionLines()}
     */
    @Deprecated
    public static final String PARAMETER_IMPORT_EXPORT_HINT =
            String.join(" ", parameterImportExportInstructionLines());
}
