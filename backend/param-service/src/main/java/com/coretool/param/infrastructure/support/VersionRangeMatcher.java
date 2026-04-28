package com.coretool.param.infrastructure.support;

/**
 * 版本号是否在 [minVersion, maxVersion] 内（字符串序比较；复杂 semver 需后续增强）。
 */
public final class VersionRangeMatcher {

    private VersionRangeMatcher() {}

    /**
     * 判断版本号是否在闭区间 {@code [minVersion, maxVersion]} 内（字符串序比较）。
     *
     * @param versionNumber 版本号
     * @param minVersion    最小版本（可为空）
     * @param maxVersion    最大版本（可为空）
     * @return 是否在范围内
     */
    public static boolean inRange(String versionNumber, String minVersion, String maxVersion) {
        if (versionNumber == null) {
            return false;
        }
        String v = versionNumber;
        String min = minVersion == null ? "" : minVersion;
        String max = maxVersion == null ? "" : maxVersion;
        return v.compareTo(min) >= 0 && v.compareTo(max) <= 0;
    }
}
