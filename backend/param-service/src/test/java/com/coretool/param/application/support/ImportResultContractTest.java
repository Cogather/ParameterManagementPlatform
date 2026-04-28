package com.coretool.param.application.support;

import com.coretool.param.ui.response.BatchImportResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImportResultContractTest {

    @Test
    void batchImportResultMatchesOpenSpecImportResultJsonShape() {
        ImportResultCollector c = new ImportResultCollector();
        c.success(1);
        c.failure(2, "row error");
        BatchImportResult r = c.build(2);

        assertEquals(2, r.getTotalRows());
        assertEquals(1, r.getSuccessCount());
        assertEquals(1, r.getFailureCount());
        assertNotNull(r.getSuccessRowNumbers());
        assertEquals(1, r.getSuccessRowNumbers().size());
        assertNotNull(r.getFailures());
        assertEquals(1, r.getFailures().size());
        assertEquals(2, r.getFailures().get(0).getRowNumber());
        assertEquals("row error", r.getFailures().get(0).getReason());
    }
}
