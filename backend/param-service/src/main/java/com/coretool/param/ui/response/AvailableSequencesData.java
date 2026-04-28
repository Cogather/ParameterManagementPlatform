package com.coretool.param.ui.response;

import lombok.Data;

import java.util.List;

/** GET .../available-sequences 的 data（openspec/schemas/available-sequences.json）。 */
@Data
public class AvailableSequencesData {

    private List<SequenceItem> sequences;

    @Data
    public static class SequenceItem {
        private int sequence;
        private String availability;
    }
}
