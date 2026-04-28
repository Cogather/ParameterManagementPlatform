package com.coretool.param.application.support;

import com.coretool.param.domain.parameter.ChangeDescriptionTypeRules;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeTypePo;

import java.util.ArrayList;
import java.util.List;

/** 表 config_change_type 无数据时的字典兜底（spec-03 §3.6）。 */
public final class ChangeDescriptionTypeDictionary {

    private ChangeDescriptionTypeDictionary() {}

    /**
     * 生成 {@code config_change_type} 的兜底字典行（当表无数据时使用）。
     *
     * @return 兜底字典行
     */
    public static List<ConfigChangeTypePo> defaultRows() {
        List<String> names =
                List.of(
                        ChangeDescriptionTypeRules.TYPE_NEW_PARAMETER_CN,
                        "修改参数含义",
                        "修改参数取值范围",
                        "修改关联参数",
                        "修改默认值",
                        "修改推荐值",
                        "修改适用网元",
                        "修改生效方式",
                        "修改参数取值说明");
        List<ConfigChangeTypePo> out = new ArrayList<>();
        int id = 1;
        for (String cn : names) {
            ConfigChangeTypePo p = new ConfigChangeTypePo();
            p.setChangeTypeId(id);
            p.setChangeTypeName(cn);
            p.setChangeTypeNameCn(cn);
            p.setChangeTypeNameEn(toEnStub(cn));
            p.setChangeSequence(id);
            id++;
            out.add(p);
        }
        return out;
    }

    private static String toEnStub(String cn) {
        return switch (cn) {
            case "新增参数" -> "New parameter";
            case "修改参数含义" -> "Change meaning";
            case "修改参数取值范围" -> "Change value range";
            case "修改关联参数" -> "Change related parameter";
            case "修改默认值" -> "Change default";
            case "修改推荐值" -> "Change recommended";
            case "修改适用网元" -> "Change NE";
            case "修改生效方式" -> "Change effective mode";
            case "修改参数取值说明" -> "Change value description";
            default -> cn;
        };
    }
}
