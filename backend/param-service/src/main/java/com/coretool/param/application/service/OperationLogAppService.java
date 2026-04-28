package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.command.Command;
import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeSourceKeywordPo;
import com.coretool.param.infrastructure.persistence.entity.EntityApplicableNeDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityBasicInfoPo;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveFormDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveModeDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.infrastructure.persistence.entity.OperationLogPo;
import com.coretool.param.infrastructure.persistence.entity.ProjectTeamDictPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
import com.coretool.param.infrastructure.persistence.mapper.OperationLogMapper;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.OperationLogGroupKey;
import com.coretool.param.ui.vo.OperationLogGroupItem;
import com.coretool.param.ui.vo.OperationLogGroupLine;
import com.coretool.param.ui.vo.OperationLogGroupPageQuery;
import com.coretool.param.ui.vo.OperationLogGroupSelectQuery;
import com.coretool.param.ui.vo.OperationLogPageQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 操作审计写入与查询（详设 §1.7）；命令 / 类型定义 / 版本区段已接写入口。
 */
@Service
public class OperationLogAppService {

    public static final String BIZ_TABLE_ENTITY_COMMAND_MAPPING = "entity_command_mapping";
    public static final String BIZ_TABLE_COMMAND_TYPE_DEFINITION = "command_type_definition";
    public static final String BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE = "command_type_version_range";
    public static final String BIZ_TABLE_ENTITY_VERSION_INFO = "entity_version_info";
    public static final String BIZ_TABLE_ENTITY_BASIC_INFO = "entity_basic_info";
    public static final String BIZ_TABLE_SYSTEM_PARAMETER = "system_parameter";
    public static final String BIZ_TABLE_ENTITY_BUSINESS_CATEGORY = "entity_business_category";
    public static final String BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT = "entity_applicable_ne_dict";
    public static final String BIZ_TABLE_ENTITY_NF_CONFIG_DICT = "entity_nf_config_dict";
    public static final String BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT = "entity_effective_mode_dict";
    public static final String BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT = "entity_effective_form_dict";
    public static final String BIZ_TABLE_PROJECT_TEAM_DICT = "project_team_dict";
    public static final String BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD = "config_change_source_keyword";
    public static final String BIZ_TABLE_VERSION_FEATURE_DICT = "version_feature_dict";

    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";

    public static final String DELETE_OBJECT_LABEL = "删除对象";
    public static final String LABEL_COMMAND = "命令";
    public static final String LABEL_OWNER = "责任人(英文逗号)";

    public static final String T_LABEL_OWNED_CMD = "归属命令ID";
    public static final String T_LABEL_TYPE_NAME = "类型名称";
    public static final String T_LABEL_TYPE_ENUM = "类型枚举";
    public static final String T_LABEL_MIN = "最小序号";
    public static final String T_LABEL_MAX = "最大序号";
    public static final String T_LABEL_OCCUPIED = "占用序号";

    public static final String R_LABEL_OWNED_CMD = "归属命令ID";
    public static final String R_LABEL_TYPE_ID = "类型ID";
    public static final String R_LABEL_START = "起始序号";
    public static final String R_LABEL_END = "结束序号";
    public static final String R_LABEL_DESC = "说明";
    public static final String R_LABEL_RANGE_TYPE = "区段划分类型";
    public static final String R_LABEL_VER = "归属版本ID";

    public static final String V_LABEL_NAME = "版本名称";
    public static final String V_LABEL_TYPE = "版本类型";
    public static final String V_LABEL_SUPPORT = "支持版本";
    public static final String V_LABEL_DESC = "版本说明";
    public static final String V_LABEL_VDESC = "版本描述";
    public static final String V_LABEL_BASE_ID = "基线版本ID";
    public static final String V_LABEL_BASE_NAME = "基线版本名称";
    public static final String V_LABEL_OWNER = "责任人";
    public static final String V_LABEL_STATUS = "状态";

    /** 与产品配置页列表列名一致（不记页面未展示的字段如产品名、产品 ID、状态等） */
    public static final String EB_SOFT_TYPE = "参数类型";
    public static final String EB_FORM = "产品形态";
    public static final String EB_OWNERS = "责任人";

    public static final String P_LABEL_CODE = "参数编码";
    public static final String P_LABEL_NAME_CN = "参数名称（中文）";
    public static final String P_LABEL_NAME_EN = "参数名称（英文）";
    public static final String P_LABEL_CMD = "归属命令ID";
    public static final String P_LABEL_STATUS = "数据状态";
    public static final String P_LABEL_SEQ = "参数序号";
    public static final String P_LABEL_INTRO = "引入类型";
    public static final String P_LABEL_INHERIT = "继承/引用版本 ID";
    public static final String P_LABEL_VALUE_RANGE = "取值范围";
    public static final String P_LABEL_BIT_USAGE = "BIT 占用";
    public static final String P_LABEL_VALUE_DESC_CN = "取值说明（中文）";
    public static final String P_LABEL_VALUE_DESC_EN = "取值说明（英文）";
    public static final String P_LABEL_SCENE_CN = "应用场景（中文）";
    public static final String P_LABEL_SCENE_EN = "应用场景（英文）";
    public static final String P_LABEL_DEFAULT = "参数默认值";
    public static final String P_LABEL_RECOMMEND = "参数推荐值";
    public static final String P_LABEL_NE = "适用网元";
    public static final String P_LABEL_FEATURE = "所属特性";
    public static final String P_LABEL_FEATURE_ID = "所属特性 ID";
    public static final String P_LABEL_BIZ_CLASS = "业务分类";
    public static final String P_LABEL_CATEGORY_ID = "业务分类 ID";
    public static final String P_LABEL_TAKE_EFFECT = "立即生效";
    public static final String P_LABEL_EM_CN = "生效方式（中文）";
    public static final String P_LABEL_EM_EN = "生效方式（英文）";
    public static final String P_LABEL_EF_CN = "生效形态（中文）";
    public static final String P_LABEL_EF_EN = "生效形态（英文）";
    public static final String P_LABEL_TEAM = "项目组";
    public static final String P_LABEL_MODULE = "归属模块";
    public static final String P_LABEL_PATCH = "版本号";
    public static final String P_LABEL_INTRO_VER = "引入版本";
    public static final String P_LABEL_PARAM_DESC_CN = "参数含义（中文）";
    public static final String P_LABEL_PARAM_DESC_EN = "参数含义（英文）";
    public static final String P_LABEL_IMPACT_CN = "影响说明（中文）";
    public static final String P_LABEL_IMPACT_EN = "影响说明（英文）";
    public static final String P_LABEL_EXAMPLE_CN = "配置举例（中文）";
    public static final String P_LABEL_EXAMPLE_EN = "配置举例（英文）";
    public static final String P_LABEL_RELATED_DESC_CN = "关联参数描述（中文）";
    public static final String P_LABEL_RELATED_DESC_EN = "关联参数描述（英文）";
    public static final String P_LABEL_REMARK = "备注";
    public static final String P_LABEL_ENUM_CN = "枚举值（中文）";
    public static final String P_LABEL_ENUM_EN = "枚举值（英文）";
    public static final String P_LABEL_UNIT_CN = "参数单位（中文）";
    public static final String P_LABEL_UNIT_EN = "参数单位（英文）";
    public static final String P_LABEL_PARAM_RANGE = "parameter_range";

