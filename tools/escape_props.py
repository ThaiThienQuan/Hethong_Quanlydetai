"""Sinh escape \\uXXXX cho các giá trị tiếng Việt trong application.properties.

Spring Boot đọc file .properties theo ISO-8859-1 (chuẩn java.util.Properties),
nên ký tự UTF-8 tiếng Việt bị hỏng. Dùng escape \\uXXXX là cách chuẩn của
java.util.Properties và OriginTrackedPropertiesLoader của Spring Boot hỗ trợ.
"""
import re
import pathlib

PROPS = pathlib.Path("src/main/resources/application.properties")

VALUES = [
    "Khoa Công nghệ Thông tin",
    "Phòng Hành chính - Tầng 2, Tòa nhà A, Trường Đại học",
    "Sáng: 8:00 - 11:30, Chiều: 14:00 - 17:00 (thứ 2 đến thứ 6)",
    "Vui lòng cung cấp tên đăng nhập và mã sinh viên / mã giảng viên để Admin tra cứu nhanh hơn.",
]


def esc(s: str) -> str:
    return "".join(c if ord(c) < 128 else "\\u%04x" % ord(c) for c in s)


text = PROPS.read_text(encoding="utf-8")
for v in VALUES:
    if v in text:
        text = text.replace(v, esc(v))
        print("escaped:", v)
    else:
        print("NOT FOUND:", v)

PROPS.write_text(text, encoding="utf-8")
print("done")
