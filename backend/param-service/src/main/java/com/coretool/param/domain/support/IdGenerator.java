package com.coretool.param.domain.support;

import java.util.UUID;

/** 主数据资源 ID 生成（前缀 + 随机段，供创建接口在客户端未传 ID 时使用）。 */
public final class IdGenerator {

    private IdGenerator() {}

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * 生成版本 ID。
     *
     * @return 版本 ID
     */
    public static String versionId() {
        return "version_" + suffix();
    }

    /**
     * 生成分类 ID。
     *
     * @return 分类 ID
     */
    public static String categoryId() {
        return "category_" + suffix();
    }

    /**
     * 生成分类编码。
     *
     * @return 分类编码
     * @deprecated 使用 {@link #categoryId()}
     */
    @Deprecated
    public static String categoryCode() {
        return categoryId();
    }

    /**
     * 生成命令类型 ID。
     *
     * @return 命令类型 ID
     */
    public static String commandTypeId() {
        return "ctype_" + suffix();
    }

    /**
     * 生成变更来源关键字 ID。
     *
     * @return 关键字 ID
     */
    public static String keywordId() {
        return "keyword_" + suffix();
    }

    /**
     * 生成网元类型 ID。
     *
     * @return 网元类型 ID
     */
    public static String neTypeId() {
        return "ne_" + suffix();
    }

    /**
     * 生成 NF 配置 ID。
     *
     * @return NF 配置 ID
     */
    public static String nfConfigId() {
        return "nf_" + suffix();
    }

    /**
     * 生成生效方式 ID。
     *
     * @return 生效方式 ID
     */
    public static String effectiveModeId() {
        return "effective_" + suffix();
    }

    /**
     * 生成生效形式 ID。
     *
     * @return 生效形式 ID
     */
    public static String effectiveFormId() {
        return "effform_" + suffix();
    }

    /**
     * 生成团队 ID。
     *
     * @return 团队 ID
     */
    public static String teamId() {
        return "team_" + suffix();
    }

    /**
     * 生成特性 ID。
     *
     * @return 特性 ID
     */
    public static String featureId() {
        return "feature_" + suffix();
    }

    /**
     * 生成版本特性业务编码（客户端可不传，由应用服务生成）。
     *
     * @return 特性业务编码
     */
    public static String featureCode() {
        return "feat_" + suffix();
    }

    /**
     * 生成命令 ID。
     *
     * @return 命令 ID
     */
    public static String commandId() {
        return "command_" + suffix();
    }

    /**
     * 生成版本区段 ID。
     *
     * @return 版本区段 ID
     */
    public static String versionRangeId() {
        return "range_" + suffix();
    }

    /**
     * 生成参数变更说明 ID。
     *
     * @return 变更说明 ID
     */
    public static String changeDescriptionId() {
        return "changeDes_" + suffix();
    }

    /**
     * 生成产品形态行主键（与 entity_basic_info.product_form_id 对齐，长度在 50 内）。
     *
     * @return 产品形态主键
     */
    public static String productFormId() {
        return "pform_" + suffix();
    }

    /**
     * 生成操作日志主键。
     *
     * @return 操作日志主键
     */
    public static String operationLogId() {
        return "oplog_" + suffix();
    }
}
