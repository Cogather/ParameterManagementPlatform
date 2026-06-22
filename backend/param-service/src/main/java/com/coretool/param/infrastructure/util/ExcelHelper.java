/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.infrastructure.util;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具/说明类「ExcelHelper」。
 *
 * @since 2026-04-28
 */

public final class ExcelHelper {

    private ExcelHelper() {}

    /**
     * 字段「XLSX_CONTENT_TYPE」。
     */

    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 构建 Excel 模板（仅表头与说明行）。
     *
     * @param sheetName    sheet 名称
     * @param instruction  说明文案（可为空）
     * @param headersCn    表头（中文）
     * @return xlsx 字节
     */
    public static byte[] buildTemplate(String sheetName, String instruction, List<String> headersCn) {
        return buildWorkbook(sheetName, instruction, headersCn, List.of());
    }

    /**
     * 构建 Excel 模板（多行说明 + 表头）。
     *
     * @param sheetName         sheet 名称
     * @param instructionLines  说明行（可为空）
     * @param headersCn         表头（中文）
     * @return xlsx 字节
     */
    public static byte[] buildTemplate(
            String sheetName, List<String> instructionLines, List<String> headersCn) {
        return buildWorkbook(sheetName, instructionLines, headersCn, List.of());
    }

    /**
     * 构建 Excel 工作簿。
     *
     * @param sheetName    sheet 名称
     * @param instruction  说明文案（可为空）
     * @param headersCn    表头（中文）
     * @param rows         数据行
     * @return xlsx 字节
     */
    public static byte[] buildWorkbook(
            String sheetName, String instruction, List<String> headersCn, List<List<String>> rows) {
        if (instruction == null || instruction.isBlank()) {
            return buildWorkbook(sheetName, List.of(), headersCn, rows);
        }
        return buildWorkbook(sheetName, List.of(instruction), headersCn, rows);
    }

    /**
     * 构建 Excel 工作簿（支持多行说明）。
     *
     * @param sheetName         sheet 名称
     * @param instructionLines  说明行（可为空）
     * @param headersCn         表头（中文）
     * @param rows              数据行
     * @return xlsx 字节
     */
    public static byte[] buildWorkbook(
            String sheetName, List<String> instructionLines, List<String> headersCn, List<List<String>> rows) {
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName == null ? "sheet1" : sheetName);
            int r = 0;
            if (instructionLines != null) {
                for (String instruction : instructionLines) {
                    if (instruction == null || instruction.isBlank()) {
                        continue;
                    }
                    Row row0 = sheet.createRow(r++);
                    row0.createCell(0).setCellValue(instruction);
                }
            }
            Row header = sheet.createRow(r++);
            for (int i = 0; i < headersCn.size(); i++) {
                header.createCell(i).setCellValue(headersCn.get(i));
            }
            for (List<String> data : rows) {
                Row rr = sheet.createRow(r++);
                for (int i = 0; i < headersCn.size(); i++) {
                    String v = i < data.size() ? data.get(i) : null;
                    rr.createCell(i).setCellValue(v == null ? "" : v);
                }
            }
            for (int i = 0; i < headersCn.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new DomainRuleException("Excel 生成失败: " + e.getMessage());
        }
    }

    /**
     * 解析第一个 sheet（返回非空白行，单元格字符串已 trim）。
     *
     * @param bytes xlsx 字节
     * @return 解析结果
     */
    public static ParsedSheet parseFirstSheet(byte[] bytes) {
        requireNonEmptyBytes(bytes);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = firstSheetOrThrow(wb);
            DataFormatter fmt = new DataFormatter();
            List<List<String>> rows = readNonBlankRows(sheet, fmt);
            return new ParsedSheet(sheet.getSheetName(), rows);
        } catch (DomainRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainRuleException("Excel 解析失败: " + e.getMessage());
        }
    }

    private static void requireNonEmptyBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new DomainRuleException("文件为空");
        }
    }

    private static Sheet firstSheetOrThrow(Workbook wb) {
        if (wb.getNumberOfSheets() <= 0) {
            throw new DomainRuleException("Excel 不包含 sheet");
        }
        return wb.getSheetAt(0);
    }

    private static List<List<String>> readNonBlankRows(Sheet sheet, DataFormatter fmt) {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            List<String> cols = readRowIfPresent(sheet.getRow(i), fmt);
            if (cols != null) {
                rows.add(cols);
            }
        }
        return rows;
    }

    private static List<String> readRowIfPresent(Row r, DataFormatter fmt) {
        if (r == null) {
            return null;
        }
        int lastCell = r.getLastCellNum();
        if (lastCell <= 0) {
            return null;
        }
        List<String> cols = readRowCells(r, fmt, lastCell);
        return rowHasContent(cols) ? cols : null;
    }

    private static List<String> readRowCells(Row r, DataFormatter fmt, int lastCell) {
        List<String> cols = new ArrayList<>(lastCell);
        for (int c = 0; c < lastCell; c++) {
            Cell cell = r.getCell(c);
            String v = cell == null ? "" : fmt.formatCellValue(cell);
            cols.add(v == null ? "" : v.trim());
        }
        return cols;
    }

    private static boolean rowHasContent(List<String> cols) {
        for (String vv : cols) {
            if (!vv.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将表头行转换为“表头名 -> 列索引”的映射。
     *
     * @param headerRow 表头行
     * @return 索引映射
     */
    public static Map<String, Integer> headerIndex(List<String> headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String h = headerRow.get(i);
            if (h != null && !h.isBlank()) {
                map.put(h.trim(), i);
            }
        }
        return map;
    }

    /**
     * 推断表头所在行下标。
     *
     * <p>本项目导出/模板首行可能为“说明：...”，其行通常仅第 1 列有值；表头为下一行。
     *
     * @param rows 解析后的非空白行
     * @return 表头行下标（默认 0；若首行为说明行则返回 1）
     */
    public static int detectHeaderRowIndex(List<List<String>> rows) {
        return detectHeaderRowIndex(rows, new String[0]);
    }

    /**
     * 推断表头所在行下标；若提供 anchor 列名则优先匹配含该表头单元格的行（用于多行说明的参数模板）。
     *
     * @param rows          解析后的非空白行
     * @param anchorHeaders 表头锚点列名（如「参数ID」「参数编码」）
     * @return 表头行下标
     */
    public static int detectHeaderRowIndex(List<List<String>> rows, String... anchorHeaders) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        if (anchorHeaders != null && anchorHeaders.length > 0) {
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null) {
                    continue;
                }
                for (String cell : row) {
                    if (cell == null || cell.isBlank()) {
                        continue;
                    }
                    String trimmed = cell.trim();
                    for (String anchor : anchorHeaders) {
                        if (trimmed.equals(anchor)) {
                            return i;
                        }
                    }
                }
            }
        }
        List<String> first = rows.get(0);
        if (first == null || first.isEmpty()) {
            return 0;
        }
        String c0 = first.get(0) == null ? "" : first.get(0).trim();
        if (!c0.startsWith("说明：")) {
            return 0;
        }
        if (rows.size() < 2) {
            return 0;
        }
        return 1;
    }

    /**
     * Excel 解析结果：sheet 名称与行数据。
     *
     * @since 2026-04-28
     * @param sheetName sheet 名称
     * @param rows      行数据（每行是列字符串列表）
     */
    public record ParsedSheet(String sheetName, List<List<String>> rows) {}
}
