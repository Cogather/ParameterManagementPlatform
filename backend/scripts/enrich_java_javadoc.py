#!/usr/bin/env python3
"""
Ensure top-level public types have class Javadoc with @since,
and public/protected members have Javadoc (description + @param/@return where applicable).

Run from repo root or backend:
  python backend/scripts/enrich_java_javadoc.py [--dry-run]
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


TYPE_LINE = re.compile(
    r"^\s*public\s+(?:sealed\s+|non-sealed\s+)?(?:abstract\s+|strictfp\s+)?(?:final\s+)?"
    r"(class|interface|enum|record)\s+(\w+)\b"
)

ANN_LINE = re.compile(r"^\s*@\w")

# Single-line method/ctor declaration (interface abstract or class member), conservative.
SINGLE_SIG = re.compile(
    r"^\s*(?:(?:@\w+(?:\([^)]*\))?)\s+)*(?:(?:public|protected)\s+)?"
    r"(?:static\s+)?(?:final\s+)?(?:default\s+)?(?:synchronized\s+)?"
    r"(?:<[^>]+>\s+)?"
    r"([\w.<>\[\],\s?]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w.,\s]+)?\s*(?:;|\{)\s*$"
)

FIELD_LINE = re.compile(
    r"^\s*(?:(?:@\w+(?:\([^)]*\))?)\s+)*(?:(?:public|protected)\s+)?"
    r"(?:static\s+)?(?:final\s+)?(?:transient\s+)?(?:volatile\s+)?"
    r"(?:<[^>]+>\s+)?([\w.<>\[\],\s?]+)\s+(\w+)\s*(?:=|;)\s*"
)


def repo_root() -> Path:
    p = Path(__file__).resolve()
    if p.parent.name == "scripts" and p.parent.parent.name == "backend":
        return p.parent.parent.parent
    return Path.cwd()


def git_first_commit_date(path: Path, cwd: Path) -> str:
    try:
        cp = subprocess.run(
            ["git", "log", "--diff-filter=A", "--format=%cs", "-1", "--", str(path)],
            cwd=cwd,
            capture_output=True,
            text=True,
            check=False,
        )
        out = (cp.stdout or "").strip()
        if re.match(r"^\d{4}-\d{2}-\d{2}$", out):
            return out
    except OSError:
        pass
    return "2026-05-13"


def describe_type(name: str, kind: str) -> str:
    if kind == "record":
        return f"不可变承载对象「{name}」（record）。"
    if kind == "enum":
        return f"枚举类型「{name}」。"
    if kind == "interface":
        if name.endswith("Repository"):
            return f"领域仓储接口「{name}」，定义聚合持久化契约。"
        if name.endswith("Mapper"):
            return f"MyBatis-Plus Mapper 接口「{name}」。"
        return f"接口「{name}」。"
    # class
    if name.endswith("Controller"):
        return f"REST 控制器「{name}」，对外提供 HTTP API。"
    if name.endswith("AppService"):
        return f"应用服务「{name}」，编排用例与事务边界。"
    if name.endswith("DomainService"):
        return f"领域服务「{name}」，承载领域规则与计算。"
    if name.endswith("Assembler"):
        return f"装配器「{name}」，在领域对象与持久化 PO 之间转换。"
    if name.endswith("Po"):
        return f"持久化实体「{name}」，映射数据库表结构。"
    if name.endswith("Config"):
        return f"Spring 配置类「{name}」。"
    if name.endswith("Exception"):
        return f"异常类型「{name}」。"
    if name == "ParamApplication":
        return "Spring Boot 应用入口，负责组件扫描与启动。"
    if name.endswith("Application"):
        return f"应用启动类「{name}」。"
    if name in ("CommonConst",):
        return "通用常量定义。"
    if name.endswith("Helper") or name.endswith("Instructions"):
        return f"工具/说明类「{name}」。"
    if name.endswith("Dictionary") or name.endswith("Collector") or name.endswith("Defaults"):
        return f"应用支撑类型「{name}」。"
    if name.endswith("Query") or name.endswith("Request") or name.endswith("Payload") or name.endswith("Key"):
        return f"请求/查询视图对象「{name}」。"
    if name.endswith("Data") or name.endswith("Response") or name.endswith("Item") or name.endswith("Node"):
        return f"响应/数据传输对象「{name}」。"
    return f"类型「{name}」，承载业务实现与数据表达。"


def javadoc_block_contains_since(lines: list[str], jd_start: int, jd_end: int) -> bool:
    chunk = "\n".join(lines[jd_start : jd_end + 1])
    return "@since" in chunk


def find_class_javadoc_insert_index(lines: list[str], type_line: int) -> int:
    """Index where the type-level Javadoc block should start (before leading annotations, else before type line)."""
    t = type_line - 1
    ann_top: int | None = None
    while t >= 0:
        st = lines[t].strip()
        if st == "":
            t -= 1
            continue
        if ANN_LINE.match(lines[t]):
            ann_top = t
            t -= 1
            continue
        break
    return ann_top if ann_top is not None else type_line


def ensure_class_javadoc(lines: list[str], type_line: int, since: str) -> list[str] | None:
    kind_m = TYPE_LINE.match(lines[type_line])
    if not kind_m:
        return None
    kind, name = kind_m.group(1), kind_m.group(2)
    desc = describe_type(name, kind)

    insert_at = find_class_javadoc_insert_index(lines, type_line)

    j = insert_at - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1

    if j >= 0 and lines[j].strip().endswith("*/"):
        jd_end = j
        k = jd_end
        jd_start = -1
        while k >= 0:
            if lines[k].strip().startswith("/**"):
                jd_start = k
                break
            k -= 1
        if jd_start == -1:
            return None
        if javadoc_block_contains_since(lines, jd_start, jd_end):
            return None
        if jd_start == jd_end:
            orig = lines[jd_end]
            if "*/" in orig:
                without_close = orig.rsplit("*/", 1)[0].rstrip()
                merged = without_close + f" @since {since} */"
                return lines[:jd_end] + [merged] + lines[jd_end + 1 :]
            return None
        star_prefix = " *"
        for li in range(jd_start + 1, jd_end + 1):
            mm = re.match(r"^(\s*\*)", lines[li])
            if mm and not lines[li].strip().startswith("/**"):
                star_prefix = mm.group(1)
                break
        since_line = star_prefix + " @since " + since
        return lines[:jd_end] + [since_line] + lines[jd_end:]

    block = [
        "/**",
        f" * {desc}",
        " *",
        f" * @since {since}",
        " */",
        "",
    ]
    return lines[:insert_at] + block + lines[insert_at:]


def return_description_for_type(ret: str) -> str:
    r = ret.strip()
    if r == "void":
        return "无"
    if r in ("boolean", "Boolean"):
        return "布尔结果"
    if r == "int" or r == "Integer" or r == "long" or r == "Long":
        return "数值结果"
    if "ResponseObject" in r:
        return "统一响应体"
    if "PageResponse" in r:
        return "分页响应"
    if "List<" in r or r.endswith("[]"):
        return "列表数据"
    if "Optional<" in r:
        return "可选结果"
    if r.startswith("ResponseEntity"):
        return "HTTP 响应"
    return "结果"


def parse_params(param_blob: str) -> list[tuple[str, str]]:
    if not param_blob.strip():
        return []
    parts: list[str] = []
    depth_angle = 0
    depth_paren = 0
    cur: list[str] = []
    i = 0
    s = param_blob.strip()
    while i < len(s):
        ch = s[i]
        if ch == "<":
            depth_angle += 1
        elif ch == ">":
            depth_angle = max(0, depth_angle - 1)
        elif ch == "(":
            depth_paren += 1
        elif ch == ")":
            depth_paren = max(0, depth_paren - 1)
        elif ch == "," and depth_angle == 0 and depth_paren == 0:
            parts.append("".join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(ch)
        i += 1
    if cur:
        parts.append("".join(cur).strip())
    out: list[tuple[str, str]] = []
    for p in parts:
        pm = re.search(r"(\w+)\s*$", p)
        if pm:
            out.append((pm.group(1), p))
        else:
            out.append(("arg", p))
    return out


def build_member_javadoc(ret_type: str, name: str, params: str, is_ctor: bool) -> list[str]:
    ps = parse_params(params)
    if is_ctor:
        lines = ["/**", f" * 构造「{name}」。", " *"]
    else:
        lines = ["/**", f" * {name}。", " *"]
    for pname, full in ps:
        lines.append(f" * @param {pname} 见方法签名")
    if not is_ctor and ret_type.strip() != "void":
        lines.append(f" * @return {return_description_for_type(ret_type)}")
    lines.extend([" */", ""])
    return lines


def preceding_has_javadoc(lines: list[str], idx: int) -> bool:
    j = idx - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    while j >= 0 and lines[j].strip().startswith("@"):
        j -= 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    if j < 0:
        return False
    if not lines[j].strip().endswith("*/"):
        return False
    k = j
    while k >= 0:
        if lines[k].strip().startswith("/**"):
            return True
        k -= 1
    return False


def should_skip_member_line(line: str) -> bool:
    s = line.strip()
    if s.startswith("//"):
        return True
    if "class " in s and s.index("class ") < s.index("(") if "(" in s else False:
        return True
    return False


def enrich_members(lines: list[str], type_line: int, is_interface: bool) -> list[str] | None:
    # find opening brace of type
    brace_idx = type_line
    while brace_idx < len(lines) and "{" not in lines[brace_idx]:
        brace_idx += 1
    if brace_idx >= len(lines):
        return None
    # track depth from first { of type; rough: count { } per line in substring
    out = lines[:]
    depth = 0
    started = False
    i = type_line
    changes: list[tuple[int, list[str]]] = []  # insert list of lines before index i

    while i < len(out):
        line = out[i]
        if not started:
            if "{" in line:
                started = True
                depth += line.count("{") - line.count("}")
            i += 1
            continue
        depth += line.count("{") - line.count("}")
        if depth == 0:
            break

        if depth == 1 and not should_skip_member_line(line):
            stripped = line.strip()
            # skip obvious non-members
            if stripped.startswith("public static void main"):
                i += 1
                continue

            m = SINGLE_SIG.match(line)
            if m and not preceding_has_javadoc(out, i):
                ret, name, params = m.group(1), m.group(2), m.group(3)
                type_m = TYPE_LINE.match(out[type_line])
                type_name = type_m.group(2) if type_m else ""
                is_ctor = name == type_name
                if not is_interface and not (
                    line.strip().startswith("public") or line.strip().startswith("protected")
                ):
                    i += 1
                    continue

                jd = build_member_javadoc(ret, name, params, is_ctor)
                changes.append((i, jd))
            else:
                fm = FIELD_LINE.match(line)
                if fm and (line.strip().startswith("public") or line.strip().startswith("protected")):
                    if not preceding_has_javadoc(out, i):
                        fname = fm.group(2)
                        jd = [
                            "/**",
                            f" * 字段「{fname}」。",
                            " */",
                            "",
                        ]
                        changes.append((i, jd))
        i += 1

    if not changes:
        return None
    # apply inserts bottom-up
    for idx, block in sorted(changes, key=lambda x: -x[0]):
        out = out[:idx] + block + out[idx:]
    return out


def process_file(path: Path, cwd: Path, dry: bool) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    type_line = -1
    for i, line in enumerate(lines):
        if TYPE_LINE.match(line):
            type_line = i
            break
    if type_line < 0:
        return False

    kind_m = TYPE_LINE.match(lines[type_line])
    is_interface = kind_m and kind_m.group(1) == "interface"

    since = git_first_commit_date(path, cwd)
    class_patched = ensure_class_javadoc(lines, type_line, since)
    if class_patched is not None:
        lines = class_patched

    type_line = -1
    for i, line in enumerate(lines):
        if TYPE_LINE.match(line):
            type_line = i
            break

    member_lines = enrich_members(lines, type_line, is_interface)
    if member_lines is not None:
        lines = member_lines

    new_text = "\n".join(lines)
    if new_text.endswith("\n") or text.endswith("\n"):
        if not new_text.endswith("\n"):
            new_text += "\n"
    if new_text == text:
        return False
    if dry:
        print(f"[dry-run] would update {path}")
        return True
    path.write_text(new_text, encoding="utf-8")
    print(f"updated {path}")
    return True


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    root = repo_root()
    java_dir = root / "backend" / "param-service" / "src" / "main" / "java"
    if not java_dir.is_dir():
        print("missing", java_dir, file=sys.stderr)
        return 1
    n = 0
    for p in sorted(java_dir.rglob("*.java")):
        if process_file(p, cwd=root, dry=args.dry_run):
            n += 1
    print(f"files touched: {n}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
