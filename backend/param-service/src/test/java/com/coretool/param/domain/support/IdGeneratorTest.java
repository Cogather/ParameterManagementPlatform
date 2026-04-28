package com.coretool.param.domain.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    void generatedIds_shouldMatchPrefix_andFixedRandomLength() {
        assertThat(IdGenerator.versionId()).matches("version_[0-9a-f]{12}");
        assertThat(IdGenerator.categoryId()).matches("category_[0-9a-f]{12}");
        assertThat(IdGenerator.commandTypeId()).matches("ctype_[0-9a-f]{12}");
        assertThat(IdGenerator.keywordId()).matches("keyword_[0-9a-f]{12}");
        assertThat(IdGenerator.neTypeId()).matches("ne_[0-9a-f]{12}");
        assertThat(IdGenerator.nfConfigId()).matches("nf_[0-9a-f]{12}");
        assertThat(IdGenerator.effectiveModeId()).matches("effective_[0-9a-f]{12}");
        assertThat(IdGenerator.effectiveFormId()).matches("effform_[0-9a-f]{12}");
        assertThat(IdGenerator.teamId()).matches("team_[0-9a-f]{12}");
        assertThat(IdGenerator.featureId()).matches("feature_[0-9a-f]{12}");
        assertThat(IdGenerator.featureCode()).matches("feat_[0-9a-f]{12}");
        assertThat(IdGenerator.commandId()).matches("command_[0-9a-f]{12}");
        assertThat(IdGenerator.versionRangeId()).matches("range_[0-9a-f]{12}");
        assertThat(IdGenerator.changeDescriptionId()).matches("changeDes_[0-9a-f]{12}");
        assertThat(IdGenerator.productFormId()).matches("pform_[0-9a-f]{12}");
        assertThat(IdGenerator.operationLogId()).matches("oplog_[0-9a-f]{12}");
    }

    @Test
    void categoryCode_shouldDelegateToCategoryId() {
        assertThat(IdGenerator.categoryCode()).startsWith("category_");
    }
}
