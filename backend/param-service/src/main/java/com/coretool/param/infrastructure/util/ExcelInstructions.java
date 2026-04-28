package com.coretool.param.infrastructure.util;

/**
 * Excel 导入/导出模板首行提示语常量。
 *
 * <p>用于统一各模块导出/模板下载的“说明：...”文案，避免多处散落导致漏改。
 */
public final class ExcelInstructions {

    private ExcelInstructions() {}

    /** 通用：导入/导出按 ID 为空新增、有值修改（不改 ID）。 */
    public static final String ID_CREATE_UPDATE_HINT = "说明：新增时ID必须为空，修改时ID无需更改。";
}

