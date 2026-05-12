#!/usr/bin/env python3
"""
PhoneMall 商品图片自动下载
从 DuckDuckGo 图片搜索下载每款手机的产品图
"""

from ddgs import DDGS
import mysql.connector
import requests
import os, re, time

# ============================================================
DB_CONFIG = {
    "host": "127.0.0.1", "port": 3306,
    "user": "root", "password": "root123",
    "database": "biyesheji", "charset": "utf8mb4"
}
IMAGE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "biyesheji-frontend", "public", "images")
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}


def download_image(url: str, save_path: str) -> bool:
    try:
        resp = requests.get(url, headers=HEADERS, timeout=15)
        if resp.status_code == 200 and len(resp.content) > 2000:
            os.makedirs(os.path.dirname(save_path), exist_ok=True)
            with open(save_path, "wb") as f:
                f.write(resp.content)
            return True
    except:
        pass
    return False


def main():
    os.makedirs(IMAGE_DIR, exist_ok=True)
    db = mysql.connector.connect(**DB_CONFIG)
    cursor = db.cursor(dictionary=True)
    cursor.execute("SELECT id, name, brand FROM t_product WHERE status=1 AND (main_image IS NULL OR main_image='') ORDER BY id")
    products = cursor.fetchall()

    if not products:
        print("所有商品已有图片，无需下载")
        cursor.close(); db.close(); return

    print(f"共 {len(products)} 款商品需要下载图片\n")

    ddgs = DDGS()
    for i, p in enumerate(products):
        pid, name = p["id"], p["name"]
        query = f"{name} 手机 官方产品图"
        print(f"[{i+1}/{len(products)}] {name} (ID:{pid})")

        try:
            results = list(ddgs.images(query, max_results=5))
        except Exception as e:
            print(f"  搜索失败: {e}")
            continue

        downloaded = False
        for j, r in enumerate(results):
            try:
                img_url = r["image"]
                ext = os.path.splitext(img_url.split("?")[0])[1] or ".jpg"
                if ext not in (".jpg", ".jpeg", ".png", ".webp"):
                    ext = ".jpg"
                # 文件名: Brand_Product_Name.ext
                safe_name = p["name"].replace(' ', '_').replace('+', 'Plus')
                safe_name = re.sub(r'[^\w\-_]', '', safe_name)
                safe_brand = p["brand"].replace(' ', '_').replace('+', 'Plus')
                safe_brand = re.sub(r'[^\w\-_]', '', safe_brand)
                save_name = f"{safe_brand}_{safe_name}{ext}"
                save_path = os.path.join(IMAGE_DIR, save_name)

                if download_image(img_url, save_path):
                    size_kb = os.path.getsize(save_path) // 1024
                    image_path = f"/images/{save_name}"
                    cursor.execute("UPDATE t_product SET main_image=%s WHERE id=%s", (image_path, pid))
                    db.commit()
                    print(f"  OK #{j+1}: {size_kb}KB → {image_path}")
                    downloaded = True
                    break
                else:
                    print(f"  跳过 #{j+1}: 下载失败或太小")
            except Exception as e:
                print(f"  跳过 #{j+1}: {e}")

        if not downloaded:
            print(f"  [失败] 未找到可用图片")

        time.sleep(2.5)

    cursor.close()
    db.close()
    print("\n完成!")


if __name__ == "__main__":
    main()
