import urllib.request

def get(path):
    return urllib.request.urlopen("http://localhost:8080" + path).read().decode("utf-8")

login = get("/login")

print("--- trang /login ---")
checks = [
    ('nút Quên mật khẩu', 'data-dialog-open="tpl-lien-he"'),
    ('template tpl-lien-he', 'id="tpl-lien-he"'),
    ('tiêu đề template', 'data-title="Liên hệ quản trị viên"'),
    ('nhãn Quên mật khẩu?', 'Quên mật khẩu?'),
    ('fragment contact', 'contact-card'),
    ('script ui.js', 'js/ui.js'),
]
for name, c in checks:
    print(("OK   " if c in login else "FAIL "), name)

print("\n--- encoding tiếng Việt trong trang /login ---")
enc = [
    "Quản trị viên hệ thống",
    "Khoa Công nghệ Thông tin",
    "Phòng Hành chính",
    "Tòa nhà A",
    "Sáng: 8:00",
    "thứ 2 đến thứ 6",
    "Vui lòng cung cấp",
    "mã sinh viên",
]
for c in enc:
    print(("OK   " if c in login else "FAIL "), c)

print("\n--- trang /support ---")
sup = get("/support")
for c in ["Phòng Hành chính", "Sáng: 8:00", "thứ 2 đến thứ 6", "Tòa nhà A", "Vui lòng cung cấp"]:
    print(("OK   " if c in sup else "FAIL "), c)
