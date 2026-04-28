package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 产品主数据全路径分页：keyword / productId 均可选。
 */
@Data
public class EntityBasicInfoListQuery {
    private int page = 1;
    private int size = 20;
    private String keyword;
    private String productId;
}
