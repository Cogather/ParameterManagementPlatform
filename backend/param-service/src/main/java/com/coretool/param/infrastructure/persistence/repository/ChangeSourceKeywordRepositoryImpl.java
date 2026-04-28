package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.ChangeSourceKeywordAssembler;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeSourceKeywordPo;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeSourceKeywordMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ChangeSourceKeywordRepositoryImpl implements ChangeSourceKeywordRepository {

    private final ConfigChangeSourceKeywordMapper mapper;

    /**
     * 创建变更来源关键字仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public ChangeSourceKeywordRepositoryImpl(ConfigChangeSourceKeywordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按关键字 ID 查询关键字条目。
     *
     * @param keywordId 关键字 ID
     * @return 关键字条目（可能为空）
     */
    @Override
    public Optional<ChangeSourceKeyword> findByKeywordId(String keywordId) {
        return Optional.ofNullable(mapper.selectById(keywordId))
                .map(ChangeSourceKeywordAssembler::toDomain);
    }

    /**
     * 新增关键字条目。
     *
     * @param keyword 关键字条目
     */
    @Override
    public void insert(ChangeSourceKeyword keyword) {
        mapper.insert(ChangeSourceKeywordAssembler.toPo(keyword));
    }

    /**
     * 更新关键字条目。
     *
     * @param keyword 关键字条目
     */
    @Override
    public void update(ChangeSourceKeyword keyword) {
        mapper.updateById(ChangeSourceKeywordAssembler.toPo(keyword));
    }

    /**
     * 按产品分页查询启用的关键字条目。
     *
     * @param productId   产品 ID
     * @param page        页码（从 1 开始）
     * @param size        页大小
     * @param textKeyword 文本关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<ChangeSourceKeyword> pageByProduct(
            String productId, int page, int size, String textKeyword) {
        Page<ConfigChangeSourceKeywordPo> p = new Page<>(page, size);
        LambdaQueryWrapper<ConfigChangeSourceKeywordPo> w =
                new LambdaQueryWrapper<ConfigChangeSourceKeywordPo>()
                        .eq(ConfigChangeSourceKeywordPo::getOwnedProductId, productId)
                        .eq(ConfigChangeSourceKeywordPo::getKeywordStatus, 1)
                        .orderByDesc(ConfigChangeSourceKeywordPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(textKeyword)) {
            w.and(
                    x ->
                            x.like(ConfigChangeSourceKeywordPo::getKeywordId, textKeyword)
                                    .or()
                                    .like(ConfigChangeSourceKeywordPo::getKeywordRegex, textKeyword));
        }
        Page<ConfigChangeSourceKeywordPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(ChangeSourceKeywordAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }

    /**
     * 查询产品下启用关键字的正则列表（用于黑名单匹配）。
     *
     * @param productId 产品 ID
     * @return 正则列表
     */
    @Override
    public List<String> listEnabledRegexesByProduct(String productId) {
        LambdaQueryWrapper<ConfigChangeSourceKeywordPo> w =
                new LambdaQueryWrapper<ConfigChangeSourceKeywordPo>()
                        .eq(ConfigChangeSourceKeywordPo::getOwnedProductId, productId)
                        .eq(ConfigChangeSourceKeywordPo::getKeywordStatus, 1)
                        .select(ConfigChangeSourceKeywordPo::getKeywordRegex);
        return mapper.selectList(w).stream()
                .map(ConfigChangeSourceKeywordPo::getKeywordRegex)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
