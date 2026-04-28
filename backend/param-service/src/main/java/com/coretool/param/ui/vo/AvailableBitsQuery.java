package com.coretool.param.ui.vo;

import lombok.Data;

@Data
public class AvailableBitsQuery {
    private String commandId;
    private String commandTypeId;
    private String commandTypeCode;
    private int sequence;
}

