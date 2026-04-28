package com.coretool.param.ui.controller;

import com.coretool.param.infrastructure.util.ExcelHelper;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CsvDownloadTest {

    @Test
    void attachment_shouldReturnCsvHeaders_whenFilenameIsCsv() {
        byte[] bytes = "a,b".getBytes(StandardCharsets.UTF_8);

        var res = CsvDownload.attachment(bytes, "demo.csv");

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getHeaders().getContentType())
                .isEqualTo(new MediaType("text", "csv", StandardCharsets.UTF_8));
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"demo.csv\"");
        assertThat(res.getBody()).isEqualTo(bytes);
    }

    @Test
    void attachment_shouldReturnXlsxContentType_whenFilenameIsXlsx_ignoreCase() {
        byte[] bytes = new byte[] {1, 2, 3};

        var res = CsvDownload.attachment(bytes, "report.XLSX");

        assertThat(res.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType(ExcelHelper.XLSX_CONTENT_TYPE));
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"report.XLSX\"");
    }

    @Test
    void attachment_shouldFallbackToCsv_whenFilenameIsNull() {
        byte[] bytes = new byte[] {9};

        var res = CsvDownload.attachment(bytes, null);

        assertThat(res.getHeaders().getContentType())
                .isEqualTo(new MediaType("text", "csv", StandardCharsets.UTF_8));
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"null\"");
        assertThat(res.getBody()).containsExactly(9);
    }
}
