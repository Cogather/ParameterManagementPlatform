package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 版本维度下按命令/类型筛选（available-sequences、export 等 query 与 path 解耦）。
 * @since 2026-04-28
 */
@Data
public class ParameterCommandFilterQuery {
    private String commandId;
    private String commandTypeId;
    private String commandTypeCode;
}
