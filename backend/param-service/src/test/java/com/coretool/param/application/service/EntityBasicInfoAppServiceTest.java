package com.coretool.param.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.infrastructure.persistence.entity.EntityBasicInfoPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityBasicInfoMapper;
import com.coretool.param.ui.vo.EntityBasicInfoListQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coretool.param.domain.exception.DomainRuleException;

import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
class EntityBasicInfoAppServiceTest {

    @Mock
    private EntityBasicInfoMapper entityBasicInfoMapper;

    @Mock
    private OperationLogAppService operationLogAppService;

    private EntityBasicInfoAppService newSvc() {
        EntityBasicInfoAppService svc = new EntityBasicInfoAppService();
        ReflectionTestUtils.setField(svc, "entityBasicInfoMapper", entityBasicInfoMapper);
        ReflectionTestUtils.setField(svc, "operationLogAppService", operationLogAppService);
        return svc;
    }

    @Test
    void page_shouldReturnRecordsAndTotals_whenMapperFillsPage() {
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Page<EntityBasicInfoPo> p = (Page<EntityBasicInfoPo>) inv.getArgument(0);
            EntityBasicInfoPo r = new EntityBasicInfoPo();
            r.setProductId("p1");
            r.setProductForm("F1");
            p.setRecords(List.of(r));
            p.setTotal(1);
            return null;
        }).when(entityBasicInfoMapper).selectPage(any(), any());

        EntityBasicInfoListQuery q = new EntityBasicInfoListQuery();
        q.setPage(1);
        q.setSize(10);
        q.setKeyword("k");

        var out = newSvc().page(q);

        assertThat(out.getTotal()).isEqualTo(1);
        assertThat(out.getRecords()).hasSize(1);
    }

    @Test
    void listProductChoices_shouldDeduplicateByProductId_andPickLexicographicallySmallestName() {
        EntityBasicInfoPo a1 = new EntityBasicInfoPo();
        a1.setProductId("p1");
        a1.setEntityName("B");
        a1.setUpdateTimestamp(LocalDateTime.now());
        EntityBasicInfoPo a2 = new EntityBasicInfoPo();
        a2.setProductId("p1");
        a2.setEntityName("A");
        a2.setUpdateTimestamp(LocalDateTime.now());
        EntityBasicInfoPo b1 = new EntityBasicInfoPo();
        b1.setProductId("p2");
        b1.setEntityName("C");
        b1.setUpdateTimestamp(LocalDateTime.now());

        when(entityBasicInfoMapper.selectList(any())).thenReturn(List.of(a1, a2, b1));

        var out = newSvc().listProductChoices();

        assertThat(out).hasSize(2);
        // sorted by entityName: A then C
        assertThat(out.getFirst().getEntityName()).isEqualTo("A");
    }

    @Test
    void listProductChoices_shouldSkipRowsWithBlankProductId() {
        EntityBasicInfoPo skip = new EntityBasicInfoPo();
        skip.setProductId("");
        skip.setEntityName("Skip");
        EntityBasicInfoPo keep = new EntityBasicInfoPo();
        keep.setProductId("p1");
        keep.setEntityName("Keep");
        keep.setUpdateTimestamp(LocalDateTime.now());

        when(entityBasicInfoMapper.selectList(any())).thenReturn(List.of(skip, keep));

        var out = newSvc().listProductChoices();

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getEntityName()).isEqualTo("Keep");
    }

    @Test
    void create_shouldInsert_whenInputValid() {
        when(entityBasicInfoMapper.selectCount(any())).thenReturn(0L);

        EntityBasicInfoPo in = new EntityBasicInfoPo();
        in.setEntityName("  Name  ");
        in.setProductSoftParamType("Single");
        in.setProductId("pid");
        in.setOwnerList("u1,u2");
        in.setProductForm("  FormOne  ");

        var out = newSvc().create(in);

        assertThat(out.getProductFormId()).isNotBlank();
        verify(entityBasicInfoMapper).insert(ArgumentMatchers.any(EntityBasicInfoPo.class));
    }

    @Test
    void create_shouldThrow_whenMandatoryFieldsMissing() {
        EntityBasicInfoPo in = new EntityBasicInfoPo();
        in.setEntityName("");
        in.setProductSoftParamType("Single");
        in.setProductId("pid");
        in.setOwnerList("u1");
        in.setProductForm("F");

        assertThatThrownBy(() -> newSvc().create(in)).isInstanceOf(DomainRuleException.class);
    }

    @Test
    void update_shouldMergeAndPersist_whenRecordExists() {
        EntityBasicInfoPo existing = new EntityBasicInfoPo();
        existing.setProductFormId("fid");
        existing.setProductId("pid");
        existing.setEntityName("Old");
        existing.setProductSoftParamType("Single");
        existing.setProductForm("FormA");
        existing.setOwnerList("u1");

        when(entityBasicInfoMapper.selectById("fid")).thenReturn(existing);
        when(entityBasicInfoMapper.selectCount(any())).thenReturn(0L);

        EntityBasicInfoPo patch = new EntityBasicInfoPo();
        patch.setEntityName("New");

        var out = newSvc().update("fid", patch);

        assertThat(out.getEntityName()).isEqualTo("New");
        verify(entityBasicInfoMapper).updateById(eq(existing));
    }

    @Test
    void softDelete_shouldSetDisabledStatus_whenRecordExists() {
        EntityBasicInfoPo existing = new EntityBasicInfoPo();
        existing.setProductFormId("fid");
        when(entityBasicInfoMapper.selectById("fid")).thenReturn(existing);

        newSvc().softDelete("fid");

        verify(entityBasicInfoMapper)
                .updateById(
                        ArgumentMatchers.<EntityBasicInfoPo>argThat(
                                po ->
                                        po.getEntityStatus() != null
                                                && po.getEntityStatus() == 0));
    }

    @Test
    void softDelete_shouldThrow_whenIdBlank() {
        assertThatThrownBy(() -> newSvc().softDelete("  "))
                .isInstanceOf(DomainRuleException.class);
    }
}

