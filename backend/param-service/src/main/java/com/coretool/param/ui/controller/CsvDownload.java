package com.coretool.param.ui.controller;

import com.coretool.param.infrastructure.util.ExcelHelper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * 下载响应工具：将内容以附件形式输出（CSV/XLSX）。
 *
 * @since 2026-04-24
 */
public final class CsvDownload {
    private CsvDownload() {}

    /**
     * 以附件形式返回字节内容。
     *
     * @param bytes    文件内容
     * @param filename 文件名（用于 Content-Disposition；用于推断内容类型）
     * @return 响应实体
     */
    public static ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
            headers.setContentType(MediaType.parseMediaType(ExcelHelper.XLSX_CONTENT_TYPE));
        } else {
            headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        }
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
