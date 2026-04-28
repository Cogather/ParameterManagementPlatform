package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.entity.EntityBasicInfoPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityBasicInfoMapper;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.EntityBasicInfoListQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class EntityBasicInfoAppService {

    @Resource
    private EntityBasicInfoMapper entityBasicInfoMapper;

    @Resource
    private OperationLogAppService operationLogAppService;

    /**
     * 分页查询产品主数据。
     *
     * @param q 产品主数据列表查询（页码、页大小、关键字、产品 ID 等见 EntityBasicInfoListQuery）
     * @return 分页结果
     */
    public PageResponse<EntityBasicInfoPo> page(EntityBasicInfoListQuery q) {
        int page = q.getPage();
        int size = q.getSize();
        String keyword = q.getKeyword();
        String productId = q.getProductId();
        var w = pageScope();
        if (StringUtils.isNotBlank(productId)) {
            w.eq(EntityBasicInfoPo::getProductId, productId.trim());
        }
        if (StringUtils.isNotBlank(keyword)) {
            String k = keyword.trim();
            w.and(
                    wq ->
                            wq.like(EntityBasicInfoPo::getEntityName, k)
                                    .or()
                                    .like(EntityBasicInfoPo::getProductForm, k)
                                    .or()
                                    .like(EntityBasicInfoPo::getProductId, k)
                                    .or()
                                    .like(EntityBasicInfoPo::getProductFormId, k));
        }
        Page<EntityBasicInfoPo> p = new Page<>(page, size);
        entityBasicInfoMapper.selectPage(
                p, w.orderByDesc(EntityBasicInfoPo::getUpdateTimestamp));
        PageResponse<EntityBasicInfoPo> out = new PageResponse<>();
        out.setRecords(p.getRecords());
        out.setTotal(p.getTotal());
        out.setPage(page);
        out.setSize(size);
        return out;
    }

    /**
     * 供「产品：」下拉里拉平展示：同 {@code product_id} 下取一条代表名称（取字典序最小的一条行）。
     *
     * @return 产品下拉选项
     */
    public List<EntityBasicInfoPo> listProductChoices() {
        var rows =
                entityBasicInfoMapper.selectList(
                        pageScope().orderByDesc(EntityBasicInfoPo::getUpdateTimestamp));
        TreeMap<String, EntityBasicInfoPo> byProductId = new TreeMap<>();
        for (EntityBasicInfoPo r : rows) {
            String pid = StringUtils.trimToEmpty(r.getProductId());
            if (StringUtils.isBlank(pid)) {
                continue;
            }
            if (!byProductId.containsKey(pid)) {
                byProductId.put(pid, r);
            } else {
                EntityBasicInfoPo old = byProductId.get(pid);
                String o1 = StringUtils.defaultString(old.getEntityName());
                String o2 = StringUtils.defaultString(r.getEntityName());
                if (o2.compareTo(o1) < 0) {
                    byProductId.put(pid, r);
                }
            }
        }
        return byProductId.values().stream()
                .sorted(Comparator.comparing(o -> StringUtils.defaultString(o.getEntityName())))
                .collect(Collectors.toList());
    }

    /**
     * 新增产品主数据。
     *
     * @param input 请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityBasicInfoPo create(EntityBasicInfoPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        validateForSave(input, true);
        String formName = requireProductFormName(input);
        assertProductFormNameUnique(input.getProductId(), formName, null);
        if (StringUtils.isBlank(input.getProductFormId())) {
            input.setProductFormId(IdGenerator.productFormId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (input.getEntityStatus() == null) {
            input.setEntityStatus(1);
        }
        String user = StringUtils.defaultIfBlank(input.getCreatorId(), "system");
        input.setCreatorId(user);
        input.setUpdaterId(StringUtils.defaultIfBlank(input.getUpdaterId(), user));
        input.setCreationTimestamp(now);
        input.setUpdateTimestamp(now);
        entityBasicInfoMapper.insert(input);
        String whoC = StringUtils.defaultIfBlank(input.getCreatorId(), "system");
        operationLogAppService.logEntityBasicInfoCreate(input.getProductId(), input, whoC);
        return input;
    }

    /**
     * 更新产品主数据（按产品形态维度）。
     *
     * @param productFormId 产品形态 ID
     * @param input         请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityBasicInfoPo update(String productFormId, EntityBasicInfoPo input) {
        if (StringUtils.isBlank(productFormId)) {
            throw new DomainRuleException("主键不能为空");
        }
        EntityBasicInfoPo existing = entityBasicInfoMapper.selectById(productFormId);
        if (existing == null) {
            throw new DomainRuleException("记录不存在");
        }
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        EntityBasicInfoPo before = new EntityBasicInfoPo();
        BeanUtils.copyProperties(existing, before);
        mergeUpdate(existing, input);
        validateForSave(existing, false);
        String formName = requireProductFormName(existing);
        assertProductFormNameUnique(existing.getProductId(), formName, productFormId);
        existing.setUpdateTimestamp(LocalDateTime.now());
        if (StringUtils.isBlank(existing.getUpdaterId())) {
            existing.setUpdaterId("system");
        }
        entityBasicInfoMapper.updateById(existing);
        String opU = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logEntityBasicInfoUpdate(before, existing, opU);
        return existing;
    }

    /**
     * 删除产品主数据（软删除）。
     *
     * @param productFormId 产品形态 ID
     */
    @Transactional
    public void softDelete(String productFormId) {
        if (StringUtils.isBlank(productFormId)) {
            throw new DomainRuleException("主键不能为空");
        }
        EntityBasicInfoPo existing = entityBasicInfoMapper.selectById(productFormId);
        if (existing == null) {
            throw new DomainRuleException("记录不存在");
        }
        existing.setEntityStatus(0);
        existing.setUpdateTimestamp(LocalDateTime.now());
        existing.setUpdaterId(
                StringUtils.defaultIfBlank(existing.getUpdaterId(), "system"));
        entityBasicInfoMapper.updateById(existing);
        operationLogAppService.logEntityBasicInfoDelete(existing, existing.getUpdaterId());
    }

    private static void mergeUpdate(EntityBasicInfoPo target, EntityBasicInfoPo src) {
        if (StringUtils.isNotBlank(src.getEntityName())) {
            target.setEntityName(src.getEntityName().trim());
        }
        if (StringUtils.isNotBlank(src.getProductSoftParamType())) {
            target.setProductSoftParamType(src.getProductSoftParamType().trim());
        }
        if (src.getProductForm() != null) {
            target.setProductForm(StringUtils.trimToNull(src.getProductForm()));
        }
        if (StringUtils.isNotBlank(src.getProductId())) {
            target.setProductId(src.getProductId().trim());
        }
        if (StringUtils.isNotBlank(src.getOwnerList())) {
            target.setOwnerList(src.getOwnerList().trim());
        }
        if (src.getEntityStatus() != null) {
            target.setEntityStatus(src.getEntityStatus());
        }
        if (StringUtils.isNotBlank(src.getUpdaterId())) {
            target.setUpdaterId(src.getUpdaterId().trim());
        }
    }

    private static void validateForSave(EntityBasicInfoPo p, boolean creating) {
        if (StringUtils.isBlank(p.getEntityName())) {
            throw new DomainRuleException("产品名称不能为空");
        }
        if (StringUtils.isBlank(p.getProductSoftParamType())) {
            throw new DomainRuleException("产品参数类型不能为空");
        }
        String t = p.getProductSoftParamType().trim();
        if (!"Single".equals(t) && !"Multi".equals(t)) {
            throw new DomainRuleException("产品参数类型仅允许 Single 或 Multi");
        }
        p.setProductSoftParamType(t);
        if (StringUtils.isBlank(p.getProductId())) {
            throw new DomainRuleException("产品 ID 不能为空");
        }
        p.setProductId(p.getProductId().trim());
        if (StringUtils.isBlank(p.getOwnerList())) {
            throw new DomainRuleException("责任人不能为空");
        }
        p.setOwnerList(p.getOwnerList().trim());
        if (creating) {
            p.setEntityName(p.getEntityName().trim());
        } else {
            p.setEntityName(StringUtils.trimToEmpty(p.getEntityName()));
        }
    }

    private static String requireProductFormName(EntityBasicInfoPo p) {
        if (p == null || StringUtils.isBlank(p.getProductForm())) {
            throw new DomainRuleException("产品形态名称不能为空");
        }
        return p.getProductForm().trim();
    }

    /**
     * 同一 {@code product_id} 下启用态的「产品形态」名称唯一（忽略大小写、首尾空白）。
     */
    private void assertProductFormNameUnique(
            String productId, String productForm, String excludeProductFormId) {
        if (StringUtils.isBlank(productForm) || StringUtils.isBlank(productId)) {
            return;
        }
        long cnt =
                entityBasicInfoMapper.selectCount(
                        Wrappers.<EntityBasicInfoPo>lambdaQuery()
                                .eq(EntityBasicInfoPo::getProductId, productId.trim())
                                .ne(
                                        StringUtils.isNotBlank(excludeProductFormId),
                                        EntityBasicInfoPo::getProductFormId,
                                        excludeProductFormId)
                                .and(
                                        q ->
                                                q.eq(EntityBasicInfoPo::getEntityStatus, 1)
                                                        .or()
                                                        .isNull(
                                                                EntityBasicInfoPo
                                                                        ::getEntityStatus))
                                .apply("lower(btrim(product_form)) = {0}", productForm.toLowerCase()));
        if (cnt > 0) {
            throw new DomainRuleException("PRODUCT_FORM_DUPLICATE: 同一产品下产品形态名称已存在");
        }
    }

    private LambdaQueryWrapper<EntityBasicInfoPo> pageScope() {
        return Wrappers.<EntityBasicInfoPo>lambdaQuery()
                .and(
                        q ->
                                q.eq(EntityBasicInfoPo::getEntityStatus, 1)
                                        .or()
                                        .isNull(EntityBasicInfoPo::getEntityStatus));
    }
}
