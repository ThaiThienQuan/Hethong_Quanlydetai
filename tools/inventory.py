import re, pathlib, json

root = pathlib.Path("src/main/java/vn/edu/demo/topicmanagement/controller")
tpl = pathlib.Path("src/main/resources/templates")

routes = []
for f in sorted(root.glob("*.java")):
    src = f.read_text(encoding="utf-8")
    cls_m = re.search(r'@RequestMapping\("([^"]*)"\)\s*\npublic class', src)
    base = cls_m.group(1) if cls_m else ""
    # method-level mappings with their method
    for m in re.finditer(
        r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?'
        r'(?:[^@]|\n)*?public\s+\S+\s+\w+\s*\(([^)]*)\)\s*\{',
        src,
    ):
        verb = m.group(1).upper()
        path = m.group(2) or ""
        sig = m.group(3)
        ret = re.search(r'return\s+"([^"]+)"', src[m.end() - 1 : m.end() + 900])
        routes.append({
            "file": f.name,
            "class_base": base,
            "verb": verb,
            "path": (base + path) or "/",
            "template": ret.group(1) if ret else "",
            "sig": " ".join(sig.split())[:90],
        })

# PreAuthorize annotations per controller
perms = {}
for f in sorted(root.glob("*.java")):
    src = f.read_text(encoding="utf-8")
    for m in re.finditer(r'@PreAuthorize\("([^"]*)"\)', src):
        perms.setdefault(f.name, set()).add(m.group(1))

print(json.dumps({"routes": routes, "perms": {k: sorted(v) for k, v in perms.items()}}, ensure_ascii=False, indent=1))
print("\n=== TEMPLATES ===")
for p in sorted(tpl.rglob("*.html")):
    print(" ", p.relative_to(tpl).as_posix())
