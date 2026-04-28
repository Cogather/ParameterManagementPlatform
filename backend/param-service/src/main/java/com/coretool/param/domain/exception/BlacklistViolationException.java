package com.coretool.param.domain.exception;

/**
 * 变更来源命中关键字黑名单（spec-03 §5.3）；HTTP 500 且 data.violatedKeywordRegex 为命中正则原文。
 */
public class BlacklistViolationException extends RuntimeException {

    private final String violatedKeywordRegex;

    /**
     * 创建黑名单命中异常。
     *
     * @param message              异常信息
     * @param violatedKeywordRegex 命中的关键字正则原文
     */
    public BlacklistViolationException(String message, String violatedKeywordRegex) {
        super(message);
        this.violatedKeywordRegex = violatedKeywordRegex;
    }

    /**
     * 获取命中的关键字正则原文。
     *
     * @return 命中的关键字正则原文
     */
    public String getViolatedKeywordRegex() {
        return violatedKeywordRegex;
    }
}
