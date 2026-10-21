#!/usr/bin/env python3
"""
Tạo các tệp PDF báo cáo mẫu cho dữ liệu demo (HT_QLDT_02_du_lieu_mau.sql).

Cách dùng (chỉ dùng thư viện chuẩn của Python 3):
    python tao_bao_cao_mau.py <thu_muc_luu_tru>
Ví dụ, nếu app.storage.dir=./storage:
    python tao_bao_cao_mau.py ./storage
=> tạo ./storage/demo/KLTN-25-101_v1.pdf ... (đúng giá trị cột reports.stored_path)

Tệp là PDF hợp lệ 1 trang, chỉ chứa chữ không dấu (dữ liệu demo).
"""
import os
import sys

# (tên tệp, dòng chữ hiển thị)
FILES = [
    ("KLTN-25-101_v1.pdf", "KLTN-25-101 - bao cao v1 (DU LIEU DEMO)"),
    ("KLTN-25-101_v2.pdf", "KLTN-25-101 - bao cao v2 (DU LIEU DEMO)"),
    ("KLTN-25-102_v1.pdf", "KLTN-25-102 - bao cao v1 (DU LIEU DEMO)"),
    ("KLTN-25-103_v1.pdf", "KLTN-25-103 - bao cao v1 (DU LIEU DEMO)"),
    ("KLTN-25-104_v1.pdf", "KLTN-25-104 - bao cao v1 (DU LIEU DEMO)"),
    ("TLCN-25-201_v1.pdf", "TLCN-25-201 - bao cao v1 (DU LIEU DEMO)"),
    ("TLCN-25-202_v1.pdf", "TLCN-25-202 - bao cao v1 (DU LIEU DEMO)"),
]


def build_pdf(text: str) -> bytes:
    stream = f"BT /F1 16 Tf 72 720 Td ({text}) Tj ET".encode("latin-1")
    objs = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
        b"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
        b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]
    out = bytearray(b"%PDF-1.4\n")
    offsets = []
    for i, body in enumerate(objs, start=1):
        offsets.append(len(out))
        out += f"{i} 0 obj\n".encode() + body + b"\nendobj\n"
    xref = len(out)
    out += f"xref\n0 {len(objs) + 1}\n".encode() + b"0000000000 65535 f \n"
    for off in offsets:
        out += f"{off:010d} 00000 n \n".encode()
    out += (f"trailer\n<< /Size {len(objs) + 1} /Root 1 0 R >>\n"
            f"startxref\n{xref}\n%%EOF\n").encode()
    return bytes(out)


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    target = os.path.join(sys.argv[1], "demo")
    os.makedirs(target, exist_ok=True)
    for name, text in FILES:
        data = build_pdf(text)
        with open(os.path.join(target, name), "wb") as f:
            f.write(data)
        print(f"{name}: {len(data)} bytes")


if __name__ == "__main__":
    main()
