package com.coretool.param.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coretool.param.infrastructure.persistence.entity.OperationLogPo;
import com.coretool.param.ui.vo.OperationLogGroupItem;
import com.coretool.param.ui.vo.OperationLogGroupKey;
import com.coretool.param.ui.vo.OperationLogGroupLine;
import com.coretool.param.ui.vo.OperationLogGroupSelectQuery;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OperationLogMapper extends BaseMapper<OperationLogPo> {

    @Select(
            """
            <script>
            SELECT COUNT(*) FROM (
              SELECT 1
              FROM operation_log
              WHERE owned_product_id = #{q.productId}
                AND biz_table = #{q.bizTable}
                <if test="q.resourceId != null and q.resourceId != ''">
                  AND resource_id = #{q.resourceId}
                </if>
                <if test="q.operatedFrom != null">
                  AND operated_at &gt;= #{q.operatedFrom}
                </if>
                <if test="q.operatedTo != null">
                  AND operated_at &lt;= #{q.operatedTo}
                </if>
                <if test="q.versionId != null and q.versionId != ''">
                  AND owned_version_id = #{q.versionId}
                </if>
                <if test="(q.versionId == null or q.versionId == '') and q.ignoreVersionFilter == false">
                  AND owned_version_id IS NULL
                </if>
              GROUP BY biz_table, owned_product_id, owned_version_id, resource_id, operation_type, operator_id, operated_at, log_batch_id
            ) t
            </script>
            """)
    long countGroups(
            @Param("q") OperationLogGroupSelectQuery q);

    @Select(
            """
            <script>
            SELECT
              -- groupKey 仅用于前端 v-model key；不依赖 DB concat 语法，直接拼接列文本
              (COALESCE(log_batch_id,'') || '|' || COALESCE(resource_id,'') || '|' || operation_type || '|' || COALESCE(operator_id,'') || '|' || CAST(operated_at AS VARCHAR(64))) AS group_key,
              biz_table AS bizTable,
              owned_product_id AS ownedProductId,
              owned_version_id AS ownedVersionId,
              resource_id AS resourceId,
              MAX(resource_name) AS resourceName,
              operation_type AS operationType,
              operator_id AS operatorId,
              operated_at AS operatedAt,
              log_batch_id AS logBatchId,
              COUNT(*) AS itemCount
            FROM operation_log
            WHERE owned_product_id = #{q.productId}
              AND biz_table = #{q.bizTable}
              <if test="q.resourceId != null and q.resourceId != ''">
                AND resource_id = #{q.resourceId}
              </if>
              <if test="q.operatedFrom != null">
                AND operated_at &gt;= #{q.operatedFrom}
              </if>
              <if test="q.operatedTo != null">
                AND operated_at &lt;= #{q.operatedTo}
              </if>
              <if test="q.versionId != null and q.versionId != ''">
                AND owned_version_id = #{q.versionId}
              </if>
              <if test="(q.versionId == null or q.versionId == '') and q.ignoreVersionFilter == false">
                AND owned_version_id IS NULL
              </if>
            GROUP BY biz_table, owned_product_id, owned_version_id, resource_id, operation_type, operator_id, operated_at, log_batch_id
            ORDER BY operated_at DESC
            LIMIT #{q.size} OFFSET #{q.offset}
            </script>
            """)
    java.util.List<OperationLogGroupItem> selectGroupPage(
            @Param("q") OperationLogGroupSelectQuery q);

    @Select(
            """
            SELECT
              log_id AS logId,
              field_label_cn AS fieldLabelCn,
              old_value AS oldValue,
              new_value AS newValue,
              operated_at AS operatedAt
            FROM operation_log
            WHERE owned_product_id = #{k.productId}
              AND biz_table = #{k.bizTable}
              AND operation_type = #{k.operationType}
              AND operated_at = #{k.operatedAt}
              AND operator_id IS NOT DISTINCT FROM #{k.operatorId}
              AND log_batch_id IS NOT DISTINCT FROM #{k.logBatchId}
              AND resource_id IS NOT DISTINCT FROM #{k.resourceId}
              AND owned_version_id IS NOT DISTINCT FROM #{k.ownedVersionId}
            ORDER BY field_label_cn ASC, log_id ASC
            """)
    java.util.List<OperationLogGroupLine> selectLinesForGroup(
            @Param("k") OperationLogGroupKey k);
}

