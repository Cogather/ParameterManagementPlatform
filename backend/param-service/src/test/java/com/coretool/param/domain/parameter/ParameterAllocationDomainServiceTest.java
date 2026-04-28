package com.coretool.param.domain.parameter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterAllocationDomainServiceTest {

    private static final String CID = "cmd-1";

    private final ParameterAllocationDomainService svc = new ParameterAllocationDomainService();

    @Test
    void computeAvailableSequences_shouldReturnFull_whenNoRowsForSequence() {
        var out = svc.computeAvailableSequences(1, 2, "BYTE", CID, List.of());
        assertThat(out).extracting(ParameterAllocationDomainService.SequenceAvailability::sequence)
                .containsExactly(1, 2);
        assertThat(out).extracting(ParameterAllocationDomainService.SequenceAvailability::availability)
                .containsOnly("FULL");
    }

    @Test
    void computeAvailableSequences_shouldReturnPartial_whenSomeBitsOccupied() {
        var snap =
                new ParameterAllocationDomainService.ParameterSnapshot("BYTE_1", 1, "1", CID);
        var out = svc.computeAvailableSequences(1, 1, "BYTE", CID, List.of(snap));
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().availability()).isEqualTo("PARTIAL");
    }

    @Test
    void computeAvailableSequences_shouldOmitSequence_whenAllBitsOccupiedForBitType() {
        var snap =
                new ParameterAllocationDomainService.ParameterSnapshot("BIT_1", 1, "1", CID);
        assertThat(svc.computeAvailableSequences(1, 1, "BIT", CID, List.of(snap))).isEmpty();
    }

    @Test
    void computeAvailableBitIndexes_shouldListUnoccupied_whenSomeBitsUsed() {
        var snap =
                new ParameterAllocationDomainService.ParameterSnapshot("BYTE_3", 3, "1", CID);
        assertThat(svc.computeAvailableBitIndexes(3, "BYTE", CID, List.of(snap)))
                .containsExactly(
                        2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void computeAvailableSequences_shouldIgnoreOtherCommandId() {
        var snap =
                new ParameterAllocationDomainService.ParameterSnapshot("BYTE_1", 1, "1", "other-cmd");
        var out = svc.computeAvailableSequences(1, 1, "BYTE", CID, List.of(snap));
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().availability()).isEqualTo("FULL");
    }
}
