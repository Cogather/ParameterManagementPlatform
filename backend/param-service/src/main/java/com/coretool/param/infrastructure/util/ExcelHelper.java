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

public final class ExcelHelper {

    private ExcelHelper() {}

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
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName == null ? "sheet1" : sheetName);
            int r = 0;
            if (instruction != null && !instruction.isBlank()) {
                Row row0 = sheet.createRow(r++);
                row0.createCell(0).setCellValue(instruction);
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
        if (bytes == null || bytes.length == 0) {
            throw new DomainRuleException("文件为空");
        }
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new DomainRuleException("Excel 不包含 sheet");
            }
            DataFormatter fmt = new DataFormatter();
            List<List<String>> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int i = 0; i <= lastRow; i++) {
                Row r = sheet.getRow(i);
                if (r == null) {
                    continue;
                }
                int lastCell = r.getLastCellNum();
                if (lastCell <= 0) {
                    continue;
                }
                List<String> cols = new ArrayList<>();
                boolean allBlank = true;
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = r.getCell(c);
                    String v = cell == null ? "" : fmt.formatCellValue(cell);
                    String vv = v == null ? "" : v.trim();
                    cols.add(vv);
                    if (!vv.isBlank()) {
                        allBlank = false;
                    }
                }
                if (!allBlank) {
                    rows.add(cols);
                }
            }
            return new ParsedSheet(sheet.getSheetName(), rows);
        } catch (Exception e) {
            throw new DomainRuleException("Excel 解析失败: " + e.getMessage());
        }
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
        if (rows == null || rows.isEmpty()) {
            return 0;
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
        // 说明行通常只有第 1 列有值；但即使带多列空串，也同样视为说明行。
        return 1;
    }

    /**
     * Excel 解析结果：sheet 名称与行数据。
     *
     * @param sheetName sheet 名称
     * @param rows      行数据（每行是列字符串列表）
     */
    public record ParsedSheet(String sheetName, List<List<String>> rows) {}
}

