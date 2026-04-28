package com.coretool.param.ui.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListPageQueryFactoriesTest {

    @Test
    void listPageWithKeywordQuery_of_shouldSetFields() {
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(2, 50, "kw");
        assertThat(q.getPage()).isEqualTo(2);
        assertThat(q.getSize()).isEqualTo(50);
        assertThat(q.getKeyword()).isEqualTo("kw");
    }

    @Test
    void listPageWithKeywordQuery_shouldExposeDefaults() {
        ListPageWithKeywordQuery q = new ListPageWithKeywordQuery();
        assertThat(q.getPage()).isEqualTo(1);
        assertThat(q.getSize()).isEqualTo(20);
    }

    @Test
    void listPageWithTypeFilterQuery_of_shouldSetFields() {
        ListPageWithTypeFilterQuery q = ListPageWithTypeFilterQuery.of(3, 15, "type-9");
        assertThat(q.getPage()).isEqualTo(3);
        assertThat(q.getSize()).isEqualTo(15);
        assertThat(q.getOwnedTypeId()).isEqualTo("type-9");
    }

    @Test
    void listPageWithTypeFilterQuery_shouldExposeDefaults() {
        ListPageWithTypeFilterQuery q = new ListPageWithTypeFilterQuery();
        assertThat(q.getPage()).isEqualTo(1);
        assertThat(q.getSize()).isEqualTo(20);
    }
}