    public static final String BC_CN = "分类名称（中文）";
    public static final String BC_EN = "分类名称（英文）";
    public static final String BC_RANGE = "包含特性范围";
    public static final String BC_TYPE = "所属类别";

    public static final String NE_NAME = "适用网元名称";
    public static final String NE_DESC = "网元类型描述";
    public static final String NE_FORM = "产品形态";

    public static final String NF_NAME = "nf 名称";
    public static final String NF_DESC = "nf 配置描述";

    public static final String EM_CN = "生效方式（中文）";
    public static final String EM_EN = "生效方式（英文）";
    public static final String EM_DESC = "生效方式描述";

    public static final String EF_CN = "生效形态（中文）";
    public static final String EF_EN = "生效形态（英文）";
    public static final String EF_DESC = "生效形态描述";

    public static final String PT_NAME = "项目组名称";
    public static final String PT_DESC = "项目组描述";
    public static final String PT_OWNER = "责任人";

    public static final String KW_REGEX = "关键字正则";
    public static final String KW_REASON = "原因";

    public static final String VF_CN = "中文名称";
    public static final String VF_EN = "英文名称";
    public static final String VF_INTRO = "引入类型";
    public static final String VF_INHERIT = "继承/引用版本 ID";

    private static final ThreadLocal<String> IMPORT_LOG_BATCH = new ThreadLocal<>();
    /** 当前线程写操作日志时的「操作对象名称」（避免前端再做 resourceId->name 映射）。 */
    private static final ThreadLocal<String> RESOURCE_NAME = new ThreadLocal<>();

    private final OperationLogMapper operationLogMapper;

    /**
     * 构造应用服务。
     *
     * @param operationLogMapper 操作日志持久化 Mapper
     */
    public OperationLogAppService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /** 导入用：同一次请求内多行写审计共享同一 log_batch_id（详设 §1.7.4.1 可选） */
    public void beginImportBatch() {
        IMPORT_LOG_BATCH.set(IdGenerator.operationLogId());
    }

    /**
     * 结束一次导入批次，清理当前线程的批次标识。
     */
    public void endImportBatch() {
        IMPORT_LOG_BATCH.remove();
    }

    /**
     * 批量写入操作日志。
     *
     * @param rows 待写入的日志行
     */
    public void insertAll(List<OperationLogPo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (OperationLogPo row : rows) {
            if (row.getLogId() == null) {
                row.setLogId(IdGenerator.operationLogId());
            }
            operationLogMapper.insert(row);
        }
    }

