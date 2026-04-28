package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterCodeTest {

    @Test
    void parse_shouldSplitTypeAndSequence() {
        ParameterCode.Parsed p = ParameterCode.parse("BYTE_12");
        assertThat(p.typePart()).isEqualTo("BYTE");
        assertThat(p.sequence()).isEqualTo(12);
    }

    @Test
    void parse_shouldUseLastUnderscore_whenMultiple() {
        ParameterCode.Parsed p = ParameterCode.parse("A_B_C_3");
        assertThat(p.typePart()).isEqualTo("A_B_C");
        assertThat(p.sequence()).isEqualTo(3);
    }

    @Test
    void parse_shouldThrow_whenNullOrEmpty() {
        assertThatThrownBy(() -> ParameterCode.parse(null)).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> ParameterCode.parse("")).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void parse_shouldThrow_whenFormatInvalid() {
        assertThatThrownBy(() -> ParameterCode.parse("NO_UNDERSCORE")).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> ParameterCode.parse("_1")).isInstanceOf(DomainRuleException.class);
        assertThatThrownBy(() -> ParameterCode.parse("T_")).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void parse_shouldThrow_whenSequenceNotNumeric() {
        assertThatThrownBy(() -> ParameterCode.parse("BIT_x")).isInstanceOf(DomainRuleException.class);
    }
}
