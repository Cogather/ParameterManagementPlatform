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
     * 参数导入/导出说明行（模板/导出文件开头多行展示；导入解析时跳过至表头「参数ID」）。
     *
     * @return 说明行列表（每行写入 Excel 首列）
     */
    public static List<String> parameterImportExportInstructionLines() {
        return List.of(
                "说明：参数导入规则（请从下方表头行起填写数据，勿删表头）",
                "1. 参数ID：导出时带出便于核对；新建导入请留空。系统以「参数编码」为主键匹配（并结合 BIT 占用），存在则更新、否则新增。",
                "2. 全量导入会先删除当前命令（及类型筛选）下既有参数再写入；增量导入已基线行跳过。",
                "3. 【取值区间】（推荐填写）JSON 数组，每段含 min、max 整数，至少 1 段，段间不得重叠。"
                        + " 单段示例：[{\"min\":0,\"max\":255}]；"
                        + " 多段示例：[{\"min\":1,\"max\":10},{\"min\":20,\"max\":30}]。"
                        + " 填好后「取值范围」可留空，系统自动拼接。",
                "4. 【取值范围】（可选，兼容旧模板）文本格式：最小值-最大值；多段用英文逗号分隔。"
                        + " 示例：0-255 或 1-10,20-30。"
                        + " 导入时优先读「取值区间」；该列为空时才解析本列。",
                "5. 新建导入必填（与页面新增一致）：名称（中）、参数默认值/推荐值（整数）、引入版本、单位（中文）、取值区间（或取值范围）；"
                        + " BIT/BYTE/DWORD/STRING 类型另填 BIT 占用。",
                "6. 更新导入叠加详细信息必填（与页面编辑一致）。变更说明：中文原因/影响必填，英文可选；"
                        + " 导出 delta 必填，为「否」时须填不导出原因。");
    }

    /**
     * 参数导入/导出说明（单行拼接，仅供文档引用）。
     *
     * @deprecated 模板/导出请使用 {@link #parameterImportExportInstructionLines()}
     */
    @Deprecated
    public static final String PARAMETER_IMPORT_EXPORT_HINT =
            String.join(" ", parameterImportExportInstructionLines());
}