    /**
     * 记录命令创建的审计日志（支持外部传入批次号）。
     *
     * @param productId  归属产品 ID
     * @param result     创建后的命令映射行
     * @param operatorId 操作人 ID（为空时按 system 记）
     * @param logBatchId 批次号（为空时自动取当前线程批次）
     */
    public void logCommandCreate(
            String productId, EntityCommandMappingPo result, String operatorId, String logBatchId) {
        if (result == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = firstNonBlank(logBatchId, IMPORT_LOG_BATCH.get());
        RESOURCE_NAME.set(StringUtils.trimToNull(result.getCommandName()));
        List<OperationLogPo> rows = new ArrayList<>();
        if (StringUtils.isNotBlank(result.getCommandName())) {
            rows.add(
                    line(new LogLineSpec(BIZ_TABLE_ENTITY_COMMAND_MAPPING, productId, null, result.getCommandId(), OP_CREATE, LABEL_COMMAND, null, result.getCommandName(), op, now, batch, null)));
        }
        if (StringUtils.isNotBlank(result.getOwnerList())) {
            rows.add(
                    line(new LogLineSpec(BIZ_TABLE_ENTITY_COMMAND_MAPPING, productId, null, result.getCommandId(), OP_CREATE, LABEL_OWNER, null, result.getOwnerList(), op, now, batch, null)));
        }
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录命令创建的审计日志（批次号自动取当前线程批次）。
     *
     * @param productId  归属产品 ID
     * @param result     创建后的命令映射行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logCommandCreate(String productId, EntityCommandMappingPo result, String operatorId) {
        logCommandCreate(productId, result, operatorId, null);
    }

    /**
     * 记录命令更新的审计日志（批次号自动取当前线程批次）。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logCommandUpdate(Command before, Command after, String operatorId) {
        logCommandUpdate(before, after, operatorId, null);
    }

    /**
     * 记录命令更新的审计日志（支持外部传入批次号）。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     * @param logBatchId 批次号（为空时自动取当前线程批次）
     */
    public void logCommandUpdate(
            Command before, Command after, String operatorId, String logBatchId) {
        if (before == null || after == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = firstNonBlank(logBatchId, IMPORT_LOG_BATCH.get());
        RESOURCE_NAME.set(StringUtils.trimToNull(after.getCommandName()));
        List<OperationLogPo> rows = new ArrayList<>();
        if (!Objects.equals(
                nullToEmpty(before.getCommandName()), nullToEmpty(after.getCommandName()))) {
            rows.add(
                    line(new LogLineSpec(BIZ_TABLE_ENTITY_COMMAND_MAPPING, after.getOwnedProductId(), null, after.getCommandId(), OP_UPDATE, LABEL_COMMAND, nullToEmpty(before.getCommandName()), nullToEmpty(after.getCommandName()), op, now, batch, null)));
        }
        if (!Objects.equals(
                nullToEmpty(before.getOwnerList()), nullToEmpty(after.getOwnerList()))) {
            rows.add(
                    line(new LogLineSpec(BIZ_TABLE_ENTITY_COMMAND_MAPPING, after.getOwnedProductId(), null, after.getCommandId(), OP_UPDATE, LABEL_OWNER, nullToEmpty(before.getOwnerList()), nullToEmpty(after.getOwnerList()), op, now, batch, null)));
        }
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录命令删除的审计日志（批次号自动取当前线程批次）。
     *
     * @param productId   归属产品 ID
     * @param commandId   命令 ID
     * @param displayName 展示名（用于“删除对象”字段）
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logCommandDelete(
            String productId, String commandId, String displayName, String operatorId) {
        logCommandDelete(productId, commandId, displayName, operatorId, null);
    }

    /**
     * 记录命令删除的审计日志（支持外部传入批次号）。
     *
     * @param productId   归属产品 ID
     * @param commandId   命令 ID
     * @param displayName 展示名（用于“删除对象”字段）
     * @param operatorId  操作人 ID（为空时按 system 记）
     * @param logBatchId  批次号（为空时自动取当前线程批次）
     */
    public void logCommandDelete(
            String productId,
            String commandId,
            String displayName,
            String operatorId,
            String logBatchId) {
        if (StringUtils.isBlank(commandId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = firstNonBlank(logBatchId, IMPORT_LOG_BATCH.get());
        String name = StringUtils.defaultString(displayName);
        RESOURCE_NAME.set(StringUtils.trimToNull(name));
        insertAll(
                List.of(
                        line(new LogLineSpec(BIZ_TABLE_ENTITY_COMMAND_MAPPING, productId, null, commandId, OP_DELETE, DELETE_OBJECT_LABEL, null, name, op, now, batch, null))));
        RESOURCE_NAME.remove();
    }

    /**
     * 记录命令类型定义创建的审计日志。
     *
     * @param productId  归属产品 ID
     * @param result     创建后的类型定义行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logTypeDefinitionCreate(
            String productId, CommandTypeDefinitionPo result, String operatorId) {
        if (result == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        String rid = result.getCommandTypeId();
        RESOURCE_NAME.set(StringUtils.trimToNull(result.getCommandTypeName()));
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_OWNED_CMD, result.getOwnedCommandId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_TYPE_NAME, result.getCommandTypeName());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_TYPE_ENUM, result.getCommandType());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_MIN, intStr(result.getMinValue()));
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_MAX, intStr(result.getMaxValue()));
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_OCCUPIED, result.getOccupiedSerialNumber());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录命令类型定义更新的审计日志。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logTypeDefinitionUpdate(
            CommandTypeDefinition before, CommandTypeDefinition after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        String rid = after.getCommandTypeId();
        String productId = after.getOwnedProductId();
        RESOURCE_NAME.set(StringUtils.trimToNull(after.getCommandTypeName()));
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_OWNED_CMD, before.getOwnedCommandId(), after.getOwnedCommandId());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_TYPE_NAME, before.getCommandTypeName(), after.getCommandTypeName());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_TYPE_ENUM, before.getCommandType(), after.getCommandType());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_MIN, before.getMinValue(), after.getMinValue());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_MAX, before.getMaxValue(), after.getMaxValue());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_DEFINITION, productId, null, rid, op, now, batch), T_LABEL_OCCUPIED, before.getOccupiedSerialNumber(), after.getOccupiedSerialNumber());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录类型版本区段创建的审计日志。
     *
     * @param productId  归属产品 ID
     * @param result     创建后的区段行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logRangeCreate(
            String productId, CommandTypeVersionRangePo result, String operatorId) {
        if (result == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        String rid = result.getVersionRangeId();
        RESOURCE_NAME.set(StringUtils.trimToNull(result.getRangeDescription()));
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_OWNED_CMD, result.getOwnedCommandId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_TYPE_ID, result.getOwnedTypeId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_START, intStr(result.getStartIndex()));
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_END, intStr(result.getEndIndex()));
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_DESC, result.getRangeDescription());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_RANGE_TYPE, result.getRangeType());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_VER, result.getOwnedVersionOrBusinessId());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录类型版本区段更新的审计日志。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logRangeUpdate(
            CommandTypeVersionRange before, CommandTypeVersionRange after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        String rid = after.getVersionRangeId();
        String productId = after.getOwnedProductId();
        RESOURCE_NAME.set(StringUtils.trimToNull(after.getRangeDescription()));
        List<OperationLogPo> rows = new ArrayList<>();
        diffInt(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_START, before.getStartIndex(), after.getStartIndex());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_END, before.getEndIndex(), after.getEndIndex());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_DESC, before.getRangeDescription(), after.getRangeDescription());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_RANGE_TYPE, before.getRangeType(), after.getRangeType());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, rid, op, now, batch), R_LABEL_VER, before.getOwnedVersionOrBusinessId(), after.getOwnedVersionOrBusinessId());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录类型版本区段删除的审计日志。
     *
     * @param productId      归属产品 ID
     * @param versionRangeId 区段 ID
     * @param displayName    展示名（用于“删除对象”字段）
     * @param operatorId     操作人 ID（为空时按 system 记）
     */
    public void logRangeDelete(
            String productId, String versionRangeId, String displayName, String operatorId) {
        if (StringUtils.isBlank(versionRangeId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        RESOURCE_NAME.set(StringUtils.trimToNull(displayName));
        insertAll(
                List.of(
                        line(new LogLineSpec(BIZ_TABLE_COMMAND_TYPE_VERSION_RANGE, productId, null, versionRangeId, OP_DELETE, DELETE_OBJECT_LABEL, null, StringUtils.defaultString(displayName), op, now, batch, null))));
        RESOURCE_NAME.remove();
    }

    // ---------- 配置主数据 / 产品 / 版本 / 参数（详设 §1.7；版本行 owned_version_id = version_id） ----------

    /**
     * 记录版本创建的审计日志。
     *
     * @param productId  产品 ID
     * @param p          创建后的版本行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logVersionCreate(String productId, EntityVersionInfoPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getVersionId())) {
            return;
        }
        String vid = p.getVersionId();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        RESOURCE_NAME.set(StringUtils.trimToNull(p.getVersionName()));
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_NAME, p.getVersionName());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_TYPE, p.getVersionType());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_SUPPORT, p.getSupportedVersion());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_DESC, p.getVersionDescription());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_VDESC, p.getVersionDesc());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_BASE_ID, p.getBaselineVersionId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_BASE_NAME, p.getBaselineVersionName());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_OWNER, p.getOwnerList());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录版本更新的审计日志。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logVersionUpdate(ProductVersion before, ProductVersion after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String vid = after.getVersionId();
        RESOURCE_NAME.set(StringUtils.trimToNull(after.getVersionName()));
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_NAME, before.getVersionName(), after.getVersionName());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_TYPE, before.getVersionType(), after.getVersionType());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_SUPPORT, before.getSupportedVersion(), after.getSupportedVersion());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_DESC, before.getVersionDescription(), after.getVersionDescription());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_VDESC, before.getVersionDesc(), after.getVersionDesc());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_BASE_ID, before.getBaselineVersionId(), after.getBaselineVersionId());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_BASE_NAME, before.getBaselineVersionName(), after.getBaselineVersionName());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_OWNER, before.getOwnerList(), after.getOwnerList());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_ENTITY_VERSION_INFO, productId, vid, vid, op, now, batch), V_LABEL_STATUS, before.getVersionStatus(), after.getVersionStatus());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录版本删除的审计日志。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param versionName 版本名称（用于“删除对象”字段）
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logVersionDelete(String productId, String versionId, String versionName, String operatorId) {
        if (StringUtils.isBlank(versionId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        RESOURCE_NAME.set(StringUtils.trimToNull(versionName));
        insertAll(
                List.of(
                        line(new LogLineSpec(BIZ_TABLE_ENTITY_VERSION_INFO, productId, versionId, versionId, OP_DELETE, DELETE_OBJECT_LABEL, null, StringUtils.defaultString(versionName), op, now, batch, null))));
        RESOURCE_NAME.remove();
    }

    /**
     * 记录产品主数据创建的审计日志（产品形态维度）。
     *
     * @param scopeProductId 产品 ID（审计作用域）
     * @param p              创建后的主数据行
     * @param operatorId     操作人 ID（为空时按 system 记）
     */
    public void logEntityBasicInfoCreate(String scopeProductId, EntityBasicInfoPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getProductFormId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getProductFormId();
        RESOURCE_NAME.set(StringUtils.trimToNull(p.getProductForm()));
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BASIC_INFO, scopeProductId, null, rid, op, now, batch), EB_SOFT_TYPE, p.getProductSoftParamType());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BASIC_INFO, scopeProductId, null, rid, op, now, batch), EB_FORM, p.getProductForm());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BASIC_INFO, scopeProductId, null, rid, op, now, batch), EB_OWNERS, p.getOwnerList());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录产品主数据更新的审计日志（产品形态维度）。
     *
     * @param before     更新前行
     * @param after      更新后行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logEntityBasicInfoUpdate(EntityBasicInfoPo before, EntityBasicInfoPo after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String scope = after.getProductId();
        String rid = after.getProductFormId();
        RESOURCE_NAME.set(StringUtils.trimToNull(after.getProductForm()));
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BASIC_INFO, scope, null, rid, op, now, batch), EB_SOFT_TYPE, before.getProductSoftParamType(), after.getProductSoftParamType());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BASIC_INFO, scope, null, rid, op, now, batch), EB_FORM, before.getProductForm(), after.getProductForm());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BASIC_INFO, scope, null, rid, op, now, batch), EB_OWNERS, before.getOwnerList(), after.getOwnerList());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录产品主数据删除的审计日志（产品形态维度）。
     *
     * @param row        待删除行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logEntityBasicInfoDelete(EntityBasicInfoPo row, String operatorId) {
        if (row == null || StringUtils.isBlank(row.getProductFormId())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        String scope = row.getProductId();
        String label = StringUtils.defaultIfBlank(row.getProductForm(), row.getProductFormId());
        RESOURCE_NAME.set(StringUtils.trimToNull(label));
        insertAll(
                List.of(
                        line(new LogLineSpec(BIZ_TABLE_ENTITY_BASIC_INFO, scope, null, row.getProductFormId(), OP_DELETE, DELETE_OBJECT_LABEL, null, label, op, now, batch, null))));
        RESOURCE_NAME.remove();
    }

    /**
     * 记录系统参数创建的审计日志。
     *
     * @param p          创建后的参数行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logSystemParameterCreate(SystemParameterPo p, String operatorId) {
        if (p == null || p.getParameterId() == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = p.getOwnedProductId();
        String versionId = p.getOwnedVersionId();
        String rid = String.valueOf(p.getParameterId());
        RESOURCE_NAME.set(StringUtils.trimToNull(p.getParameterCode()));
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_CODE, p.getParameterCode());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_NAME_CN, p.getParameterNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_NAME_EN, p.getParameterNameEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_CMD, p.getOwnedCommandId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_STATUS, p.getDataStatus());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_SEQ, intStr(p.getParameterSequence()));
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_INTRO, p.getIntroduceType());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_INHERIT, p.getInheritReferenceVersionId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_VALUE_RANGE, p.getValueRange());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_BIT_USAGE, p.getBitUsage());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_VALUE_DESC_CN, p.getValueDescriptionCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_VALUE_DESC_EN, p.getValueDescriptionEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_SCENE_CN, p.getApplicationScenarioCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_SCENE_EN, p.getApplicationScenarioEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_DEFAULT, p.getParameterDefaultValue());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_RECOMMEND, p.getParameterRecommendedValue());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_NE, p.getApplicableNe());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_FEATURE, p.getFeature());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_FEATURE_ID, p.getFeatureId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_BIZ_CLASS, p.getBusinessClassification());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_CATEGORY_ID, p.getCategoryId());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_TAKE_EFFECT, p.getTakeEffectImmediately());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EM_CN, p.getEffectiveModeCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EM_EN, p.getEffectiveModeEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EF_CN, p.getEffectiveFormCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EF_EN, p.getEffectiveFormEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_TEAM, p.getProjectTeam());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_MODULE, p.getBelongingModule());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PATCH, p.getPatchVersion());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_INTRO_VER, p.getIntroducedVersion());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PARAM_DESC_CN, p.getParameterDescriptionCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PARAM_DESC_EN, p.getParameterDescriptionEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_IMPACT_CN, p.getImpactDescriptionCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_IMPACT_EN, p.getImpactDescriptionEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EXAMPLE_CN, p.getConfigurationExampleCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EXAMPLE_EN, p.getConfigurationExampleEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_RELATED_DESC_CN, p.getRelatedParameterDescriptionCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_RELATED_DESC_EN, p.getRelatedParameterDescriptionEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_REMARK, p.getRemark());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_ENUM_CN, p.getEnumerationValuesCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_ENUM_EN, p.getEnumerationValuesEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_UNIT_CN, p.getParameterUnitCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_UNIT_EN, p.getParameterUnitEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PARAM_RANGE, p.getParameterRange());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录系统参数更新的审计日志。
     *
     * @param before     更新前行
     * @param after      更新后行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logSystemParameterUpdate(SystemParameterPo before, SystemParameterPo after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String versionId = after.getOwnedVersionId();
        String rid = String.valueOf(after.getParameterId());
        RESOURCE_NAME.set(StringUtils.trimToNull(after.getParameterCode()));
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_CODE, before.getParameterCode(), after.getParameterCode());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_NAME_CN, before.getParameterNameCn(), after.getParameterNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_NAME_EN, before.getParameterNameEn(), after.getParameterNameEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_CMD, before.getOwnedCommandId(), after.getOwnedCommandId());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_STATUS, before.getDataStatus(), after.getDataStatus());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_SEQ, before.getParameterSequence(), after.getParameterSequence());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_INTRO, before.getIntroduceType(), after.getIntroduceType());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_INHERIT, before.getInheritReferenceVersionId(), after.getInheritReferenceVersionId());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_VALUE_RANGE, before.getValueRange(), after.getValueRange());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_BIT_USAGE, before.getBitUsage(), after.getBitUsage());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_VALUE_DESC_CN, before.getValueDescriptionCn(), after.getValueDescriptionCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_VALUE_DESC_EN, before.getValueDescriptionEn(), after.getValueDescriptionEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_SCENE_CN, before.getApplicationScenarioCn(), after.getApplicationScenarioCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_SCENE_EN, before.getApplicationScenarioEn(), after.getApplicationScenarioEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_DEFAULT, before.getParameterDefaultValue(), after.getParameterDefaultValue());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_RECOMMEND, before.getParameterRecommendedValue(), after.getParameterRecommendedValue());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_NE, before.getApplicableNe(), after.getApplicableNe());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_FEATURE, before.getFeature(), after.getFeature());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_FEATURE_ID, before.getFeatureId(), after.getFeatureId());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_BIZ_CLASS, before.getBusinessClassification(), after.getBusinessClassification());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_CATEGORY_ID, before.getCategoryId(), after.getCategoryId());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_TAKE_EFFECT, before.getTakeEffectImmediately(), after.getTakeEffectImmediately());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EM_CN, before.getEffectiveModeCn(), after.getEffectiveModeCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EM_EN, before.getEffectiveModeEn(), after.getEffectiveModeEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EF_CN, before.getEffectiveFormCn(), after.getEffectiveFormCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EF_EN, before.getEffectiveFormEn(), after.getEffectiveFormEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_TEAM, before.getProjectTeam(), after.getProjectTeam());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_MODULE, before.getBelongingModule(), after.getBelongingModule());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PATCH, before.getPatchVersion(), after.getPatchVersion());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_INTRO_VER, before.getIntroducedVersion(), after.getIntroducedVersion());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PARAM_DESC_CN, before.getParameterDescriptionCn(), after.getParameterDescriptionCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PARAM_DESC_EN, before.getParameterDescriptionEn(), after.getParameterDescriptionEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_IMPACT_CN, before.getImpactDescriptionCn(), after.getImpactDescriptionCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_IMPACT_EN, before.getImpactDescriptionEn(), after.getImpactDescriptionEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EXAMPLE_CN, before.getConfigurationExampleCn(), after.getConfigurationExampleCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_EXAMPLE_EN, before.getConfigurationExampleEn(), after.getConfigurationExampleEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_RELATED_DESC_CN, before.getRelatedParameterDescriptionCn(), after.getRelatedParameterDescriptionCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_RELATED_DESC_EN, before.getRelatedParameterDescriptionEn(), after.getRelatedParameterDescriptionEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_REMARK, before.getRemark(), after.getRemark());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_ENUM_CN, before.getEnumerationValuesCn(), after.getEnumerationValuesCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_ENUM_EN, before.getEnumerationValuesEn(), after.getEnumerationValuesEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_UNIT_CN, before.getParameterUnitCn(), after.getParameterUnitCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_UNIT_EN, before.getParameterUnitEn(), after.getParameterUnitEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, op, now, batch), P_LABEL_PARAM_RANGE, before.getParameterRange(), after.getParameterRange());
        try {
            insertAll(rows);
        } finally {
            RESOURCE_NAME.remove();
        }
    }

    /**
     * 记录系统参数删除的审计日志。
     *
     * @param p          待删除参数行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logSystemParameterDelete(SystemParameterPo p, String operatorId) {
        if (p == null || p.getParameterId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        String productId = p.getOwnedProductId();
        String versionId = p.getOwnedVersionId();
        String rid = String.valueOf(p.getParameterId());
        String label = StringUtils.defaultIfBlank(p.getParameterNameCn(), rid);
        RESOURCE_NAME.set(StringUtils.trimToNull(p.getParameterCode()));
        insertAll(
                List.of(
                        line(new LogLineSpec(BIZ_TABLE_SYSTEM_PARAMETER, productId, versionId, rid, OP_DELETE, DELETE_OBJECT_LABEL, null, label, op, now, batch, null))));
        RESOURCE_NAME.remove();
    }

    /**
     * 记录业务分类创建的审计日志。
     *
     * @param productId  产品 ID
     * @param p          创建后的业务分类行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logBusinessCategoryCreate(String productId, EntityBusinessCategoryPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getCategoryId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getCategoryId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_CN, p.getCategoryNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_EN, p.getCategoryNameEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_RANGE, p.getFeatureRange());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_TYPE, p.getCategoryType());
        insertAll(rows);
    }

    /**
     * 记录业务分类更新的审计日志。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logBusinessCategoryUpdate(BusinessCategory before, BusinessCategory after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getCategoryId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_CN, before.getCategoryNameCn(), after.getCategoryNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_EN, before.getCategoryNameEn(), after.getCategoryNameEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_RANGE, before.getFeatureRange(), after.getFeatureRange());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), BC_TYPE, before.getCategoryType(), after.getCategoryType());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getCategoryStatus(), after.getCategoryStatus());
        insertAll(rows);
    }

    /**
     * 记录业务分类删除的审计日志。
     *
     * @param productId  产品 ID
     * @param categoryId 分类 ID
     * @param display    展示名（用于“删除对象”字段）
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logBusinessCategoryDelete(String productId, String categoryId, String display, String operatorId) {
        if (StringUtils.isBlank(categoryId)) {
            return;
        }
        deleteOne(
                new LogDictRowDeleteInput(
                        BIZ_TABLE_ENTITY_BUSINESS_CATEGORY, productId, null, categoryId, display, operatorId));
    }

    /**
     * 记录适用网元创建的审计日志。
     *
     * @param productId  产品 ID
     * @param p          创建后的网元行
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logApplicableNeCreate(String productId, EntityApplicableNeDictPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getNeTypeId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getNeTypeId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), NE_NAME, p.getNeTypeNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), NE_DESC, p.getNeTypeDescription());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), NE_FORM, p.getProductForm());
        insertAll(rows);
    }

    /**
     * 记录适用网元更新的审计日志。
     *
     * @param before     更新前领域对象
     * @param after      更新后领域对象
     * @param operatorId 操作人 ID（为空时按 system 记）
     */
    public void logApplicableNeUpdate(ApplicableNe before, ApplicableNe after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getNeTypeId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), NE_NAME, before.getNeTypeNameCn(), after.getNeTypeNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), NE_DESC, before.getNeTypeDescription(), after.getNeTypeDescription());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), NE_FORM, before.getProductForm(), after.getProductForm());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getNeTypeStatus(), after.getNeTypeStatus());
        insertAll(rows);
    }

    /**
     * 记录 NF 配置字典创建的审计日志。
     *
     * @param productId   产品 ID
     * @param p           新增行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logNfConfigCreate(String productId, EntityNfConfigDictPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getNfConfigId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getNfConfigId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_NF_CONFIG_DICT, productId, null, rid, op, now, batch), NF_NAME, p.getNfConfigNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_NF_CONFIG_DICT, productId, null, rid, op, now, batch), NF_DESC, p.getNfConfigDescription());
        insertAll(rows);
    }

    /**
     * 记录 NF 配置字典更新的审计日志。
     *
     * @param before      更新前行
     * @param after       更新后行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logNfConfigUpdate(NfConfigEntry before, NfConfigEntry after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getNfConfigId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_NF_CONFIG_DICT, productId, null, rid, op, now, batch), NF_NAME, before.getNfConfigNameCn(), after.getNfConfigNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_NF_CONFIG_DICT, productId, null, rid, op, now, batch), NF_DESC, before.getNfConfigDescription(), after.getNfConfigDescription());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_ENTITY_NF_CONFIG_DICT, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getNfConfigStatus(), after.getNfConfigStatus());
        insertAll(rows);
    }

    /**
     * 记录生效方式字典创建的审计日志。
     *
     * @param productId   产品 ID
     * @param p           新增行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logEffectiveModeCreate(String productId, EntityEffectiveModeDictPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getEffectiveModeId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getEffectiveModeId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), EM_CN, p.getEffectiveModeNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), EM_EN, p.getEffectiveModeNameEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), EM_DESC, p.getEffectiveModeDescription());
        insertAll(rows);
    }

    /**
     * 记录生效方式字典更新的审计日志。
     *
     * @param before      更新前行
     * @param after       更新后行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logEffectiveModeUpdate(EffectiveMode before, EffectiveMode after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getEffectiveModeId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), EM_CN, before.getEffectiveModeNameCn(), after.getEffectiveModeNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), EM_EN, before.getEffectiveModeNameEn(), after.getEffectiveModeNameEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), EM_DESC, before.getEffectiveModeDescription(), after.getEffectiveModeDescription());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getEffectiveModeStatus(), after.getEffectiveModeStatus());
        insertAll(rows);
    }

    /**
     * 记录生效形式字典创建的审计日志。
     *
     * @param productId   产品 ID
     * @param p           新增行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logEffectiveFormCreate(String productId, EntityEffectiveFormDictPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getEffectiveFormId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getEffectiveFormId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), EF_CN, p.getEffectiveFormNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), EF_EN, p.getEffectiveFormNameEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), EF_DESC, p.getEffectiveFormDescription());
        insertAll(rows);
    }

    /**
     * 记录生效形式字典更新的审计日志。
     *
     * @param before      更新前行
     * @param after       更新后行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logEffectiveFormUpdate(EffectiveForm before, EffectiveForm after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getEffectiveFormId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), EF_CN, before.getEffectiveFormNameCn(), after.getEffectiveFormNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), EF_EN, before.getEffectiveFormNameEn(), after.getEffectiveFormNameEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), EF_DESC, before.getEffectiveFormDescription(), after.getEffectiveFormDescription());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getEffectiveFormStatus(), after.getEffectiveFormStatus());
        insertAll(rows);
    }

    /**
     * 记录项目组字典创建的审计日志。
     *
     * @param productId   产品 ID
     * @param p           新增行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logProjectTeamCreate(String productId, ProjectTeamDictPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getTeamId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getTeamId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), PT_NAME, p.getTeamName());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), PT_DESC, p.getTeamDescription());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), PT_OWNER, p.getOwnerList());
        insertAll(rows);
    }

    /**
     * 记录项目组字典更新的审计日志。
     *
     * @param before      更新前行
     * @param after       更新后行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logProjectTeamUpdate(ProjectTeam before, ProjectTeam after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getTeamId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), PT_NAME, before.getTeamName(), after.getTeamName());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), PT_DESC, before.getTeamDescription(), after.getTeamDescription());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), PT_OWNER, before.getOwnerList(), after.getOwnerList());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_PROJECT_TEAM_DICT, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getTeamStatus(), after.getTeamStatus());
        insertAll(rows);
    }

    /**
     * 记录变更来源关键字字典创建的审计日志。
     *
     * @param productId   产品 ID
     * @param p           新增行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logChangeKeywordCreate(String productId, ConfigChangeSourceKeywordPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getKeywordId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getKeywordId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD, productId, null, rid, op, now, batch), KW_REGEX, p.getKeywordRegex());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD, productId, null, rid, op, now, batch), KW_REASON, p.getReason());
        insertAll(rows);
    }

    /**
     * 记录变更来源关键字字典更新的审计日志。
     *
     * @param before      更新前行
     * @param after       更新后行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logChangeKeywordUpdate(ChangeSourceKeyword before, ChangeSourceKeyword after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductId();
        String rid = after.getKeywordId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD, productId, null, rid, op, now, batch), KW_REGEX, before.getKeywordRegex(), after.getKeywordRegex());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD, productId, null, rid, op, now, batch), KW_REASON, before.getReason(), after.getReason());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD, productId, null, rid, op, now, batch), V_LABEL_STATUS, before.getKeywordStatus(), after.getKeywordStatus());
        insertAll(rows);
    }

    /**
     * 记录版本特性字典创建的审计日志。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param p           新增行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logVersionFeatureCreate(String productId, String versionId, VersionFeatureDictPo p, String operatorId) {
        if (p == null || StringUtils.isBlank(p.getFeatureId())) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String rid = p.getFeatureId();
        List<OperationLogPo> rows = new ArrayList<>();
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_CN, p.getFeatureNameCn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_EN, p.getFeatureNameEn());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_INTRO, p.getIntroduceType());
        addCreateStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_INHERIT, p.getInheritReferenceVersionId());
        insertAll(rows);
    }

    /**
     * 记录版本特性字典更新的审计日志。
     *
     * @param before      更新前行
     * @param after       更新后行
     * @param operatorId  操作人 ID（为空时按 system 记）
     */
    public void logVersionFeatureUpdate(VersionFeature before, VersionFeature after, String operatorId) {
        if (before == null || after == null) {
            return;
        }
        String op = StringUtils.defaultIfBlank(operatorId, "system");
        String batch = IMPORT_LOG_BATCH.get();
        LocalDateTime now = LocalDateTime.now();
        String productId = after.getOwnedProductPbiId();
        String versionId = after.getOwnedVersionId();
        String rid = after.getFeatureId();
        List<OperationLogPo> rows = new ArrayList<>();
        diffStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_CN, before.getFeatureNameCn(), after.getFeatureNameCn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_EN, before.getFeatureNameEn(), after.getFeatureNameEn());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_INTRO, before.getIntroduceType(), after.getIntroduceType());
        diffStr(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), VF_INHERIT, before.getInheritReferenceVersionId(), after.getInheritReferenceVersionId());
        diffInt(rows, new LogSessionContext(BIZ_TABLE_VERSION_FEATURE_DICT, productId, versionId, rid, op, now, batch), V_LABEL_STATUS, before.getFeatureStatus(), after.getFeatureStatus());
        insertAll(rows);
    }

    /**
     * 记录通用字典行删除的审计。
     *
     * @param in 删除入参，字段见 LogDictRowDeleteInput
     */
    public void logDictRowDelete(LogDictRowDeleteInput in) {
        deleteOne(in);
    }

    private void deleteOne(LogDictRowDeleteInput d) {
        if (StringUtils.isBlank(d.resourceId())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String op = StringUtils.defaultIfBlank(d.operatorId(), "system");
        String batch = IMPORT_LOG_BATCH.get();
        RESOURCE_NAME.set(StringUtils.trimToNull(d.display()));
        insertAll(
                List.of(
                        line(
                                new LogLineSpec(
                                        d.bizTable(),
                                        d.productId(),
                                        d.versionId(),
                                        d.resourceId(),
                                        OP_DELETE,
                                        DELETE_OBJECT_LABEL,
                                        null,
                                        StringUtils.defaultString(d.display()),
                                        op,
                                        now,
                                        batch,
                                        null))));
        RESOURCE_NAME.remove();
    }

    /**
     * 分页查询操作日志（不分组）。
     *
     * @param query 分页与业务过滤，字段见 OperationLogPageQuery
     * @return 分页结果
     */
    public PageResponse<OperationLogPo> page(OperationLogPageQuery query) {
        Page<OperationLogPo> p = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<OperationLogPo> w =
                new LambdaQueryWrapper<OperationLogPo>()
                        .eq(OperationLogPo::getOwnedProductId, query.getProductId())
                        .eq(OperationLogPo::getBizTable, query.getBizTable());
        if (StringUtils.isNotBlank(query.getVersionId())) {
            w.eq(OperationLogPo::getOwnedVersionId, query.getVersionId());
        } else if (!query.isIgnoreVersionFilter()) {
            w.isNull(OperationLogPo::getOwnedVersionId);
        }
        if (StringUtils.isNotBlank(query.getResourceId())) {
            w.eq(OperationLogPo::getResourceId, query.getResourceId());
        }
        if (query.getOperatedFrom() != null) {
            w.ge(OperationLogPo::getOperatedAt, query.getOperatedFrom());
        }
        if (query.getOperatedTo() != null) {
            w.le(OperationLogPo::getOperatedAt, query.getOperatedTo());
        }
        if (StringUtils.isNotBlank(query.getSort())
                && "operatedAt,asc".equalsIgnoreCase(StringUtils.deleteWhitespace(query.getSort()))) {
            w.orderByAsc(OperationLogPo::getOperatedAt);
        } else {
            w.orderByDesc(OperationLogPo::getOperatedAt);
        }
        Page<OperationLogPo> out = operationLogMapper.selectPage(p, w);
        PageResponse<OperationLogPo> resp = new PageResponse<>();
        resp.setRecords(out.getRecords());
        resp.setTotal(out.getTotal());
        resp.setPage((int) out.getCurrent());
        resp.setSize((int) out.getSize());
        return resp;
    }

    /**
     * 分组分页：total 按“折叠组”统计；每组 items 为字段行列表。
     *
     * @param query 分组分页与业务过滤，字段见 OperationLogGroupPageQuery
     * @return 分页结果（按组统计）
     */
    public PageResponse<OperationLogGroupItem> pageGroups(OperationLogGroupPageQuery query) {
        int safePage = Math.max(1, query.getPage());
        int safeSize = Math.max(1, query.getSize());
        OperationLogGroupSelectQuery totalQ = toSelectQuery(query, 0, 0);
        long totalGroups = operationLogMapper.countGroups(totalQ);
        long offset = (long) (safePage - 1) * safeSize;
        OperationLogGroupSelectQuery pageQ = toSelectQuery(query, offset, safeSize);
        List<OperationLogGroupItem> groups = operationLogMapper.selectGroupPage(pageQ);
        for (OperationLogGroupItem g : groups) {
            OperationLogGroupKey k = new OperationLogGroupKey();
            k.setProductId(query.getProductId());
            k.setBizTable(query.getBizTable());
            k.setOwnedVersionId(g.getOwnedVersionId());
            k.setResourceId(g.getResourceId());
            k.setOperationType(g.getOperationType());
            k.setOperatorId(g.getOperatorId());
            k.setOperatedAt(g.getOperatedAt());
            k.setLogBatchId(g.getLogBatchId());
            List<OperationLogGroupLine> items = operationLogMapper.selectLinesForGroup(k);
            g.setItems(items);
        }
        PageResponse<OperationLogGroupItem> resp = new PageResponse<>();
        resp.setRecords(groups);
        resp.setTotal(totalGroups);
        resp.setPage(safePage);
        resp.setSize(safeSize);
        return resp;
    }

    private static OperationLogGroupSelectQuery toSelectQuery(OperationLogGroupPageQuery src, long offset, long size) {
        OperationLogGroupSelectQuery q = new OperationLogGroupSelectQuery();
        q.setProductId(src.getProductId());
        q.setBizTable(src.getBizTable());
        q.setVersionId(src.getVersionId());
        q.setIgnoreVersionFilter(src.isIgnoreVersionFilter());
        q.setResourceId(src.getResourceId());
        q.setOperatedFrom(src.getOperatedFrom());
        q.setOperatedTo(src.getOperatedTo());
        q.setOffset(offset);
        q.setSize(size);
        return q;
    }

    private static void addCreateStr(
            List<OperationLogPo> rows, LogSessionContext c, String label, String newV) {
        if (StringUtils.isBlank(newV)) {
            return;
        }
        rows.add(
                line(
                        new LogLineSpec(
                                c.bizTable(),
                                c.productId(),
                                c.versionId(),
                                c.resourceId(),
                                OP_CREATE,
                                label,
                                null,
                                newV,
                                c.operatorId(),
                                c.now(),
                                c.batch(),
                                null)));
    }

    private static void diffStr(
            List<OperationLogPo> rows, LogSessionContext c, String label, String b, String a) {
        if (Objects.equals(nullToEmpty(b), nullToEmpty(a))) {
            return;
        }
        rows.add(
                line(
                        new LogLineSpec(
                                c.bizTable(),
                                c.productId(),
                                c.versionId(),
                                c.resourceId(),
                                OP_UPDATE,
                                label,
                                nullToEmpty(b),
                                nullToEmpty(a),
                                c.operatorId(),
                                c.now(),
                                c.batch(),
                                null)));
    }

    private static void diffInt(
            List<OperationLogPo> rows, LogSessionContext c, String label, Integer b, Integer a) {
        if (Objects.equals(b, a)) {
            return;
        }
        rows.add(
                line(
                        new LogLineSpec(
                                c.bizTable(),
                                c.productId(),
                                c.versionId(),
                                c.resourceId(),
                                OP_UPDATE,
                                label,
                                intStr(b),
                                intStr(a),
                                c.operatorId(),
                                c.now(),
                                c.batch(),
                                null)));
    }

    private static String intStr(Integer i) {
        return i == null ? "" : String.valueOf(i);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.isNotBlank(a)) {
            return a;
        }
        return b;
    }

    private static OperationLogPo line(LogLineSpec s) {
        OperationLogPo po = new OperationLogPo();
        po.setLogId(IdGenerator.operationLogId());
        po.setBizTable(s.bizTable());
        po.setOwnedProductId(s.productId());
        po.setOwnedVersionId(s.versionId());
        po.setResourceId(s.resourceId());
        po.setResourceName(firstNonBlank(s.resourceName(), RESOURCE_NAME.get()));
        po.setOperationType(s.opType());
        po.setFieldLabelCn(s.fieldLabelCn());
        po.setOldValue(s.oldV());
        po.setNewValue(s.newV());
        po.setOperatorId(s.operatorId());
        po.setOperatedAt(s.at());
        String batch = firstNonBlank(s.logBatchId(), IMPORT_LOG_BATCH.get());
        po.setLogBatchId(batch);
        return po;
    }

    /** 与 {@link #addCreateStr} / {@link #diffStr} / {@link #diffInt} 配套的会话上下文。 */
    private record LogSessionContext(
            String bizTable,
            String productId,
            String versionId,
            String resourceId,
            String operatorId,
            LocalDateTime now,
            String batch) {}

    /** 构造单条操作日志行（与 {@link #line(LogLineSpec)} 配套）。 */
    private record LogLineSpec(
            String bizTable,
            String productId,
            String versionId,
            String resourceId,
            String opType,
            String fieldLabelCn,
            String oldV,
            String newV,
            String operatorId,
            LocalDateTime at,
            String logBatchId,
            String resourceName) {}

    /**
     * 按业务表删除字典行时写入的审计入参（供各应用服务调用
     * {@link #logDictRowDelete(LogDictRowDeleteInput)}）。
     */
    public record LogDictRowDeleteInput(
            String bizTable,
            String productId,
            String versionId,
            String resourceId,
            String display,
            String operatorId) {}
}
