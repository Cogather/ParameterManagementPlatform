package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import java.util.List;
import java.util.Set;

/**
 * 变更类型与新增/编辑约束（spec-03 §3.3）；入参为已 trim 的变更类型中文名列表。
 */
public final class ChangeDescriptionTypeRules {

    public static final String TYPE_NEW_PARAMETER_CN = "新增参数";

    private ChangeDescriptionTypeRules() {}

    /**
     * 校验参数保存时的变更说明类型约束。
     *
     * @param isCreate          是否创建场景
     * @param changeTypeNamesCn 变更类型中文名列表（已 trim）
     * @param allowedNameCn     允许的变更类型中文名集合
     */
    public static void validateForSave(boolean isCreate, List<String> changeTypeNamesCn, Set<String> allowedNameCn) {
        if (changeTypeNamesCn == null || changeTypeNamesCn.isEmpty()) {
            throw new DomainRuleException("PARAM_CHANGE_DESC_REQUIRED: 至少一条变更说明");
        }
        for (String name : changeTypeNamesCn) {
            if (name == null || name.isBlank()) {
                throw new DomainRuleException("变更类型不能为空");
            }
            String n = name.trim();
            if (!allowedNameCn.contains(n)) {
                throw new DomainRuleException("变更类型不在字典范围内: " + n);
            }
        }
        if (isCreate) {
            for (String name : changeTypeNamesCn) {
                if (!TYPE_NEW_PARAMETER_CN.equals(name.trim())) {
                    throw new DomainRuleException("新增参数时变更类型仅允许「新增参数」");
                }
            }
        }
    }
}
