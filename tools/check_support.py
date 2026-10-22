import urllib.request

url = "http://localhost:8080/support"
raw = urllib.request.urlopen(url).read()
text = raw.decode("utf-8")

checks = [
    "Phòng Hành chính",
    "Sáng: 8:00",
    "Vui lòng cung cấp",
    "Quản trị hệ thống",
    "Khoa Công nghệ Thông tin",
    "Trường Đại học",
    "0900000001",
    "admin@demo.local",
    "thứ 2 đến thứ 6",
    "Tòa nhà A",
]
for c in checks:
    print(("OK   " if c in text else "FAIL "), c)
