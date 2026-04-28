package com.coretool.param.domain.command.repository;

import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface CommandTypeDefinitionRepository {

    Optional<CommandTypeDefinition> findById(String commandTypeId);

    boolean existsSameNameInProduct(String productId, String commandTypeName, String excludeCommandTypeId);

    /** 同一产品下按类型名称查找“已删除/未启用”(status=0) 的类型，用于新增时自动恢复。 */
    Optional<CommandTypeDefinition> findDisabledByNameInProduct(String productId, String commandTypeName);

    void insert(CommandTypeDefinition type);

    void update(CommandTypeDefinition type);

    PageSlice<CommandTypeDefinition> pageByProduct(String productId, int page, int size, String keyword);
}

