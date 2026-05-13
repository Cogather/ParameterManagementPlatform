package com.coretool.param.ui.vo;

import com.coretool.param.infrastructure.persistence.entity.ConfigChangeDescriptionPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

import lombok.Data;

import java.util.List;

/**
 * 参数保存：主表 + 变更说明子表（spec-03）。
 *
 * @since 2026-04-28
 */
@Data
public class ParameterSaveRequest {

    private SystemParameterPo main;
    private List<ConfigChangeDescriptionPo> changeDescriptions;
}
