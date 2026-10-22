import re, pathlib, json, sys

root = pathlib.Path("src/main/resources/templates")
css_dir = pathlib.Path("src/main/resources/static/css")

css_text = "\n".join(p.read_text(encoding="utf-8") for p in css_dir.glob("*.css") if p.name != "style.css")
css_non_theme = "\n".join(p.read_text(encoding="utf-8") for p in css_dir.glob("*.css") if p.name not in ("style.css", "theme.css"))
css_classes = set(re.findall(r"\.([A-Za-z][A-Za-z0-9_-]*)", css_text))

used = {}
inline_styles = []
for p in root.rglob("*.html"):
    t = p.read_text(encoding="utf-8")
    for m in re.finditer(r'class="([^"]*)"', t):
        for c in m.group(1).split():
            if c.startswith("th:") or c.startswith("{"):
                continue
            used.setdefault(c, set()).add(str(p.relative_to(root)).replace("\\", "/"))
    for m in re.finditer(r'style="([^"]*)"', t):
        inline_styles.append((str(p.relative_to(root)).replace("\\", "/"), m.group(1)[:60]))

missing = {k: sorted(v) for k, v in used.items() if k not in css_classes}
print("=== CLASSES USED IN TEMPLATES BUT NOT DEFINED IN NEW CSS ===")
for k in sorted(missing, key=lambda x: -len(missing[x])):
    print(f"  {k}  ({len(missing[k])} files)")
print()
print("=== INLINE style= ATTRIBUTES ===")
from collections import Counter
c = Counter(f"{f} :: {s}" for f, s in inline_styles)
for k, v in c.items():
    print(f"  {v:>3}x  {k}")
print(f"  TOTAL inline style attributes: {len(inline_styles)}")

# hardcoded colors outside theme.css
print()
print("=== HARDCODED COLORS OUTSIDE theme.css ===")
hexes = re.findall(r"#[0-9a-fA-F]{3,8}\b", css_non_theme)
rgb = re.findall(r"rgba?\([^)]*\)", css_non_theme)
print(f"  hex in css (excl theme.css): {len(hexes)}  -> {sorted(set(hexes))[:40]}")
print(f"  rgb() in css (excl theme.css): {len(rgb)}")
for p in list(css_dir.glob("*.css")):
    if p.name == "theme.css":
        continue
    t = p.read_text(encoding="utf-8")
    h = re.findall(r"#[0-9a-fA-F]{6}\b", t)
    r = re.findall(r"rgba?\([^)]*\)", t)
    if h or r:
        print(f"   {p.name}: hex={len(h)} rgb={len(r)}")
        for x in sorted(set(h)):
            print(f"      {x}")
for p in root.rglob("*.html"):
    t = p.read_text(encoding="utf-8")
    h = re.findall(r"#[0-9a-fA-F]{6}\b", t)
    r = re.findall(r"rgba?\([^)]*\)", t)
    if h or r:
        print(f"   HTML {p.relative_to(root)}: hex={sorted(set(h))} rgb={sorted(set(r))}")
