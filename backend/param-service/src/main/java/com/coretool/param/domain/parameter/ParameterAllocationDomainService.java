package com.coretool.param.domain.parameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 序号 FULL/PARTIAL 与可用 BIT（spec-03 §3.2、§7.1～§7.2）。
 *
 * <p>FULL：该序号下尚无任何参数行。PARTIAL：已有行但仍有未占用 BIT。若 BIT 槽位已满则该序号不出现在结果中。
 */
public class ParameterAllocationDomainService {

    /**
     * 序号可用性视图。
     *
     * @param sequence     序号
     * @param availability 可用性（FULL/PARTIAL）
     */
    public record SequenceAvailability(int sequence, String availability) {}

    /**
     * 计算指定范围内可用序号与可用性（FULL/PARTIAL）。
     *
     * @param rangeMin        序号最小值（包含）
     * @param rangeMax        序号最大值（包含）
     * @param commandTypeCode 命令类型编码
     * @param commandId       命令 ID
     * @param inScope         作用域内参数快照
     * @return 可用序号列表
     */
    public List<SequenceAvailability> computeAvailableSequences(
            int rangeMin, int rangeMax, String commandTypeCode, String commandId, List<ParameterSnapshot> inScope) {
        int maxBits = ParameterTypeSemantics.maxBitSlots(commandTypeCode);
        List<SequenceAvailability> out = new ArrayList<>();
        for (int seq = rangeMin; seq <= rangeMax; seq++) {
            List<Set<Integer>> rows = collectBitRowsForSequence(commandTypeCode, commandId, seq, inScope);
            if (rows.isEmpty()) {
                out.add(new SequenceAvailability(seq, "FULL"));
            } else {
                Set<Integer> union = union(rows);
                if (union.size() >= maxBits) {
                    continue;
                }
                out.add(new SequenceAvailability(seq, "PARTIAL"));
            }
        }
        return out;
    }

    /**
     * 计算指定序号下可用的 BIT 序号集合。
     *
     * @param sequence        序号
     * @param commandTypeCode 命令类型编码
     * @param commandId       命令 ID
     * @param inScope         作用域内参数快照
     * @return 可用 BIT 序号列表
     */
    public List<Integer> computeAvailableBitIndexes(
            int sequence, String commandTypeCode, String commandId, List<ParameterSnapshot> inScope) {
        int maxBits = ParameterTypeSemantics.maxBitSlots(commandTypeCode);
        Set<Integer> occupied = new HashSet<>();
        for (ParameterSnapshot p : inScope) {
            if (!commandId.equals(p.ownedCommandId())) {
                continue;
            }
            if (p.parameterSequence() == null || p.parameterSequence() != sequence) {
                continue;
            }
            ParameterCode.Parsed parsed = ParameterCode.parse(p.parameterCode());
            if (!parsed.typePart().equalsIgnoreCase(commandTypeCode)) {
                continue;
            }
            occupied.addAll(BitUsage.parseIndexes(p.bitUsage(), maxBits));
        }
        List<Integer> avail = new ArrayList<>();
        for (int b = 1; b <= maxBits; b++) {
            if (!occupied.contains(b)) {
                avail.add(b);
            }
        }
        return avail;
    }

    private static List<Set<Integer>> collectBitRowsForSequence(
            String commandTypeCode, String commandId, int sequence, List<ParameterSnapshot> inScope) {
        int maxBits = ParameterTypeSemantics.maxBitSlots(commandTypeCode);
        List<Set<Integer>> rows = new ArrayList<>();
        for (ParameterSnapshot p : inScope) {
            if (!commandId.equals(p.ownedCommandId())) {
                continue;
            }
            if (p.parameterSequence() == null || p.parameterSequence() != sequence) {
                continue;
            }
            ParameterCode.Parsed parsed = ParameterCode.parse(p.parameterCode());
            if (!parsed.typePart().equalsIgnoreCase(commandTypeCode)) {
                continue;
            }
            rows.add(BitUsage.parseIndexes(p.bitUsage(), maxBits));
        }
        return rows;
    }

    private static Set<Integer> union(List<Set<Integer>> rows) {
        Set<Integer> u = new HashSet<>();
        for (Set<Integer> s : rows) {
            u.addAll(s);
        }
        return u;
    }

    /** 领域快照：仅承载分配计算所需字段（充血模型外可复用的值对象视图）。 */
    /**
     * 参数分配计算所需的领域快照。
     *
     * @param parameterCode     参数编码
     * @param parameterSequence 参数序号
     * @param bitUsage          bit_usage（逗号分隔）
     * @param ownedCommandId    归属命令 ID
     */
    public record ParameterSnapshot(
            String parameterCode, Integer parameterSequence, String bitUsage, String ownedCommandId) {}
}
