package com.coretool.param.application.service;

import com.coretool.param.infrastructure.util.ExcelHelper;

import java.util.List;

final class ExcelTestHelper {
    private ExcelTestHelper() {}

    static byte[] workbookBytes(String sheetName, String instruction, List<String> headersCn, List<List<String>> rows) {
        return ExcelHelper.buildWorkbook(sheetName, instruction, headersCn, rows);
    }
}

