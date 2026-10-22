"""
UniResearch – script chuyển đổi hàng loạt template sang layout mới (lần 2).
Đưa skip-link và fragment nav ra khỏi <main> (đúng thứ tự landmark).
"""
import re, pathlib

ROOT = pathlib.Path(r"c:\Users\Khanh\Downloads\Hethong_Quanlydetai-khanh_demo (1)\Hethong_Quanlydetai-ducmanh_demo")
TPL = ROOT / "src" / "main" / "resources" / "templates"

SKIP = {
    "login.html",
    "error/403.html", "error/404.html", "error/500.html",
    "fragments/nav.html", "fragments/layout.html", "fragments/icons.html",
    "fragments/error.html",
}

NAV_RE = re.compile(r'<div\s+th:replace="~\{fragments/nav\s*::\s*nav\}"\s*></div>', re.I)
SKIPLINK_RE = re.compile(r'<a\s+class="skip-link".*?</a>', re.S | re.I)

def main():
    changed = []
    for p in sorted(TPL.rglob("*.html")):
        rel = p.relative_to(TPL).as_posix()
        if rel in SKIP:
            continue
        src = p.read_text(encoding="utf-8")
        orig = src

        m = re.search(r"<body\b[^>]*>", src, re.I)
        if not m:
            continue
        body_start = m.end()
        rest = src[body_start:]
        rest = re.sub(r"</body>\s*</html>\s*$", "", rest, flags=re.I | re.S)

        mm = re.search(r'<main\s+id="main-content"\s+class="app-main">(.*)</main>\s*$', rest, re.S | re.I)
        if not mm:
            continue
        inner = mm.group(1)

        inner = SKIPLINK_RE.sub("", inner)
        inner = NAV_RE.sub("", inner)

        new_body = (
            '\n<a class="skip-link" href="#main-content">Bỏ qua đến nội dung chính</a>\n'
            '<div th:replace="~{fragments/nav :: nav}"></div>\n'
            '<main id="main-content" class="app-main">'
            + inner.strip()
            + "\n</main>\n"
        )
        src = src[:body_start] + new_body + "\n</body>\n</html>\n"
        if src != orig:
            p.write_text(src, encoding="utf-8")
            changed.append(rel)
    print("Đã sửa lại %d template" % len(changed))

if __name__ == "__main__":
    main()
