#!/usr/bin/env python3
"""
PhoneMall 商品数据自动采集脚本
从 中关村在线(ZOL) 爬取: 图片、价格、规格参数、外观颜色、存储配置
"""

import requests
from bs4 import BeautifulSoup
import mysql.connector
import os, re, time, json, urllib.parse

# ============================================================
# 配置
# ============================================================
DB_CONFIG = {
    "host": "127.0.0.1", "port": 3306,
    "user": "root", "password": "root123",
    "database": "biyesheji", "charset": "utf8mb4"
}
IMAGE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "biyesheji-frontend", "public", "images")
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Referer": "https://detail.zol.com.cn/"
}
DELAY = 2  # 请求间隔(秒)，别太快被封


def search_zol(name: str) -> str | None:
    """在 ZOL 搜索产品，返回详情页 URL"""
    q = urllib.parse.quote(name)
    search_url = f"https://detail.zol.com.cn/index.php?c=SearchList&keyword={q}&page=1"
    try:
        resp = requests.get(search_url, headers=HEADERS, timeout=15)
        soup = BeautifulSoup(resp.text, "html.parser")
        # 结果列表第一条
        first = soup.select_one(".list-box .list-item .pro-intro h3 a")
        if first:
            href = first.get("href", "")
            if href:
                return "https://detail.zol.com.cn" + href
        # 备用: 也用 Google style redirect 试一下
        for a in soup.select("a[href*='/cell_phone/index']"):
            href = a.get("href", "")
            if href and "index" in href:
                return "https://detail.zol.com.cn" + href
    except Exception as e:
        print(f"  [搜索失败] {name}: {e}")
    return None


def parse_detail(url: str) -> dict:
    """解析产品详情页，提取价格、描述、图片"""
    info = {"price": None, "image_url": None, "description": ""}
    try:
        resp = requests.get(url, headers=HEADERS, timeout=15)
        soup = BeautifulSoup(resp.text, "html.parser")

        # 价格
        price_el = soup.select_one(".product-price .price-type")
        if price_el:
            m = re.search(r"[\d]+", price_el.text.replace("¥", "").replace("￥", ""))
            if m:
                info["price"] = float(m.group())

        # 主图
        img_el = soup.select_one("#_x_imgsrc0") or soup.select_one(".product-img img") or soup.select_one(".pic img")
        if img_el:
            src = img_el.get("src") or img_el.get("_src") or ""
            if src and src.startswith("//"):
                src = "https:" + src
            if src:
                info["image_url"] = src

        # 描述摘要
        desc_el = soup.select_one(".product-intro .intro-detail") or soup.select_one(".product-intro")
        if desc_el:
            info["description"] = desc_el.get_text(strip=True)[:200]

    except Exception as e:
        print(f"  [解析失败] {url}: {e}")
    return info


def parse_params(url: str) -> dict:
    """解析参数页，提取规格、颜色、存储"""
    info = {"spec": {}, "colors": [], "storages": []}

    # 把 detail url 转换成 param url
    # detail: /cell_phone/index123456.shtml → param: /xxx/123456/param.shtml
    m = re.search(r"index(\d+)", url)
    if not m:
        return info
    pid = m.group(1)
    prefix = str(int(pid[:4]) + 1)
    param_url = f"https://detail.zol.com.cn/{prefix}/{pid}/param.shtml"

    try:
        resp = requests.get(param_url, headers=HEADERS, timeout=15)
        soup = BeautifulSoup(resp.text, "html.parser")

        spec_map = {
            "CPU": "cpu", "处理器": "cpu",
            "屏幕尺寸": "screen", "主屏尺寸": "screen",
            "电池容量": "battery",
            "后置摄像头": "camera", "摄像头": "camera",
            "RAM": "ram", "运行内存": "ram",
            "ROM": "storage", "机身存储": "storage",
            "操作系统": "os", "CPU型号": "cpu_model",
        }

        for row in soup.select(".param-table tr") or soup.select("table tr"):
            cells = row.find_all(["th", "td"])
            if len(cells) >= 2:
                key = cells[0].get_text(strip=True)
                val = cells[1].get_text(strip=True)
                for k, v in spec_map.items():
                    if k in key:
                        info["spec"][v] = val
                        break

        # 颜色
        for row in soup.select("tr"):
            txt = row.get_text(strip=True)
            if "机身颜色" in txt or "外观颜色" in txt:
                colors = re.findall(r"[\u4e00-\u9fff]+色|[\u4e00-\u9fff]+钛|[\u4e00-\u9fff]{1,4}色?", txt)
                info["colors"] = [c for c in colors if len(c) >= 2] or [txt.split("\t")[-1]]

        # 存储
        for row in soup.select("tr"):
            txt = row.get_text(strip=True)
            if "机身存储" in txt or "存储容量" in txt:
                storages = re.findall(r"\d+\s*GB|\d+\s*TB", txt)
                info["storages"] = storages

    except Exception as e:
        print(f"  [参数解析失败] {param_url}: {e}")
    return info


