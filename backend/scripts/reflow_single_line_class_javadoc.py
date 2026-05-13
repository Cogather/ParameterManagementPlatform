#!/usr/bin/env python3
"""Expand one-line /** ... @since yyyy-mm-dd */ class Javadoc into multi-line form."""
from __future__ import annotations

import re
import sys
from pathlib import Path

LINE_JD = re.compile(r"^(\s*)/\*\*\s*(.+?)\s*@since\s+(\d{4}-\d{2}-\d{2})\s*\*/\s*$")


def main() -> int:
    root = Path(__file__).resolve().parents[2]
    java = root / "backend" / "param-service" / "src" / "main" / "java"
    n = 0
    for p in sorted(java.rglob("*.java")):
        lines = p.read_text(encoding="utf-8").splitlines()
        out: list[str] = []
        changed = False
        for line in lines:
            m = LINE_JD.match(line)
            if m:
                ind, body, since = m.group(1), m.group(2).strip(), m.group(3)
                out.append(f"{ind}/**")
                out.append(f"{ind} * {body}")
                out.append(f"{ind} *")
                out.append(f"{ind} * @since {since}")
                out.append(f"{ind} */")
                changed = True
            else:
                out.append(line)
        if changed:
            text = "\n".join(out)
            if not text.endswith("\n"):
                text += "\n"
            p.write_text(text, encoding="utf-8")
            n += 1
    print(f"reflowed {n} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
