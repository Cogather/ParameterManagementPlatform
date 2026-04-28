package com.coretool.param.ui.vo;

import lombok.Data;

@Data
public class ParameterPageQuery {
    private String commandId;
    private String commandTypeId;
    private String commandTypeCode;
    private int page = 1;
    private int size = 20;
}

