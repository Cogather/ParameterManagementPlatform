package com.coretool.param.infrastructure.util;

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
     * 参数导入/导出：首列为系统主键「参数ID」；匹配以参数编码为主，与 {@link #ID_CREATE_UPDATE_HINT} 语义不同。
     */
    public static final String PARAMETER_IMPORT_EXPORT_HINT =
            "说明：首列「参数ID」为系统主键，导出时带出以便核对；新建导入该行请留空。"
                    + "系统以「参数编码」为主匹配当前命令与版本下已有行（并结合 BIT 占用），存在则更新、否则新增。"
                    + "全量导入会先删除当前筛选范围内的既有参数再写入。";
}
