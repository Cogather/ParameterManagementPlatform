package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 保存路径：parameter_code 与序号、bit_usage 不相交（spec-03 §5.1～§5.2）。 */
public final class ParameterSaveInvariant {

    private ParameterSaveInvariant() {}

    /**
     * 校验参数序号与参数编码中解析出的序号一致。
     *
     * @param parameterCode     参数编码
     * @param parameterSequence 参数序号
     */
    public static void assertSequenceMatchesCode(String parameterCode, Integer parameterSequence) {
        ParameterCode.Parsed parsed = ParameterCode.parse(parameterCode);
        if (parameterSequence == null || parameterSequence != parsed.sequence()) {
            throw new DomainRuleException("PARAM_SEQUENCE_INVALID: parameter_sequence 须与 parameter_code 中序号一致");
        }
    }

    /**
     * 同一版本+命令下，按「类型+序号」分组校验 BIT 集合两两不相交；{@code excludeParameterId} 更新时排除自身。
     */
    public static void assertBitDisjointAcrossVersionCommand(
            Integer excludeParameterId,
            List<ParameterRowForBitCheck> rows) {
        Map<String, List<Set<Integer>>> byTypeAndSeq = new HashMap<>();
        Map<String, String> keyToTypePart = new HashMap<>();
        for (ParameterRowForBitCheck row : rows) {
            if (excludeParameterId != null
                    && excludeParameterId.equals(row.parameterId())) {
                continue;
            }
            ParameterCode.Parsed parsed = ParameterCode.parse(row.parameterCode());
            String key = parsed.typePart().toUpperCase() + "|" + parsed.sequence();
            keyToTypePart.putIfAbsent(key, parsed.typePart());
            int maxBits = ParameterTypeSemantics.maxBitSlots(parsed.typePart());
            Set<Integer> bits = parseOrAssumeFullUsage(row.bitUsage(), maxBits);
            byTypeAndSeq.computeIfAbsent(key, k -> new ArrayList<>()).add(bits);
        }
        for (Map.Entry<String, List<Set<Integer>>> e : byTypeAndSeq.entrySet()) {
            String typePart = keyToTypePart.get(e.getKey());
            BitUsage.assertPairwiseDisjointAcrossRows(typePart, e.getValue());
        }
    }

    /**
     * bit_usage 为空时按“全占用”处理：
     * - 解决历史数据/请求未填 bit_usage 导致的冲突校验绕过
     * - 满足“同序号不允许重复，除非 BIT 位不同”的业务期望
     */
    private static Set<Integer> parseOrAssumeFullUsage(String bitUsageComma, int maxBitInclusive) {
        Set<Integer> parsed = BitUsage.parseIndexes(bitUsageComma, maxBitInclusive);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        if (maxBitInclusive <= 0) {
            return Set.of();
        }
        Set<Integer> full = new LinkedHashSet<>();
        for (int i = 1; i <= maxBitInclusive; i++) {
            full.add(i);
        }
        return full;
    }

    /**
     * BIT 不相交校验所需的最小行视图。
     *
     * @param parameterId   参数 ID
     * @param parameterCode 参数编码
     * @param bitUsage      bit_usage（逗号分隔）
     */
    public record ParameterRowForBitCheck(Integer parameterId, String parameterCode, String bitUsage) {}
}
