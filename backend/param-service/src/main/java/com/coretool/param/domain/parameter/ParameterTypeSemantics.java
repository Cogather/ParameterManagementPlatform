package com.coretool.param.domain.parameter;

import java.util.Locale;

/** BIT/BYTE/DWORD/STRING 与 BIT 槽位上限（spec-03 §3.2）。 */
public final class ParameterTypeSemantics {

    private ParameterTypeSemantics() {}

    /**
     * 根据命令类型编码推导 BIT 槽位上限。
     *
     * @param commandTypeCode 命令类型编码（BIT/BYTE/DWORD/STRING 等）
     * @return 最大 BIT 槽位数
     */
    public static int maxBitSlots(String commandTypeCode) {
        if (commandTypeCode == null || commandTypeCode.isEmpty()) {
            return 32;
        }
        String u = commandTypeCode.toUpperCase(Locale.ROOT);
        if ("BIT".equals(u)) {
            return 1;
        }
        if ("BYTE".equals(u)) {
            return 8;
        }
        if ("DWORD".equals(u) || "STRING".equals(u)) {
            return 32;
        }
        return 32;
    }

    /**
     * 判断参数编码中的类型片段是否匹配命令类型编码。
     *
     * @param typeFromCode    参数编码中的类型片段
     * @param commandTypeCode 命令类型编码
     * @return 是否匹配
     */
    public static boolean typePartMatchesCommandType(String typeFromCode, String commandTypeCode) {
        if (commandTypeCode == null || typeFromCode == null) {
            return false;
        }
        return typeFromCode.equalsIgnoreCase(commandTypeCode);
    }
}
