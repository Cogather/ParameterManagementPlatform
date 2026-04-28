package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

/** 参数编码 类型_序号（spec-03 §5.1、design.md R5）。 */
public final class ParameterCode {

    private ParameterCode() {}

    /**
     * 解析参数编码为类型片段与序号。
     *
     * @param parameterCode 参数编码（格式：类型_序号）
     * @return 解析结果
     */
    public static Parsed parse(String parameterCode) {
        if (parameterCode == null || parameterCode.isEmpty()) {
            throw new DomainRuleException("PARAM_CODE_INVALID: parameter_code 不能为空");
        }
        int idx = parameterCode.lastIndexOf('_');
        if (idx <= 0 || idx == parameterCode.length() - 1) {
            throw new DomainRuleException("PARAM_CODE_INVALID: 格式须为 类型_序号");
        }
        String typePart = parameterCode.substring(0, idx);
        String seqStr = parameterCode.substring(idx + 1);
        try {
            int sequence = Integer.parseInt(seqStr);
            return new Parsed(typePart, sequence);
        } catch (NumberFormatException e) {
            throw new DomainRuleException("PARAM_CODE_INVALID: 序号须为数字");
        }
    }

    /**
     * 参数编码解析结果。
     *
     * @param typePart 类型片段
     * @param sequence 序号
     */
    public record Parsed(String typePart, int sequence) {}
}
