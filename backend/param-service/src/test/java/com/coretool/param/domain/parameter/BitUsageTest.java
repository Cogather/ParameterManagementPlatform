package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BitUsageTest {

    @Test
    void parseIndexes_shouldReturnEmpty_whenBlank() {
        assertThat(BitUsage.parseIndexes(null, 8)).isEmpty();
        assertThat(BitUsage.parseIndexes("  ", 8)).isEmpty();
    }

    @Test
    void parseIndexes_shouldParseAndTrimParts() {
        assertThat(BitUsage.parseIndexes(" 1 , 2 ,3 ", 8)).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void parseIndexes_shouldThrow_whenNotInteger() {
        assertThatThrownBy(() -> BitUsage.parseIndexes("1,a", 8)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void parseIndexes_shouldThrow_whenOutOfRangeOrDuplicate() {
        assertThatThrownBy(() -> BitUsage.parseIndexes("0", 8)).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> BitUsage.parseIndexes("9", 8)).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> BitUsage.parseIndexes("1,1", 8)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void assertPairwiseDisjointAcrossRows_shouldPass_whenDisjoint() {
        BitUsage.assertPairwiseDisjointAcrossRows(
                "BYTE", List.of(Set.of(1, 2), Set.of(3, 4)));
    }

    @Test
    void assertPairwiseDisjointAcrossRows_shouldThrow_whenOverlap() {
        assertThatThrownBy(
                        () ->
                                BitUsage.assertPairwiseDisjointAcrossRows(
                                        "BYTE", List.of(Set.of(1), Set.of(1))))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void assertPairwiseDisjointAcrossRows_shouldThrow_whenBitOutOfMaxForType() {
        assertThatThrownBy(
                        () ->
                                BitUsage.assertPairwiseDisjointAcrossRows(
                                        "BIT", List.of(Set.of(2))))
                .isInstanceOf(DomainRuleException.class);
    }
}
