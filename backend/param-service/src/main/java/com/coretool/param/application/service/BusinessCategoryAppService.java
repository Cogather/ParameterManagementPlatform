package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.config.category.repository.BusinessCategoryRepository;
import com.coretool.param.domain.config.category.service.BusinessCategoryDomainService;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.BusinessCategoryAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.exception.BizException;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessCategoryAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN =
            List.of("分类ID", "分类名称（中文）", "分类名称（英文）", "包含特性范围", "所属类别");

    private final BusinessCategoryRepository categoryRepository;
    private final BusinessCategoryDomainService domainService;
    private final OperationLogAppService operationLogAppService;

    /**
     * 构造函数。
     *
     * @param categoryRepository     业务分类仓储
     * @param operationLogAppService 操作日志应用服务
     */
    public BusinessCategoryAppService(
            BusinessCategoryRepository categoryRepository, OperationLogAppService operationLogAppService) {
        this.categoryRepository = categoryRepository;
        this.domainService = new BusinessCategoryDomainService(categoryRepository);
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 分页查询业务分类字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<EntityBusinessCategoryPo> page(
            String productId, ListPageWithKeywordQuery q) {
        PageSlice<BusinessCategory> slice =
                categoryRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<EntityBusinessCategoryPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(BusinessCategoryAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(slice.getPage());
        resp.setSize(slice.getSize());
        return resp;
    }

    /**
     * 新增业务分类字典项（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityBusinessCategoryPo create(String productId, EntityBusinessCategoryPo input) {
        if (input == null
                || StringUtils.isBlank(input.getCategoryNameCn())
                || StringUtils.isBlank(input.getCategoryNameEn())) {
            throw new DomainRuleException("分类中英文名称不能为空");
        }
        String nameCn = input.getCategoryNameCn().trim();
        BusinessCategory disabled = categoryRepository.findDisabledByChineseNameInProduct(productId, nameCn).orElse(null);
        if (disabled != null) {
            BusinessCategory before =
                    BusinessCategory.rehydrate(
                            new BusinessCategory.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getCategoryId(),
                                    disabled.getCategoryNameCn(),
                                    disabled.getCategoryNameEn(),
                                    disabled.getFeatureRange(),
                                    disabled.getCategoryType(),
                                    disabled.getCategoryStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyEditablePatch(
                    new BusinessCategory.EditablePatch(
                            nameCn,
                            input.getCategoryNameEn(),
                            input.getFeatureRange(),
                            input.getCategoryType(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            LocalDateTime.now()));
            categoryRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logBusinessCategoryUpdate(before, disabled, opR);
            return BusinessCategoryAssembler.toPo(disabled);
        }
        if (StringUtils.isBlank(input.getCategoryId())) {
            input.setCategoryId(IdGenerator.categoryId());
        }
        input.setCategoryStatus(1);
        LocalDateTime now = LocalDateTime.now();
        BusinessCategory c =
                domainService.createNew(
                        new BusinessCategoryDomainService.CreateCommand(
                                productId,
                                input.getCategoryId(),
                                input.getCategoryNameCn(),
                                input.getCategoryNameEn(),
                                input.getFeatureRange(),
                                input.getCategoryType(),
                                input.getCategoryStatus(),
                                input.getCreatorId(),
                                input.getUpdaterId(),
                                now));
        categoryRepository.insert(c);
        EntityBusinessCategoryPo out = BusinessCategoryAssembler.toPo(c);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), c.getCreatorId()),
                        "system");
        operationLogAppService.logBusinessCategoryCreate(productId, out, w);
        return out;
    }

    /**
     * 更新业务分类字典项。
     *
     * @param productId  产品 ID
     * @param categoryId 分类 ID
     * @param input      请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityBusinessCategoryPo update(
            String productId, String categoryId, EntityBusinessCategoryPo input) {
        BusinessCategory before =
                categoryRepository
                        .findByCategoryId(categoryId)
                        .filter(x -> x.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("业务分类不存在或不属于该产品"));
        BusinessCategory existing =
                domainService.updateExisting(
                        new BusinessCategoryDomainService.UpdateCommand(
                                productId,
                                categoryId,
                                input == null ? null : input.getCategoryNameCn(),
                                input == null ? null : input.getCategoryNameEn(),
                                input == null ? null : input.getFeatureRange(),
                                input == null ? null : input.getCategoryType(),
                                input == null ? null : input.getCategoryStatus(),
                                input == null ? null : input.getUpdaterId(),
                                LocalDateTime.now()));
        categoryRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logBusinessCategoryUpdate(before, existing, opU);
        return BusinessCategoryAssembler.toPo(existing);
    }

    /**
     * 禁用业务分类字典项。
     *
     * @param productId  产品 ID
     * @param categoryId 分类 ID
     */
    @Transactional
    public void disable(String productId, String categoryId) {
        BusinessCategory pre = categoryRepository.findByCategoryId(categoryId).orElse(null);
        String display = pre == null ? categoryId : StringUtils.defaultIfBlank(pre.getCategoryNameCn(), categoryId);
        BusinessCategory existing = domainService.disable(productId, categoryId, LocalDateTime.now());
        categoryRepository.update(existing);
        String opD = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logBusinessCategoryDelete(productId, categoryId, display, opD);
    }

    /**
     * 导入业务分类字典（Excel 第一张表）。
     *
     * @param productId 产品 ID
     * @param bytes     文件内容
     * @return 导入结果
     */
    public BatchImportResult importCsv(String productId, byte[] bytes) {
        operationLogAppService.beginImportBatch();
        try {
            ExcelHelper.ParsedSheet sheet = ExcelHelper.parseFirstSheet(bytes);
            List<List<String>> rows = sheet.rows();
            if (rows.size() <= 1) {
                BatchImportResult empty = new BatchImportResult();
                empty.setTotalRows(0);
                empty.setSuccessCount(0);
                empty.setFailureCount(0);
                empty.setSuccessRowNumbers(List.of());
                empty.setFailures(List.of());
                return empty;
            }
            int headerIdx = ExcelHelper.detectHeaderRowIndex(rows);
            if (rows.size() <= headerIdx) {
                throw new BizException("表头缺失");
            }
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    upsertFromExcelRow(productId, rows.get(i), idx);
                    c.success(line);
                } catch (Exception ex) {
                    c.failure(line, ex.getMessage() == null ? "处理失败" : ex.getMessage());
                }
            }
            return c.build(dataRows);
        } finally {
            operationLogAppService.endImportBatch();
        }
    }

    /**
     * 导出业务分类字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<EntityBusinessCategoryPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getCategoryId()),
                                                StringUtils.defaultString(r.getCategoryNameCn()),
                                                StringUtils.defaultString(r.getCategoryNameEn()),
                                                StringUtils.defaultString(r.getFeatureRange()),
                                                StringUtils.defaultString(r.getCategoryType())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("业务分类", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取业务分类字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("业务分类", INSTRUCTION, HEADERS_CN);
    }

    private void upsertFromExcelRow(String productId, List<String> cols, Map<String, Integer> idx) {
        EntityBusinessCategoryPo po = new EntityBusinessCategoryPo();
        po.setCategoryId(StringUtils.trimToNull(col(cols, idx, "分类ID")));
        po.setCategoryNameCn(col(cols, idx, "分类名称（中文）"));
        po.setCategoryNameEn(col(cols, idx, "分类名称（英文）"));
        po.setFeatureRange(StringUtils.trimToNull(col(cols, idx, "包含特性范围")));
        po.setCategoryType(StringUtils.trimToNull(col(cols, idx, "所属类别")));
        po.setCategoryStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
        if (StringUtils.isBlank(po.getCategoryId())) {
            create(productId, po);
        } else {
            BusinessCategory ex =
                    categoryRepository
                            .findByCategoryId(po.getCategoryId())
                            .orElseThrow(() -> new DomainRuleException("分类不存在"));
            if (!ex.belongsToProduct(productId)) {
                throw new DomainRuleException("分类编码与其他产品冲突");
            }
            po.setUpdaterId("system");
            update(productId, po.getCategoryId(), po);
        }
    }

    private static int parseIntDefault(String s, int def) {
        if (StringUtils.isBlank(s)) {
            return def;
        }
        return Integer.parseInt(s.trim());
    }

    private static String col(List<String> cols, Map<String, Integer> idx, String headerCn) {
        Integer i = idx.get(headerCn);
        if (i == null || i < 0 || i >= cols.size()) {
            return "";
        }
        return cols.get(i) == null ? "" : cols.get(i).trim();
    }
}
