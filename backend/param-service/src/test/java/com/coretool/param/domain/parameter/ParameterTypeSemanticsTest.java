package com.coretool.param.domain.parameter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterTypeSemanticsTest {

    @Test
    void maxBitSlots_shouldDefaultTo32_whenNullOrUnknown() {
        assertThat(ParameterTypeSemantics.maxBitSlots(null)).isEqualTo(32);
        assertThat(ParameterTypeSemantics.maxBitSlots("")).isEqualTo(32);
        assertThat(ParameterTypeSemantics.maxBitSlots("OTHER")).isEqualTo(32);
    }

    @Test
    void maxBitSlots_shouldMapKnownTypes() {
        assertThat(ParameterTypeSemantics.maxBitSlots("bit")).isEqualTo(1);
        assertThat(ParameterTypeSemantics.maxBitSlots("BYTE")).isEqualTo(8);
        assertThat(ParameterTypeSemantics.maxBitSlots("dword")).isEqualTo(32);
        assertThat(ParameterTypeSemantics.maxBitSlots("string")).isEqualTo(32);
    }

    @Test
    void typePartMatchesCommandType_shouldBeCaseInsensitive() {
        assertThat(ParameterTypeSemantics.typePartMatchesCommandType("byte", "BYTE")).isTrue();
        assertThat(ParameterTypeSemantics.typePartMatchesCommandType("BIT", "bit")).isTrue();
    }

    @Test
    void typePartMatchesCommandType_shouldReturnFalse_whenEitherNull() {
        assertThat(ParameterTypeSemantics.typePartMatchesCommandType(null, "BIT")).isFalse();
        assertThat(ParameterTypeSemantics.typePartMatchesCommandType("BIT", null)).isFalse();
    }
}
