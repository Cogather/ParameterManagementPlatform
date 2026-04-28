package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.config.nf.repository.NfConfigRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.NfConfigEntryAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityNfConfigDictMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class NfConfigRepositoryImpl implements NfConfigRepository {

    private final EntityNfConfigDictMapper mapper;

    /**
     * 创建 NF 配置仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public NfConfigRepositoryImpl(EntityNfConfigDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按 NF 配置 ID 查询条目。
     *
     * @param nfConfigId NF 配置 ID
     * @return 条目（可能为空）
     */
    @Override
    public Optional<NfConfigEntry> findById(String nfConfigId) {
        return Optional.ofNullable(mapper.selectById(nfConfigId)).map(NfConfigEntryAssembler::toDomain);
    }

    /**
     * 判断同一产品下是否存在同名且启用的 NF 配置条目。
     *
     * @param productId        产品 ID
     * @param nfConfigNameCn   NF 配置中文名
     * @param excludeNfConfigId 排除的 NF 配置 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameNameInProduct(
            String productId, String nfConfigNameCn, String excludeNfConfigId) {
        LambdaQueryWrapper<EntityNfConfigDictPo> w =
                new LambdaQueryWrapper<EntityNfConfigDictPo>()
                        .eq(EntityNfConfigDictPo::getOwnedProductId, productId)
                        .eq(EntityNfConfigDictPo::getNfConfigNameCn, nfConfigNameCn)
                        .eq(EntityNfConfigDictPo::getNfConfigStatus, 1);
        if (excludeNfConfigId != null) {
            w.ne(EntityNfConfigDictPo::getNfConfigId, excludeNfConfigId);
        }
        Long c = mapper.selectCount(w);
        return c != null && c > 0;
    }

    /**
     * 按名称查询同一产品下禁用的 NF 配置条目（用于“启用复用”场景）。
     *
     * @param productId      产品 ID
     * @param nfConfigNameCn NF 配置中文名
     * @return 禁用条目（可能为空）
     */
    @Override
    public Optional<NfConfigEntry> findDisabledByNameInProduct(String productId, String nfConfigNameCn) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(nfConfigNameCn)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<EntityNfConfigDictPo> w =
                new LambdaQueryWrapper<EntityNfConfigDictPo>()
                        .eq(EntityNfConfigDictPo::getOwnedProductId, productId)
                        .eq(EntityNfConfigDictPo::getNfConfigNameCn, nfConfigNameCn)
                        .eq(EntityNfConfigDictPo::getNfConfigStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(NfConfigEntryAssembler::toDomain);
    }

    /**
     * 新增 NF 配置条目。
     *
     * @param entry 条目
     */
    @Override
    public void insert(NfConfigEntry entry) {
        mapper.insert(NfConfigEntryAssembler.toPo(entry));
    }

    /**
     * 更新 NF 配置条目。
     *
     * @param entry 条目
     */
    @Override
    public void update(NfConfigEntry entry) {
        mapper.updateById(NfConfigEntryAssembler.toPo(entry));
    }

    /**
     * 按产品分页查询启用的 NF 配置条目。
     *
     * @param productId    产品 ID
     * @param page         页码（从 1 开始）
     * @param size         页大小
     * @param nameKeyword  名称关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<NfConfigEntry> pageByProduct(
            String productId, int page, int size, String nameKeyword) {
        Page<EntityNfConfigDictPo> p = new Page<>(page, size);
        LambdaQueryWrapper<EntityNfConfigDictPo> w =
                new LambdaQueryWrapper<EntityNfConfigDictPo>()
                        .eq(EntityNfConfigDictPo::getOwnedProductId, productId)
                        .eq(EntityNfConfigDictPo::getNfConfigStatus, 1)
                        .orderByDesc(EntityNfConfigDictPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(nameKeyword)) {
            w.like(EntityNfConfigDictPo::getNfConfigNameCn, nameKeyword);
        }
        Page<EntityNfConfigDictPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(NfConfigEntryAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
