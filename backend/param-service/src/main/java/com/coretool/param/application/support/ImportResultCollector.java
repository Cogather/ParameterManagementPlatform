package com.coretool.param.application.support;

import com.coretool.param.ui.response.BatchImportResult;

import java.util.ArrayList;
import java.util.List;

public class ImportResultCollector {
    private final List<Integer> successRowNumbers = new ArrayList<>();
    private final List<BatchImportResult.BatchImportFailure> failures = new ArrayList<>();

    /**
     * 记录导入成功的行号（以 Excel/CSV 的数据行号为准）。
     *
     * @param rowNumber 行号
     */
    public void success(int rowNumber) {
        successRowNumbers.add(rowNumber);
    }

    /**
     * 记录导入失败的行号与原因（用于批量导入结果回显）。
     *
     * @param rowNumber 行号
     * @param reason    失败原因
     */
    public void failure(int rowNumber, String reason) {
        BatchImportResult.BatchImportFailure f = new BatchImportResult.BatchImportFailure();
        f.setRowNumber(rowNumber);
        f.setReason(reason);
        failures.add(f);
    }

    /**
     * 构建批量导入结果对象。
     *
     * @param totalDataRows 数据总行数
     * @return 导入结果
     */
    public BatchImportResult build(int totalDataRows) {
        BatchImportResult r = new BatchImportResult();
        r.setTotalRows(totalDataRows);
        r.setSuccessCount(successRowNumbers.size());
        r.setFailureCount(failures.size());
        r.setSuccessRowNumbers(new ArrayList<>(successRowNumbers));
        r.setFailures(new ArrayList<>(failures));
        return r;
    }
}
