/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.command.repository;

import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「CommandTypeDefinitionRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface CommandTypeDefinitionRepository {

/**
 * findById。
 *
 * @param commandTypeId 见方法签名
 * @return 可选结果
 */

    Optional<CommandTypeDefinition> findById(String commandTypeId);

/**
 * existsSameNameInProduct。
 *
 * @param productId 见方法签名
 * @param commandTypeName 见方法签名
 * @param excludeCommandTypeId 见方法签名
 * @return 布尔结果
 */

    boolean existsSameNameInProduct(String productId, String commandTypeName, String excludeCommandTypeId);

    /**
     * 同一产品下按类型名称查找“已删除/未启用”(status=0) 的类型，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param commandTypeName 命令类型名称
     * @return 若存在则返回命令类型定义
     */
    Optional<CommandTypeDefinition> findDisabledByNameInProduct(String productId, String commandTypeName);

/**
 * insert。
 *
 * @param type 见方法签名
 */

    void insert(CommandTypeDefinition type);

/**
 * update。
 *
 * @param type 见方法签名
 */

    void update(CommandTypeDefinition type);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param keyword 见方法签名
 * @return 结果
 */

    PageSlice<CommandTypeDefinition> pageByProduct(String productId, int page, int size, String keyword);
}
