package com.coretool.param.ui.response;

import lombok.Data;

import java.util.List;

/** GET .../available-bits 的 data（openspec/schemas/available-bits.json）。 */
@Data
public class AvailableBitsData {

    private int sequence;
    private List<Integer> availableBitIndexes;
}
