# -*- coding: utf-8 -*-
"""为指定类补充类级 Javadoc：功能说明 + @since（格式见 backend-spring.mdc §6.1）。"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SINCE = "2026-04-28"

DESCRIPTIONS: dict[str, str] = {
    "ApplicableNeAppServiceTest": "适用网元字典应用服务单元测试。",
    "BusinessCategoryAppServiceTest": "业务分类字典应用服务单元测试。",
    "ChangeSourceKeywordAppServiceTest": "变更来源关键字字典应用服务单元测试。",
    "CommandAppServiceTest": "命令字典应用服务单元测试。",
    "CommandTypeDefinitionAppServiceTest": "命令类型定义应用服务单元测试。",
    "CommandTypeVersionRangeAppServiceTest": "命令类型版本区段应用服务单元测试。",
    "ConfigChangeTypeAppServiceTest": "变更类型字典应用服务单元测试。",
    "EffectiveFormAppServiceTest": "生效形式字典应用服务单元测试。",
    "EffectiveModeAppServiceTest": "生效模式字典应用服务单元测试。",
    "EntityBasicInfoAppServiceTest": "产品基础信息应用服务单元测试。",
    "ExcelTestHelper": "应用服务层 Excel 导入导出测试辅助工具。",
    "NfConfigAppServiceTest": "NF 配置字典应用服务单元测试。",
    "OperationLogAppServiceTest": "操作日志应用服务单元测试。",
    "ParameterAppServiceTest": "参数核心应用服务单元测试。",
    "ParameterCommandTreeAppServiceTest": "参数命令树应用服务单元测试。",
    "ParameterRecordsTest": "参数应用服务 Record 构造与字段访问单元测试。",
    "ProjectTeamAppServiceTest": "项目团队字典应用服务单元测试。",
    "TypeBitDictAppServiceTest": "类型 BIT 字典应用服务单元测试。",
    "VersionAppServiceTest": "产品版本应用服务单元测试。",
    "VersionFeatureAppServiceTest": "版本特性字典应用服务单元测试。",
    "ImportResultContractTest": "批量导入结果契约单元测试。",
    "NfConfigEntryTest": "NF 配置领域实体单元测试。",
    "NfConfigDomainServiceTest": "NF 配置领域服务单元测试。",
    "BlacklistViolationExceptionTest": "黑名单违规异常单元测试。",
    "DomainRuleExceptionTest": "领域规则异常单元测试。",
    "BitUsageTest": "参数 BIT 占用值对象单元测试。",
    "ChangeDescriptionTypeRulesTest": "变更说明类型规则单元测试。",
    "ParameterAllocationDomainServiceTest": "参数序号与 BIT 分配领域服务单元测试。",
    "ParameterBaselinePolicyTest": "参数基线锁定策略单元测试。",
    "ParameterCodeTest": "参数编码值对象单元测试。",
    "ParameterSaveInvariantTest": "参数保存不变量单元测试。",
    "ParameterTest": "参数领域实体单元测试。",
    "ParameterTypeSemanticsTest": "参数类型语义单元测试。",
    "IdGeneratorTest": "主数据 ID 生成器单元测试。",
    "PageSliceTest": "领域分页切片单元测试。",
    "CommandTypeQueryParamTest": "命令类型查询参数解析单元测试。",
    "ConfigChangeTypeControllerWebMvcTest": "变更类型接口 MockMvc 冒烟测试。",
    "CsvDownloadTest": "CSV 下载工具类单元测试。",
    "ParameterSideControllersTest": "参数侧控制器 MockMvc 测试。",
    "TypeBitDictControllerWebMvcTest": "类型 BIT 字典接口 MockMvc 冒烟测试。",
    "UiControllerDelegateBatchTest": "配置类控制器委托批量场景 MockMvc 测试。",
    "ExceptionHandlerBlacklistContractTest": "全局异常处理黑名单契约单元测试。",
    "ChangeSourceBlacklistViolationPayloadTest": "变更来源黑名单违规响应载荷单元测试。",
    "ResponseObjectTest": "统一响应对象单元测试。",
    "ListPageQueryFactoriesTest": "列表分页查询工厂方法单元测试。",
    "ParameterAppCollaboration": "参数应用服务所需领域协作依赖分组。",
    "ParameterAppPersistenceMappers": "参数应用服务所需持久化 Mapper 分组。",
}


def find_type_line(lines: list[str], stem: str) -> int | None:
    pat = re.compile(
        rf"^\s*(?:public\s+)?(?:final\s+)?(?:abstract\s+)?(class|interface|enum|record)\s+{re.escape(stem)}\b"
    )
    for i, line in enumerate(lines):
        if pat.match(line):
            return i
    return None


def find_javadoc_range(lines: list[str], before: int) -> tuple[int, int] | None:
    j = before - 1
    while j >= 0 and lines[j].strip().startswith("@"):
        j -= 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    if j < 0:
        return None
    if lines[j].strip() != "*/":
        if lines[j].strip().startswith("/**") and "*/" in lines[j]:
            return j, j
        return None
    end = j
    k = end
    while k >= 0 and not lines[k].strip().startswith("/**"):
        k -= 1
    if k < 0:
        return None
    return k, end


def build_block(description: str, indent: str = "") -> list[str]:
    return [
        f"{indent}/**",
        f"{indent} * {description}",
        f"{indent} *",
        f"{indent} * @since {DEFAULT_SINCE}",
        f"{indent} */",
    ]


def patch_file(path: Path, stem: str, description: str) -> bool:
    raw = path.read_text(encoding="utf-8")
    lines = raw.splitlines()
    type_line = find_type_line(lines, stem)
    if type_line is None:
        print(f"SKIP no type: {path.name}")
        return False

    insert_at = type_line
    while insert_at > 0 and lines[insert_at - 1].strip().startswith("@"):
        insert_at -= 1

    rng = find_javadoc_range(lines, insert_at)
    if rng is not None:
        start, end = rng
        block_text = "\n".join(lines[start : end + 1])
        if "@since" in block_text and re.search(r"@since\s+\d{4}-\d{2}-\d{2}", block_text):
            desc_ok = any(
                ln.strip().startswith("* ")
                and not ln.strip().startswith("* @")
                and ln.strip() != "*"
                for ln in lines[start + 1 : end]
            )
            if desc_ok:
                return False
        if "@since" not in block_text:
            new_lines = lines[:end] + [f" * @since {DEFAULT_SINCE}"] + lines[end:]
            lines = new_lines
        else:
            for i in range(start + 1, end):
                t = lines[i].strip()
                if t.startswith("* ") and not t.startswith("* @"):
                    lines = lines[:start] + build_block(description, indent_for(lines, start)) + lines[end + 1 :]
                    break
            else:
                lines = lines[: start + 1] + [f" * {description}", " *"] + lines[start + 1 :]
    else:
        indent = ""
        if insert_at > 0:
            m = re.match(r"^(\s*)\S", lines[insert_at])
            if m:
                indent = m.group(1)
        block = build_block(description, indent)
        if insert_at > 0 and lines[insert_at - 1].strip() != "":
            block = [""] + block
        lines = lines[:insert_at] + block + lines[insert_at:]

    new_body = "\n".join(lines) + "\n"
    if new_body != raw:
        path.write_text(new_body, encoding="utf-8", newline="\n")
        return True
    return False


def indent_for(lines: list[str], start: int) -> str:
    ln = lines[start]
    m = re.match(r"^(\s*)/\*\*", ln)
    return m.group(1) if m else ""


def main() -> int:
    touched = 0
    for stem, desc in DESCRIPTIONS.items():
        found = list(ROOT.rglob(f"{stem}.java"))
        if not found:
            print(f"NOT FOUND: {stem}")
            continue
        if patch_file(found[0], stem, desc):
            print(f"PATCHED: {found[0].relative_to(ROOT)}")
            touched += 1
    print(f"Done. Patched {touched} files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
