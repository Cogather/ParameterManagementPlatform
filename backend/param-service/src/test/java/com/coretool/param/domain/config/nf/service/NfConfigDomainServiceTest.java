package com.coretool.param.domain.config.nf.service;

import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.config.nf.repository.NfConfigRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfConfigDomainServiceTest {

    @Mock
    private NfConfigRepository repository;

    private NfConfigDomainService svc() {
        return new NfConfigDomainService(repository);
    }

    private static NfConfigDomainService.CreateCommand newCmd(LocalDateTime now) {
        return new NfConfigDomainService.CreateCommand("p1", "nf_x", "名称", "desc", 1, "c", "u", now);
    }

    private static NfConfigDomainService.UpdateCommand upd(
            LocalDateTime now,
            String name,
            Integer status,
            String updaterId) {
        return new NfConfigDomainService.UpdateCommand("p1", "nf_x", name, null, status, updaterId, now);
    }

    @Test
    void createNew_shouldThrow_whenInputNull() {
        assertThatThrownBy(() -> svc().createNew(null)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void createNew_shouldThrow_whenDuplicateName() {
        when(repository.existsSameNameInProduct(eq("p1"), eq("名称"), eq(null))).thenReturn(true);

        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> svc().createNew(newCmd(now)))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("DUPLICATE");
    }

    @Test
    void createNew_shouldReturnEntry_whenUnique() {
        when(repository.existsSameNameInProduct(eq("p1"), eq("名称"), eq(null))).thenReturn(false);

        LocalDateTime now = LocalDateTime.now();
        NfConfigEntry e = svc().createNew(newCmd(now));

        assertThat(e.getNfConfigId()).isEqualTo("nf_x");
        assertThat(e.getNfConfigNameCn()).isEqualTo("名称");
    }

    @Test
    void updateExisting_shouldTouch_whenNoEffectiveFieldChanges() {
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        NfConfigEntry existing =
                NfConfigEntry.rehydrate(
                        new NfConfigEntry.Snapshot(
                                "p1",
                                "nf_x",
                                "名称",
                                "d",
                                1,
                                "c",
                                created,
                                "u",
                                created));
        when(repository.findById("nf_x")).thenReturn(Optional.of(existing));

        LocalDateTime patchTime = LocalDateTime.now();
        var out = svc().updateExisting(upd(patchTime, null, null, null));

        assertThat(out.getUpdateTimestamp()).isEqualTo(patchTime);
    }

    @Test
    void updateExisting_shouldThrow_whenRenameConflicts() {
        NfConfigEntry existing =
                NfConfigEntry.rehydrate(
                        new NfConfigEntry.Snapshot(
                                "p1",
                                "nf_x",
                                "Old",
                                "d",
                                1,
                                "c",
                                LocalDateTime.now(),
                                "u",
                                LocalDateTime.now()));
        when(repository.findById("nf_x")).thenReturn(Optional.of(existing));
        when(repository.existsSameNameInProduct(eq("p1"), eq("New"), eq("nf_x"))).thenReturn(true);

        assertThatThrownBy(() -> svc().updateExisting(upd(LocalDateTime.now(), "New", null, "u")))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("DUPLICATE");
    }

    @Test
    void requireOwned_shouldThrow_whenMissingOrWrongProduct() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> svc().requireOwned("p1", "nf_x"))
                .isInstanceOf(DomainRuleException.class);

        NfConfigEntry wrong =
                NfConfigEntry.rehydrate(
                        new NfConfigEntry.Snapshot(
                                "other",
                                "nf_x",
                                "n",
                                "d",
                                1,
                                "c",
                                LocalDateTime.now(),
                                "u",
                                LocalDateTime.now()));
        when(repository.findById("nf_x")).thenReturn(Optional.of(wrong));
        assertThatThrownBy(() -> svc().requireOwned("p1", "nf_x"))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void disable_shouldFlipStatus_viaRequireOwnedPath() {
        LocalDateTime t0 = LocalDateTime.now().minusHours(1);
        NfConfigEntry existing =
                NfConfigEntry.rehydrate(
                        new NfConfigEntry.Snapshot(
                                "p1",
                                "nf_x",
                                "n",
                                "d",
                                1,
                                "c",
                                t0,
                                "u",
                                t0));
        when(repository.findById("nf_x")).thenReturn(Optional.of(existing));

        LocalDateTime now = LocalDateTime.now();
        assertThat(svc().disable("p1", "nf_x", now).getNfConfigStatus()).isEqualTo(0);
    }

    @Test
    void updateExisting_shouldThrow_whenInputNull() {
        assertThatThrownBy(() -> svc().updateExisting(null)).isInstanceOf(DomainRuleException.class);
    }
}
