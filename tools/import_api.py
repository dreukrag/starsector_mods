#!/usr/bin/env python3
"""Import the Starsector API (starfarer.api) into this repo as a local knowledge base.

Source: the game ships its API as Java source with Javadoc at
    <Starsector install>/starsector-core/starfarer.api.zip
(the same content published at https://fractalsoftworks.com/starfarer.api/).

Usage:
    python3 tools/import_api.py /path/to/starfarer.api.zip --version 0.98a-RC8
    python3 tools/import_api.py /path/to/extracted/src/dir --version 0.98a-RC8

Output (replaces any previous import):
    reference/starfarer-api/VERSION
    reference/starfarer-api/INDEX.md          one line per type: kind, FQN, path, summary
    reference/starfarer-api/src/com/fs/...    the .java sources, unchanged
"""
import argparse
import re
import shutil
import sys
import zipfile
from pathlib import Path, PurePosixPath

REPO = Path(__file__).resolve().parent.parent
OUT = REPO / "reference" / "starfarer-api"

TYPE_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*"
    r"(?:(?:public|protected|private|abstract|static|final|sealed|non-sealed|strictfp)\s+)*"
    r"(class|interface|enum|@interface|record)\s+(\w+)",
    re.MULTILINE,
)
JAVADOC_RE = re.compile(r"/\*\*(.*?)\*/", re.DOTALL)
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)


def iter_java(source: Path):
    """Yield (relative posix path under the package root, bytes) for every .java file."""
    if source.is_file() and zipfile.is_zipfile(source):
        with zipfile.ZipFile(source) as zf:
            for info in zf.infolist():
                name = PurePosixPath(info.filename)
                if info.is_dir() or name.suffix != ".java" or ".." in name.parts:
                    continue
                yield name, zf.read(info)
    elif source.is_dir():
        for p in sorted(source.rglob("*.java")):
            yield PurePosixPath(p.relative_to(source).as_posix()), p.read_bytes()
    else:
        sys.exit(f"error: {source} is neither a zip file nor a directory")


def summary_of(text: str, type_match: re.Match) -> str:
    """First sentence of the Javadoc immediately preceding the type declaration."""
    docs = [m for m in JAVADOC_RE.finditer(text, 0, type_match.start())]
    if not docs:
        return ""
    doc = docs[-1]
    between = text[doc.end():type_match.start()]
    if re.search(r"[;{}]", between):  # doc belongs to something else
        return ""
    body = re.sub(r"^\s*\*\s?", "", doc.group(1), flags=re.MULTILINE)
    body = body.split("\n@", 1)[0]                      # drop @author/@see tags
    body = re.sub(r"\{@\w+\s+([^}]*)\}", r"\1", body)   # {@link X} -> X
    body = re.sub(r"<[^>]+>", "", body)                  # strip HTML
    body = " ".join(body.split())
    m = re.match(r"(.+?[.!?])(\s|$)", body)
    s = m.group(1) if m else body
    return (s[:200] + "…") if len(s) > 200 else s


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source", type=Path, help="starfarer.api.zip or a directory of extracted sources")
    ap.add_argument("--version", required=True, help="Starsector version the API comes from, e.g. 0.98a-RC8")
    args = ap.parse_args()

    files = list(iter_java(args.source))
    if not files:
        sys.exit("error: no .java files found")

    # Re-root at the package root so paths look like com/fs/starfarer/api/...
    rows = []
    staged = []
    for rel, data in files:
        text = data.decode("utf-8", errors="replace")
        pkg = PACKAGE_RE.search(text)
        pkg_name = pkg.group(1) if pkg else ""
        dest = PurePosixPath(*pkg_name.split("."), rel.name) if pkg_name else rel
        staged.append((dest, data))
        m = TYPE_RE.search(text)
        kind, name = (m.group(1), m.group(2)) if m else ("?", rel.stem)
        fqn = f"{pkg_name}.{name}" if pkg_name else name
        rows.append((pkg_name, name, kind, fqn, f"src/{dest}", summary_of(text, m) if m else ""))

    if OUT.exists():
        shutil.rmtree(OUT)
    for dest, data in staged:
        p = OUT / "src" / dest
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(data)

    (OUT / "VERSION").write_text(args.version.strip() + "\n")

    rows.sort()
    lines = [
        f"# starfarer.api index — Starsector {args.version}",
        "",
        f"{len(rows)} types. Format: `kind FQN` — summary → path (relative to this file).",
        "Grep this file to find a type, then open its source for full signatures and Javadoc.",
        "",
    ]
    current = None
    for pkg_name, name, kind, fqn, path, summ in rows:
        if pkg_name != current:
            current = pkg_name
            lines += ["", f"## {pkg_name or '(default package)'}", ""]
        lines.append(f"- `{kind} {fqn}`" + (f" — {summ}" if summ else "") + f" → [{path}]({path})")
    (OUT / "INDEX.md").write_text("\n".join(lines) + "\n")

    print(f"imported {len(rows)} types into {OUT.relative_to(REPO)} (version {args.version})")


if __name__ == "__main__":
    main()
