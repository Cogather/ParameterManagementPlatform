/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import com.coretool.param.domain.exception.DomainRuleException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 取值区间多段 min/max 校验与 {@code value_range} 拼接。
 *
 * @since 2026-06-11
 */
public final class ValueRangeSegmentsSupport {

    private static final int MAX_VALUE_RANGE_LEN = 255;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValueRangeSegmentsSupport() {}

    /**
     * 解析 JSON 段列表并写入 PO 的 segments 与拼接后的 value_range。
     *
     * @param segmentsJson JSON 数组字符串
     * @param target       目标参数 PO
     * @throws DomainRuleException 解析或校验失败时
     */
    public static void applyToParameter(String segmentsJson, com.coretool.param.infrastructure.persistence.entity.SystemParameterPo target) {
        List<Segment> segments = parseSegments(segmentsJson);
        applySegments(segments, target);
    }

    /**
     * 导入场景：优先 JSON 段列表，否则解析拼接文本（如 {@code 1-10,20-30}）。
     *
     * @param segmentsJson 取值区间 JSON（可为空）
     * @param joinedText   取值范围拼接串（可为空）
     * @param target       目标参数 PO
     * @throws DomainRuleException 两者皆空或格式非法时
     */
    public static void applyFromImport(
            String segmentsJson,
            String joinedText,
            com.coretool.param.infrastructure.persistence.entity.SystemParameterPo target) {
        if (StringUtils.isNotBlank(segmentsJson)) {
            applyToParameter(segmentsJson, target);
            return;
        }
        if (StringUtils.isNotBlank(joinedText)) {
            applyFromJoinedText(joinedText, target);
            return;
        }
    }

    /**
     * 从 {@code min-max,min-max} 文本解析并写入 PO。
     *
     * @param joinedText 拼接串
     * @param target     目标参数 PO
     * @throws DomainRuleException 解析或校验失败时
     */
    public static void applyFromJoinedText(
            String joinedText, com.coretool.param.infrastructure.persistence.entity.SystemParameterPo target) {
        List<Segment> segments = parseFromJoinedText(joinedText);
        applySegments(segments, target);
    }

    /**
     * 解析拼接文本为段列表。
     *
     * @param joinedText 如 {@code 1-10,20-30}
     * @return 段列表
     * @throws DomainRuleException 格式非法时
     */
    public static List<Segment> parseFromJoinedText(String joinedText) {
        if (StringUtils.isBlank(joinedText)) {
            throw new DomainRuleException("取值区间至少 1 段");
        }
        List<Segment> raw = new ArrayList<>();
        for (String part : joinedText.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int dash = p.indexOf('-');
            if (dash <= 0 || dash >= p.length() - 1) {
                throw new DomainRuleException("取值范围格式无效: " + p);
            }
            raw.add(new Segment(parseIntToken(p.substring(0, dash).trim()), parseIntToken(p.substring(dash + 1).trim())));
        }
        if (raw.isEmpty()) {
            throw new DomainRuleException("取值区间至少 1 段");
        }
        List<Segment> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingInt(Segment::min));
        validateSorted(sorted);
        return sorted;
    }

    private static void applySegments(
            List<Segment> segments, com.coretool.param.infrastructure.persistence.entity.SystemParameterPo target) {
        String joined = formatValueRange(segments);
        target.setValueRangeSegments(toJson(segments));
        target.setValueRange(joined);
    }

    private static int parseIntToken(String token) {
        if (!token.matches("-?\\d+")) {
            throw new DomainRuleException("取值范围格式无效: " + token);
        }
        return Integer.parseInt(token);
    }

    /**
     * 解析并校验段列表。
     *
     * @param segmentsJson JSON 数组
     * @return 段列表
     * @throws DomainRuleException 校验失败时
     */
    public static List<Segment> parseSegments(String segmentsJson) {
        if (StringUtils.isBlank(segmentsJson)) {
            throw new DomainRuleException("取值区间至少 1 段");
        }
        List<Segment> raw = readJson(segmentsJson);
        if (raw.isEmpty()) {
            throw new DomainRuleException("取值区间至少 1 段");
        }
        List<Segment> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingInt(Segment::min));
        validateSorted(sorted);
        return sorted;
    }

    /**
     * 将段列表拼接为 value_range 展示串。
     *
     * @param segments 已排序且合法的段列表
     * @return 如 {@code 1-10,20-30}
     * @throws DomainRuleException 超长时
     */
    public static String formatValueRange(List<Segment> segments) {
        String joined =
                String.join(
                        ",",
                        segments.stream().map(s -> s.min() + "-" + s.max()).toList());
        if (joined.length() > MAX_VALUE_RANGE_LEN) {
            throw new DomainRuleException("取值范围拼接后超过 " + MAX_VALUE_RANGE_LEN + " 字符");
        }
        return joined;
    }

    private static void validateSorted(List<Segment> sorted) {
        Segment prev = null;
        for (Segment s : sorted) {
            if (s.min() > s.max()) {
                throw new DomainRuleException("取值区间每段须满足 min ≤ max");
            }
            if (prev != null && s.min() <= prev.max()) {
                throw new DomainRuleException("取值区间段之间不得重叠");
            }
            prev = s;
        }
    }

    private static List<Segment> readJson(String json) {
        try {
            List<Map<String, Object>> maps =
                    MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            List<Segment> out = new ArrayList<>();
            for (Map<String, Object> m : maps) {
                out.add(new Segment(toInt(m.get("min")), toInt(m.get("max"))));
            }
            return out;
        } catch (DomainRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainRuleException("取值区间 JSON 格式无效");
        }
    }

    private static String toJson(List<Segment> segments) {
        try {
            return MAPPER.writeValueAsString(segments);
        } catch (Exception e) {
            throw new DomainRuleException("取值区间序列化失败");
        }
    }

    private static int toInt(Object v) {
        if (v == null) {
            throw new DomainRuleException("取值区间 min/max 必填");
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(v).trim();
        if (!s.matches("-?\\d+")) {
            throw new DomainRuleException("取值区间 min/max 须为整数");
        }
        return Integer.parseInt(s);
    }

    /**
     * 取值区间单段。
     *
     * @param min 最小值
     * @param max 最大值
     */
    public record Segment(int min, int max) {}
}
