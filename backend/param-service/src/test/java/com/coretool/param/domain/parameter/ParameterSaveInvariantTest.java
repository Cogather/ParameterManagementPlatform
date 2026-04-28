package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterSaveInvariantTest {

    @Test
    void assertSequenceMatchesCode_shouldPass_whenConsistent() {
        assertThatCode(() -> ParameterSaveInvariant.assertSequenceMatchesCode("BYTE_3", 3))
                .doesNotThrowAnyException();
    }

    @Test
    void assertSequenceMatchesCode_shouldThrow_whenNullOrMismatch() {
        assertThatThrownBy(() -> ParameterSaveInvariant.assertSequenceMatchesCode("BYTE_3", null))
                .isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> ParameterSaveInvariant.assertSequenceMatchesCode("BYTE_3", 4))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void assertBitDisjointAcrossVersionCommand_shouldPass_whenDisjointUnderSameTypeSeq() {
        var rows =
                List.of(
                        new ParameterSaveInvariant.ParameterRowForBitCheck(1, "BYTE_1", "1,2"),
                        new ParameterSaveInvariant.ParameterRowForBitCheck(2, "BYTE_1", "3,4"));
        assertThatCode(() -> ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, rows))
                .doesNotThrowAnyException();
    }

    @Test
    void assertBitDisjointAcrossVersionCommand_shouldSkipExcludedId() {
        var rows =
                List.of(
                        new ParameterSaveInvariant.ParameterRowForBitCheck(9, "BIT_1", "1"),
                        new ParameterSaveInvariant.ParameterRowForBitCheck(
                                9, "BIT_1", "1")); // duplicate id — second skipped
        assertThatCode(() -> ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(9, rows))
                .doesNotThrowAnyException();
    }

    @Test
    void assertBitDisjointAcrossVersionCommand_shouldThrow_whenBitsOverlapAcrossRows() {
        var rows =
                List.of(
                        new ParameterSaveInvariant.ParameterRowForBitCheck(1, "BYTE_1", "1"),
                        new ParameterSaveInvariant.ParameterRowForBitCheck(2, "BYTE_1", "1"));
        assertThatThrownBy(() -> ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, rows))
                .isInstanceOf(DomainRuleException.class);
    }
}
