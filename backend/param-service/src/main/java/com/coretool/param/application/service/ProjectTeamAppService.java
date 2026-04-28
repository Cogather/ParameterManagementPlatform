package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.domain.config.projectteam.service.ProjectTeamDomainService;
import com.coretool.param.domain.config.projectteam.repository.ProjectTeamRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.assembly.ProjectTeamAssembler;
import com.coretool.param.infrastructure.persistence.entity.ProjectTeamDictPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectTeamAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("ID", "项目组名称", "项目组描述", "责任人");

    @Resource
    private ProjectTeamRepository projectTeamRepository;

    @Resource
    private OperationLogAppService operationLogAppService;

    private ProjectTeamDomainService domainService;

    /**
     * 分页查询项目组字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<ProjectTeamDictPo> page(
            String productId, ListPageWithKeywordQuery q) {
        var slice =
                projectTeamRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<ProjectTeamDictPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(ProjectTeamAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(q.getPage());
        resp.setSize(q.getSize());
        return resp;
    }

    /**
     * 新增项目组字典项（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public ProjectTeamDictPo create(String productId, ProjectTeamDictPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String name = input.getTeamName() == null ? "" : input.getTeamName().trim();
        ProjectTeam disabled = projectTeamRepository.findDisabledByNameInProduct(productId, name).orElse(null);
        if (disabled != null) {
            ProjectTeam before =
                    ProjectTeam.rehydrate(
                            new ProjectTeam.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getTeamId(),
                                    disabled.getTeamName(),
                                    disabled.getTeamDescription(),
                                    disabled.getTeamStatus(),
                                    disabled.getOwnerList(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyPatch(
                    new ProjectTeam.Patch(
                            name,
                            input.getTeamDescription(),
                            1,
                            input.getOwnerList(),
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            now));
            projectTeamRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logProjectTeamUpdate(before, disabled, opR);
            return ProjectTeamAssembler.toPo(disabled);
        }

        if (StringUtils.isBlank(input.getTeamId())) {
            input.setTeamId(IdGenerator.teamId());
        }
        input.setTeamStatus(1);
        ProjectTeam t =
                ensureDomain()
                        .createNew(
                                new ProjectTeamDomainService.CreateCommand(
                                        productId,
                                        input.getTeamId(),
                                        input.getTeamName(),
                                        input.getTeamDescription(),
                                        input.getTeamStatus(),
                                        input.getOwnerList(),
                                        input.getCreatorId(),
                                        input.getUpdaterId(),
                                        now));
        projectTeamRepository.insert(t);
        ProjectTeamDictPo out = ProjectTeamAssembler.toPo(t);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), t.getCreatorId()),
                        "system");
        operationLogAppService.logProjectTeamCreate(productId, out, w);
        return out;
    }

    /**
     * 更新项目组字典项。
     *
     * @param productId 产品 ID
     * @param teamId    项目组 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public ProjectTeamDictPo update(String productId, String teamId, ProjectTeamDictPo input) {
        LocalDateTime now = LocalDateTime.now();
        ProjectTeam before =
                projectTeamRepository
                        .findByTeamId(teamId)
                        .filter(t -> t.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("项目组不存在或不属于该产品"));
        ProjectTeam existing =
                ensureDomain()
                        .updateExisting(
                                new ProjectTeamDomainService.UpdateCommand(
                                        productId,
                                        teamId,
                                        input == null ? null : input.getTeamName(),
                                        input == null ? null : input.getTeamDescription(),
                                        input == null ? null : input.getTeamStatus(),
                                        input == null ? null : input.getOwnerList(),
                                        input == null ? null : input.getUpdaterId(),
                                        now));
        projectTeamRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logProjectTeamUpdate(before, existing, opU);
        return ProjectTeamAssembler.toPo(existing);
    }

    /**
     * 禁用项目组字典项。
     *
     * @param productId 产品 ID
     * @param teamId    项目组 ID
     */
    @Transactional
    public void disable(String productId, String teamId) {
        ProjectTeam pre = projectTeamRepository.findByTeamId(teamId).orElse(null);
        String display = pre == null ? teamId : StringUtils.defaultIfBlank(pre.getTeamName(), teamId);
        ProjectTeam e = ensureDomain().disable(productId, teamId, LocalDateTime.now());
        projectTeamRepository.update(e);
        String opD = StringUtils.defaultIfBlank(e.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, teamId, display, opD));
    }

    /**
     * 导入项目组字典（Excel 第一张表）。
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
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    ProjectTeamDictPo po = new ProjectTeamDictPo();
                    po.setTeamId(StringUtils.trimToNull(col(cols, idx, "ID")));
                    po.setTeamName(col(cols, idx, "项目组名称"));
                    po.setTeamDescription(StringUtils.trimToNull(col(cols, idx, "项目组描述")));
                    po.setTeamStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    po.setOwnerList(col(cols, idx, "责任人"));
                    if (StringUtils.isBlank(po.getTeamId())) {
                        create(productId, po);
                    } else {
                        ProjectTeam ex = projectTeamRepository.findByTeamId(po.getTeamId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        ensureDomain().requireOwned(productId, po.getTeamId());
                        po.setUpdaterId("system");
                        update(productId, po.getTeamId(), po);
                    }
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
     * 导出项目组字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<ProjectTeamDictPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getTeamId()),
                                                StringUtils.defaultString(r.getTeamName()),
                                                StringUtils.defaultString(r.getTeamDescription()),
                                                StringUtils.defaultString(r.getOwnerList())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("项目组", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取项目组字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("项目组", INSTRUCTION, HEADERS_CN);
    }

    private ProjectTeamDomainService ensureDomain() {
        if (domainService == null) {
            domainService = new ProjectTeamDomainService(projectTeamRepository);
        }
        return domainService;
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
