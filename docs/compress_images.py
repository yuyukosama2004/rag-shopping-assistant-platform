#!/usr/bin/env python3
"""
PhoneMall 图片智能批量压缩
规则: >50KB 才压, 宽度>800px 才缩小, quality=80, 输出为jpg
"""
from PIL import Image
import os, sys

IMG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "biyesheji-frontend", "public", "images")
MAX_WIDTH = 800          # 超过此宽度缩到800
MIN_KB = 50              # 小于此KB数的跳过
QUALITY = 80             # JPEG质量 1-100

def compress_image(filepath: str) -> bool:
    fname = os.path.basename(filepath)
    size_kb = os.path.getsize(filepath) // 1024

    # 太小不压
    if size_kb < MIN_KB:
        print(f"  [SKIP] {fname}: {size_kb}KB 已够小")
        return False

    try:
        img = Image.open(filepath).convert("RGB")
        w, h = img.size

        # 太窄不缩
        if w <= MAX_WIDTH and size_kb < 80:
            print(f"  [SKIP] {fname}: {w}x{h} {size_kb}KB 尺寸和大小都OK")
            return False

        # 缩宽度
        if w > MAX_WIDTH:
            ratio = MAX_WIDTH / w
            new_size = (MAX_WIDTH, int(h * ratio))
            img = img.resize(new_size, Image.LANCZOS)

        # 保存为jpg
        new_path = os.path.splitext(filepath)[0] + ".jpg"
        # 如果原来是png, 保存jpg后删png
        img.save(new_path, "JPEG", quality=QUALITY, optimize=True)
        new_kb = os.path.getsize(new_path) // 1024

        if filepath != new_path:
            os.remove(filepath)  # 删旧文件

        pct = int((1 - new_kb/size_kb) * 100)
        print(f"  [OK] {fname} → {os.path.basename(new_path)}: {size_kb}KB → {new_kb}KB (-{pct}%)")
        return True
    except Exception as e:
        print(f"  [ERR] {fname}: {e}")
        return False

def main():
    if not os.path.isdir(IMG_DIR):
        print(f"目录不存在: {IMG_DIR}")
        sys.exit(1)

    files = [f for f in os.listdir(IMG_DIR) if f.lower().endswith(('.jpg','.jpeg','.png','.webp'))]
    files.sort()

    total_before = sum(os.path.getsize(os.path.join(IMG_DIR, f)) for f in files)
    print(f"共 {len(files)} 张图片, 总大小 {total_before//1024}KB\n")

    skipped = 0; compressed = 0
    for f in files:
        filepath = os.path.join(IMG_DIR, f)
        if compress_image(filepath):
            compressed += 1
        else:
            skipped += 1

    files_after = [f for f in os.listdir(IMG_DIR) if f.lower().endswith(('.jpg','.jpeg','.png','.webp'))]
    total_after = sum(os.path.getsize(os.path.join(IMG_DIR, f)) for f in files_after)
    pct = int((1 - total_after/total_before) * 100) if total_before else 0

    print(f"\n=== 完成 ===")
    print(f"压缩: {compressed} 张, 跳过: {skipped} 张")
    print(f"总大小: {total_before//1024}KB → {total_after//1024}KB (-{pct}%)")

if __name__ == "__main__":
    main()
