package com.coretool.param.domain.config.nf.service;

import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.config.nf.repository.NfConfigRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** NF 配置领域服务：封装唯一性/归属/状态变更等规则。 */
public class NfConfigDomainService {

    public record CreateCommand(
            String productId,
            String nfConfigId,
            String nameCn,
            String description,
            Integer status,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String nfConfigId,
            String nameCn,
            String description,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private final NfConfigRepository repository;

    /**
     * 创建 NF 配置领域服务。
     *
     * @param repository NF 配置仓储
     */
    public NfConfigDomainService(NfConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新 NF 配置（含同产品下名称唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新 NF 配置
     */
    public NfConfigEntry createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        NfConfigEntry entry =
                NfConfigEntry.registerNew(
                        new NfConfigEntry.Registration(
                                input.productId(),
                                input.nfConfigId(),
                                input.nameCn(),
                                input.description(),
                                input.status(),
                                input.creatorId(),
                                input.updaterId(),
                                input.now()));
        if (repository.existsSameNameInProduct(input.productId(), entry.getNfConfigNameCn(), null)) {
            throw new DomainRuleException("NF_NAME_DUPLICATE: 同一产品下 NF 名称已存在");
        }
        return entry;
    }

    /**
     * 更新既有 NF 配置（含归属校验与同产品下名称唯一性校验；无字段变更则仅刷新更新时间）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的 NF 配置
     */
    public NfConfigEntry updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        NfConfigEntry existing = requireOwned(input.productId(), input.nfConfigId());
        if (StringUtils.isNotBlank(input.nameCn())
                && !StringUtils.equals(input.nameCn(), existing.getNfConfigNameCn())) {
            if (repository.existsSameNameInProduct(input.productId(), input.nameCn(), input.nfConfigId())) {
                throw new DomainRuleException("NF_NAME_DUPLICATE: 同一产品下 NF 名称已存在");
            }
        }
        if (StringUtils.isNotBlank(input.nameCn())
                || input.description() != null
                || input.status() != null
                || StringUtils.isNotBlank(input.updaterId())) {
            existing.applyPatch(
                    new NfConfigEntry.Patch(
                            input.nameCn(),
                            input.description(),
                            input.status(),
                            input.updaterId(),
                            input.now()));
        } else {
            existing.touch(input.now());
        }
        return existing;
    }

    /**
     * 禁用 NF 配置（含归属校验）。
     *
     * @param productId  产品 ID
     * @param nfConfigId NF 配置 ID
     * @param now        当前时间
     * @return 禁用后的 NF 配置
     */
    public NfConfigEntry disable(String productId, String nfConfigId, LocalDateTime now) {
        NfConfigEntry existing = requireOwned(productId, nfConfigId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验 NF 配置归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId  产品 ID
     * @param nfConfigId NF 配置 ID
     * @return NF 配置
     */
    public NfConfigEntry requireOwned(String productId, String nfConfigId) {
        return repository
                .findById(nfConfigId)
                .filter(e -> e.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("NF 配置不存在或不属于该产品"));
    }
}

