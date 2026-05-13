package com.coretool.param.ui.response;

import lombok.Data;

import java.util.List;

/**
 * GET .../available-sequences 接口返回的 data 载荷（openspec/schemas/available-sequences.json），
 * 描述各序号及其可用性。
 *
 * @since 2026-04-28
 */
@Data
public class AvailableSequencesData {

    private List<SequenceItem> sequences;

    /**
     * 单个序号及其可用性说明（嵌套 DTO）。
     *
     * @since 2026-04-28
     */
    @Data
    public static class SequenceItem {
        private int sequence;
        private String availability;
    }
}
