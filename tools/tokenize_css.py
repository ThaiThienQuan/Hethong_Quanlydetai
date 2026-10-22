import pathlib, re

base = pathlib.Path("src/main/resources/static/css")

repl_common = {
    # layout.css
    "box-shadow: 0 0 0 1px var(--nav-border), 0 28px 60px rgba(2,6,23,.45);":
        "box-shadow: 0 0 0 1px var(--nav-border), 0 28px 60px var(--ink-shadow);",
    "background: rgba(8,12,32,.55);": "background: var(--overlay);",
    "background: radial-gradient(circle at 30% 30%, rgba(99,102,241,.22), transparent 70%);":
        "background: radial-gradient(circle at 30% 30%, var(--nav-glow-1), transparent 70%);",
    "background: radial-gradient(circle at 60% 60%, rgba(6,182,212,.18), transparent 70%);":
        "background: radial-gradient(circle at 60% 60%, var(--nav-glow-2), transparent 70%);",
    "color: #fff;": "color: var(--on-brand);",
    "box-shadow: 0 6px 18px rgba(79,70,229,.45);": "box-shadow: 0 6px 18px var(--primary-glow);",
    "scrollbar-color: rgba(255,255,255,.10) transparent;": "scrollbar-color: var(--on-brand-10) transparent;",
    "box-shadow: 0 4px 14px rgba(79,70,229,.35);": "box-shadow: 0 4px 14px var(--primary-glow);",
    "color: #A5B4FC;": "color: var(--nav-accent);",
    "body { background: #fff; color: #000; }": "body { background: var(--surface); color: var(--text-strong); }",
    # components.css
    ".btn-danger:hover { background: color-mix(in srgb, var(--danger) 86%, #000); color: #fff; }":
        ".btn-danger:hover { background: color-mix(in srgb, var(--danger) 86%, var(--brand-ink)); color: var(--on-brand); }",
    # pages.css
    "background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,.28) 1px, transparent 0);":
        "background-image: radial-gradient(circle at 1px 1px, var(--on-brand-28) 1px, transparent 0);",
    "border: 1px solid rgba(255,255,255,.20);": "border: 1px solid var(--on-brand-20);",
    "box-shadow: 0 0 0 60px rgba(255,255,255,.05), 0 0 0 120px rgba(255,255,255,.035);":
        "box-shadow: 0 0 0 60px var(--on-brand-05), 0 0 0 120px var(--on-brand-035);",
    "color: rgba(255,255,255,.9);": "color: var(--on-brand-90);",
    "background: #fff;\n  color: var(--primary);": "background: var(--on-brand);\n  color: var(--primary);",
    "box-shadow: 0 8px 20px rgba(15,23,42,.22);": "box-shadow: 0 8px 20px var(--ink-shadow);",
    "box-shadow: 0 12px 26px rgba(15,23,42,.28);": "box-shadow: 0 12px 26px var(--ink-shadow);",
    "border: 1px solid rgba(255,255,255,.28);": "border: 1px solid var(--on-brand-28);",
    "background: rgba(255,255,255,.96);": "background: var(--on-brand-96);",
    "box-shadow: 0 20px 44px rgba(15,23,42,.3);": "box-shadow: 0 20px 44px var(--ink-shadow);",
    "box-shadow: 0 12px 24px rgba(15,23,42,.3);": "box-shadow: 0 12px 24px var(--ink-shadow);",
    "linear-gradient(rgba(255,255,255,.07) 1px, transparent 1px),": "linear-gradient(var(--on-brand-07) 1px, transparent 1px),",
    "linear-gradient(90deg, rgba(255,255,255,.07) 1px, transparent 1px);": "linear-gradient(90deg, var(--on-brand-07) 1px, transparent 1px);",
    "background: radial-gradient(circle, rgba(255,255,255,.22), transparent 65%);":
        "background: radial-gradient(circle, var(--on-brand-22), transparent 65%);",
    "color: rgba(255,255,255,.85);": "color: var(--on-brand-85);",
    "background: rgba(255,255,255,.12);": "background: var(--on-brand-12);",
    "color: rgba(255,255,255,.75);": "color: var(--on-brand-75);",
    "color: #A5F3FC;": "color: var(--on-accent);",
    "color: #FDE68A;": "color: var(--brand-amber);",
    "background: #D1FAE5;": "background: var(--success-bg);",
    "color: #047857;": "color: var(--success);",
}

for name in ("layout.css", "components.css", "pages.css"):
    p = base / name
    t = p.read_text(encoding="utf-8")
    for a, b in repl_common.items():
        t = t.replace(a, b)
    p.write_text(t, encoding="utf-8")
    print("updated", name)

# report remaining
for p in sorted(base.glob("*.css")):
    if p.name == "theme.css":
        continue
    t = p.read_text(encoding="utf-8")
    h = re.findall(r"#[0-9a-fA-F]{3,8}\b", t)
    r = re.findall(r"rgba?\([^)]*\)", t)
    print(f"  {p.name}: hex={sorted(set(h))} rgb={sorted(set(r))}")
