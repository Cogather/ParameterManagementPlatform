package com.coretool.param.ui.controller;

import com.coretool.param.application.service.EntityBasicInfoAppService;
import com.coretool.param.application.service.ParameterAppService;
import com.coretool.param.application.service.ParameterCommandTreeAppService;
import com.coretool.param.infrastructure.persistence.entity.EntityBasicInfoPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.ui.response.AvailableBitsData;
import com.coretool.param.ui.response.AvailableSequencesData;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.AvailableBitsQuery;
import com.coretool.param.ui.vo.EntityBasicInfoListQuery;
import com.coretool.param.ui.vo.ParameterCommandFilterQuery;
import com.coretool.param.ui.vo.ParameterCommandTreeNode;
import com.coretool.param.ui.vo.ParameterPageQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParameterSideControllersTest {

    @Mock
    private ParameterAppService parameterAppService;
    @Mock
    private ParameterCommandTreeAppService parameterCommandTreeAppService;
    @Mock
    private EntityBasicInfoAppService entityBasicInfoAppService;

    @Test
    void parameterController_page_shouldDelegate() {
        ParameterController c = new ParameterController(parameterAppService);
        ParameterPageQuery q = new ParameterPageQuery();
        PageResponse<SystemParameterPo> page = new PageResponse<>();
        when(parameterAppService.page("p", "v", q)).thenReturn(page);
        assertThat(c.page("p", "v", q).getData()).isSameAs(page);
    }

    @Test
    void parameterController_availableSequences_shouldResolveTypeKey() {
        ParameterController c = new ParameterController(parameterAppService);
        ParameterCommandFilterQuery f = new ParameterCommandFilterQuery();
        f.setCommandId("cmd");
        f.setCommandTypeCode(" BYTE ");
        AvailableSequencesData out = new AvailableSequencesData();
        when(parameterAppService.availableSequences("p", "v", "cmd", "BYTE")).thenReturn(out);

        assertThat(c.availableSequences("p", "v", f).getData()).isSameAs(out);
    }

    @Test
    void parameterController_availableBits_shouldResolveTypeKey() {
        ParameterController c = new ParameterController(parameterAppService);
        AvailableBitsQuery q = new AvailableBitsQuery();
        q.setCommandId("cmd");
        q.setCommandTypeId(" T1 ");
        q.setSequence(3);
        AvailableBitsData out = new AvailableBitsData();
        when(parameterAppService.availableBits("p", "v", "cmd", "T1", 3)).thenReturn(out);

        assertThat(c.availableBits("p", "v", q).getData()).isSameAs(out);
    }

    @Test
    void productParameterController_baselineCount_shouldDelegate() {
        ProductParameterController c = new ProductParameterController();
        ReflectionTestUtils.setField(c, "parameterAppService", parameterAppService);
        when(parameterAppService.countBaselineInProduct("p")).thenReturn(7L);

        assertThat(c.baselineCount("p").getData().getBaselineCount()).isEqualTo(7L);
    }

    @Test
    void parameterCommandTreeController_tree_shouldDelegate() {
        ParameterCommandTreeController c = new ParameterCommandTreeController();
        ReflectionTestUtils.setField(c, "parameterCommandTreeAppService", parameterCommandTreeAppService);
        ParameterCommandTreeNode node = new ParameterCommandTreeNode();
        when(parameterCommandTreeAppService.treeForProduct("p")).thenReturn(List.of(node));

        assertThat(c.tree("p").getData()).hasSize(1);
    }

    @Test
    void entityBasicInfoController_productChoices_andDelete_shouldDelegate() {
        EntityBasicInfoController c = new EntityBasicInfoController(entityBasicInfoAppService);
        when(entityBasicInfoAppService.listProductChoices()).thenReturn(List.of(new EntityBasicInfoPo()));

        assertThat(c.productChoices().getData()).hasSize(1);
        c.delete("pf1");
        verify(entityBasicInfoAppService).softDelete("pf1");
    }

    @Test
    void entityBasicInfoController_page_shouldDelegate() {
        EntityBasicInfoController c = new EntityBasicInfoController(entityBasicInfoAppService);
        EntityBasicInfoListQuery q = new EntityBasicInfoListQuery();
        PageResponse<EntityBasicInfoPo> page = new PageResponse<>();
        when(entityBasicInfoAppService.page(q)).thenReturn(page);

        assertThat(c.page(q).getData()).isSameAs(page);
    }
}
