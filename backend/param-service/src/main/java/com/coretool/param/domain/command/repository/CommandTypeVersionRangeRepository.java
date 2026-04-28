package com.coretool.param.domain.command.repository;

import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.domain.support.PageSlice;

import java.util.List;
import java.util.Optional;

public interface CommandTypeVersionRangeRepository {

    Optional<CommandTypeVersionRange> findById(String rangeId);

    /**
     * 与候选区段同一产品/命令/类型/版本作用域下的启用区段，用于序号区间重叠校验。
     */
    List<CommandTypeVersionRange> listEnabledInScope(
            String productId, String ownedCommandId, String ownedTypeId, String ownedVersionOrBusinessId);

    void insert(CommandTypeVersionRange range);

    void update(CommandTypeVersionRange range);

    PageSlice<CommandTypeVersionRange> pageByProduct(String productId, int page, int size, String ownedTypeIdFilter);
}

