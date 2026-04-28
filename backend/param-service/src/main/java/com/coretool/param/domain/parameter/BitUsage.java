package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/** bit_usage：英文逗号分隔；单行内不重复；同序号多行集合两两不相交（spec-03 §5.2）。 */
public final class BitUsage {

    private BitUsage() {}

    /**
     * 解析 {@code bit_usage} 逗号分隔序号为集合。
     *
     * @param bitUsageComma   逗号分隔序号（可为空/空白）
     * @param maxBitInclusive 最大允许序号（包含）
     * @return 序号集合
     */
    public static Set<Integer> parseIndexes(String bitUsageComma, int maxBitInclusive) {
        Set<Integer> set = new HashSet<>();
        if (StringUtils.isBlank(bitUsageComma)) {
            return set;
        }
        String[] parts = bitUsageComma.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            int bit;
            try {
                bit = Integer.parseInt(t.trim());
            } catch (NumberFormatException e) {
                throw new DomainRuleException("PARAM_BIT_CONFLICT: BIT 序号须为整数");
            }
            if (bit < 1 || bit > maxBitInclusive) {
                throw new DomainRuleException(
                        "PARAM_BIT_CONFLICT: BIT 序号须在 1～" + maxBitInclusive + " 范围内");
            }
            if (!set.add(bit)) {
                throw new DomainRuleException("PARAM_BIT_CONFLICT: 同一行 BIT 序号不可重复");
            }
        }
        return set;
    }

    /**
     * 校验同一序号下多行 BIT 集合两两不相交。
     *
     * @param typePartForMaxBits 类型语义片段（用于计算最大 BIT 槽位）
     * @param rowBitSets         多行 BIT 集合
     */
    public static void assertPairwiseDisjointAcrossRows(
            String typePartForMaxBits, java.util.List<Set<Integer>> rowBitSets) {
        int max = ParameterTypeSemantics.maxBitSlots(typePartForMaxBits);
        Set<Integer> union = new HashSet<>();
        int total = 0;
        for (Set<Integer> s : rowBitSets) {
            for (Integer b : s) {
                if (b < 1 || b > max) {
                    throw new DomainRuleException(
                            "PARAM_BIT_CONFLICT: BIT 序号须在 1～" + max + " 范围内");
                }
            }
            total += s.size();
            union.addAll(s);
        }
        if (union.size() != total) {
            throw new DomainRuleException(
                    "PARAM_BIT_CONFLICT: 同一个产品，同一个命令，同一个类型下，序号不能重复（一个序号根据BIT位拆分多个参数除外。）");
        }
    }
}
