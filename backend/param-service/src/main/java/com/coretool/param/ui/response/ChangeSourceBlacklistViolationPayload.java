package com.coretool.param.ui.response;

import lombok.Data;

/** 黑名单命中时 ResponseObject.data（openspec/schemas/blacklist-violation.json）。 */
@Data
public class ChangeSourceBlacklistViolationPayload {

    private String violatedKeywordRegex;

    /**
     * 创建空载荷。
     */
    public ChangeSourceBlacklistViolationPayload() {}

    /**
     * 创建载荷并设置命中的关键字正则原文。
     *
     * @param violatedKeywordRegex 命中的关键字正则原文
     */
    public ChangeSourceBlacklistViolationPayload(String violatedKeywordRegex) {
        this.violatedKeywordRegex = violatedKeywordRegex;
    }
}