def download_image(url: str, save_path: str) -> bool:
    """下载图片到本地"""
    if not url:
        return False
    try:
        resp = requests.get(url, headers=HEADERS, timeout=20)
        if resp.status_code == 200:
            os.makedirs(os.path.dirname(save_path), exist_ok=True)
            with open(save_path, "wb") as f:
                f.write(resp.content)
            return True
    except Exception as e:
        print(f"  [图片下载失败] {url}: {e}")
    return False


def main():
    os.makedirs(IMAGE_DIR, exist_ok=True)

    db = mysql.connector.connect(**DB_CONFIG)
    cursor = db.cursor(dictionary=True)
    cursor.execute("SELECT id, name, brand FROM t_product WHERE status=1 ORDER BY brand, id")
    products = cursor.fetchall()

    print(f"共 {len(products)} 款商品，开始采集...\n")

    for i, p in enumerate(products):
        pid, name, brand = p["id"], p["name"], p["brand"]
        print(f"[{i+1}/{len(products)}] {name} (ID:{pid})")

        # 1. 搜索 ZOL
        detail_url = search_zol(name)
        if not detail_url:
            print(f"  [跳过] 未找到 ZOL 页面")
            continue

        # 2. 解析详情页（价格 + 图片）
        detail = parse_detail(detail_url)
        print(f"  价格: {detail['price']}  图片: {detail['image_url'][:60] if detail['image_url'] else '无'}")

        # 3. 解析参数页（规格 + 颜色 + 存储）
        params = parse_params(detail_url)
        print(f"  规格: {len(params['spec'])}项  颜色: {params['colors'][:3]}  存储: {params['storages']}")

        # 4. 下载图片
        if detail["image_url"]:
            ext = os.path.splitext(detail["image_url"].split("?")[0])[1] or ".jpg"
            save_name = f"{pid}{ext}"
            save_path = os.path.join(IMAGE_DIR, save_name)
            ok = download_image(detail["image_url"], save_path)
            if ok:
                print(f"  图片已保存: images/{save_name}")
                # 更新数据库 main_image
                image_path = f"/images/{save_name}"
                cursor.execute("UPDATE t_product SET main_image=%s WHERE id=%s", (image_path, pid))
            else:
                print(f"  [图片下载失败]")

        # 5. 更新价格
        if detail["price"] and detail["price"] > 0:
            # ZOL 参考报价作为 price, 如果有更高报价作为 original_price
            cursor.execute("SELECT price FROM t_product WHERE id=%s", (pid,))
            old = cursor.fetchone()
            old_price = old["price"] if old else 0
            cursor.execute(
                "UPDATE t_product SET price=%s, original_price=%s WHERE id=%s",
                (detail["price"], max(old_price, detail["price"]), pid)
            )

        # 6. 更新规格 JSON
        if params["spec"]:
            cursor.execute("UPDATE t_product SET spec_json=%s WHERE id=%s",
                           (json.dumps(params["spec"], ensure_ascii=False), pid))

        # 7. 更新颜色选项
        if params["colors"]:
            cursor.execute("UPDATE t_product SET color_options=%s WHERE id=%s",
                           (json.dumps(params["colors"][:6], ensure_ascii=False), pid))

        # 8. 更新存储选项
        if params["storages"]:
            cursor.execute("UPDATE t_product SET storage_options=%s WHERE id=%s",
                           (json.dumps(params["storages"][:5], ensure_ascii=False), pid))

        db.commit()
        print()

        time.sleep(DELAY)

    cursor.close()
    db.close()
    print("全部完成!")


if __name__ == "__main__":
    main()
