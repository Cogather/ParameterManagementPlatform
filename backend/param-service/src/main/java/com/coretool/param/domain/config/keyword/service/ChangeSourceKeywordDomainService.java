package com.coretool.param.domain.config.keyword.service;

import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 变更来源关键字领域服务：封装归属校验、ID 冲突、状态变更等规则。 */
public class ChangeSourceKeywordDomainService {

    public record CreateCommand(
            String productId,
            String keywordId,
            String keywordRegex,
            String reason,
            Integer status,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String keywordId,
            String keywordRegex,
            String reason,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private final ChangeSourceKeywordRepository repository;

    /**
     * 创建变更来源关键字领域服务。
     *
     * @param repository 变更来源关键字仓储
     */
    public ChangeSourceKeywordDomainService(ChangeSourceKeywordRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新关键字（含原因必填与关键字 ID 冲突校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新关键字
     */
    public ChangeSourceKeyword createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        if (StringUtils.isBlank(input.reason())) {
            throw new DomainRuleException("原因不能为空");
        }
        if (repository.findByKeywordId(input.keywordId()).isPresent()) {
            throw new DomainRuleException("KEYWORD_ID_DUPLICATE: 关键字ID已存在");
        }
        return ChangeSourceKeyword.registerNew(
                new ChangeSourceKeyword.Registration(
                        input.productId(),
                        input.keywordId(),
                        input.keywordRegex(),
                        input.reason(),
                        input.status(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 更新既有关键字（含归属校验）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的关键字
     */
    public ChangeSourceKeyword updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        ChangeSourceKeyword existing = requireOwned(input.productId(), input.keywordId());
        existing.replaceRegexAndMeta(
                new ChangeSourceKeyword.RegexAndMetaPatch(
                        input.keywordRegex(), input.reason(), input.status(), input.updaterId(), input.now()));
        return existing;
    }

    /**
     * 禁用关键字（含归属校验）。
     *
     * @param productId 产品 ID
     * @param keywordId 关键字 ID
     * @param now       当前时间
     * @return 禁用后的关键字
     */
    public ChangeSourceKeyword disable(String productId, String keywordId, LocalDateTime now) {
        ChangeSourceKeyword existing = requireOwned(productId, keywordId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验关键字归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param keywordId 关键字 ID
     * @return 关键字
     */
    public ChangeSourceKeyword requireOwned(String productId, String keywordId) {
        ChangeSourceKeyword existing =
                repository.findByKeywordId(keywordId).orElseThrow(() -> new DomainRuleException("关键字不存在或不属于该产品"));
        if (!existing.belongsToProduct(productId)) {
            throw new DomainRuleException("关键字不存在或不属于该产品");
        }
        return existing;
    }
}

