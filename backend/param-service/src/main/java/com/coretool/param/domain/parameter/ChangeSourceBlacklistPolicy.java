package com.coretool.param.domain.parameter;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** change_source 黑名单：非 blank 时 find() 匹配任一启用正则即命中（spec-03 §5.3）。 */
public final class ChangeSourceBlacklistPolicy {

    private ChangeSourceBlacklistPolicy() {}

    /**
     * @param rawChangeSource 未 trim 的原文
     * @param enabledRegexes 当前产品 keyword_status=1 的 keyword_regex 列表
     * @return 命中的正则原文
     */
    public static Optional<String> findFirstViolation(String rawChangeSource, List<String> enabledRegexes) {
        if (rawChangeSource == null || rawChangeSource.isBlank()) {
            return Optional.empty();
        }
        if (enabledRegexes == null || enabledRegexes.isEmpty()) {
            return Optional.empty();
        }
        for (String regex : enabledRegexes) {
            if (regex == null || regex.isEmpty()) {
                continue;
            }
            try {
                if (Pattern.compile(regex).matcher(rawChangeSource).find()) {
                    return Optional.of(regex);
                }
            } catch (PatternSyntaxException ignored) {
                // 跳过非法正则，避免拖垮整站
            }
        }
        return Optional.empty();
    }
}
