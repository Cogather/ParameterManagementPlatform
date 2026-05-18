/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.infrastructure.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具/说明类「CsvHelper」。
 *
 * @since 2026-04-28
 */

public final class CsvHelper {

    private CsvHelper() {}

    /**
     * 解析 UTF-8 CSV 内容为行数组列表。
     *
     * @param bytes CSV 文件字节
     * @return 行列表
     */
    public static List<String[]> parse(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(parseLine(line));
        }
        return rows;
    }

    /**
     * RFC4180-ish: fields may be quoted.
     *
     * @param line CSV 单行文本
     * @return 字段数组
     */
    public static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                QuoteStep step = stepInQuotes(line, cur, i, c);
                i = step.index();
                inQuotes = step.inQuotes();
            } else {
                inQuotes = consumeUnquotedChar(fields, cur, c);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

    private record QuoteStep(int index, boolean inQuotes) {}

    private static QuoteStep stepInQuotes(String line, StringBuilder cur, int i, char c) {
        if (c != '"') {
            cur.append(c);
            return new QuoteStep(i, true);
        }
        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            cur.append('"');
            return new QuoteStep(i + 1, true);
        }
        return new QuoteStep(i, false);
    }

    /** @return 是否进入引号域 */
    private static boolean consumeUnquotedChar(List<String> fields, StringBuilder cur, char c) {
        if (c == '"') {
            return true;
        }
        if (c == ',') {
            fields.add(cur.toString());
            cur.setLength(0);
            return false;
        }
        cur.append(c);
        return false;
    }

    /**
     * 转义 CSV 字段（必要时加引号并转义双引号）。
     *
     * @param s 字段原值
     * @return 转义后的字段
     */
    public static String escapeField(String s) {
        if (s == null) {
            return "";
        }
        String cleaned = s.replace("\"", "\"\"");
        if (cleaned.contains(",") || cleaned.contains("\n") || cleaned.contains("\r")) {
            return "\"" + cleaned + "\"";
        }
        return cleaned;
    }
}
