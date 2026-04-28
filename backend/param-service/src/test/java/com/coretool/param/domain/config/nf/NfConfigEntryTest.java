package com.coretool.param.domain.config.nf;

import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NfConfigEntryTest {

    @Test
    void registerNew_shouldHydrateMandatoryFields_andDefaultCreator() {
        LocalDateTime now = LocalDateTime.now();
        var e =
                NfConfigEntry.registerNew(
                        new NfConfigEntry.Registration(
                                "p", "id1", "  N ", "d", null, "", "", now));

        assertThat(e.getNfConfigId()).isEqualTo("id1");
        assertThat(e.getNfConfigNameCn()).isEqualTo("N");
        assertThat(e.getNfConfigStatus()).isEqualTo(1);
        assertThat(e.getCreatorId()).isEqualTo("system");
    }

    @Test
    void registerNew_shouldThrow_whenInputNullOrMissingKeyFields() {
        assertThatThrownBy(() -> NfConfigEntry.registerNew(null)).isInstanceOf(DomainRuleException.class);
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(
                        () ->
                                NfConfigEntry.registerNew(
                                        new NfConfigEntry.Registration(
                                                "p", "", "n", "d", 1, "c", "u", now)))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void rehydrate_shouldReturnNull_whenSnapshotNull() {
        assertThat(NfConfigEntry.rehydrate(null)).isNull();
    }

    @Test
    void disable_shouldSetStatusZero() {
        LocalDateTime t = LocalDateTime.now();
        NfConfigEntry e =
                NfConfigEntry.registerNew(
                        new NfConfigEntry.Registration("p", "id1", "n", "d", 1, "c", "u", t));
        e.disable(t.plusHours(1));
        assertThat(e.getNfConfigStatus()).isZero();
    }
}
