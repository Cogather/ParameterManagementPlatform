/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.coretool.param.infrastructure.util.ExcelHelper;

import java.util.List;

/**
 * 应用服务层 Excel 导入导出测试辅助工具。
 *
 * @since 2026-04-28
 */
final class ExcelTestHelper {
    private ExcelTestHelper() {}

    static byte[] workbookBytes(String sheetName, String instruction, List<String> headersCn, List<List<String>> rows) {
        return ExcelHelper.buildWorkbook(sheetName, instruction, headersCn, rows);
    }

    static byte[] workbookBytes(
            String sheetName, List<String> instructionLines, List<String> headersCn, List<List<String>> rows) {
        return ExcelHelper.buildWorkbook(sheetName, instructionLines, headersCn, rows);
    }
}

