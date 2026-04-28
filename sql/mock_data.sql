-- =====================================================
-- 手机电商平台 模拟数据
-- 30款主流手机的模拟数据（价格、参数参考2024-2025市场）
-- =====================================================

USE biyesheji;

-- =====================================================
-- 商品数据
-- =====================================================
INSERT INTO t_product (id, name, brand, category, price, original_price, spec_json, main_image, images, description, sales, status) VALUES
(1001, 'iPhone 16 Pro Max', 'Apple', '智能手机', 9999.00, 9999.00,
 '{"cpu":"A18 Pro","screen":"6.9英寸 OLED 120Hz","battery":"4676mAh","camera":"4800万三摄","ram":"8GB","storage":"256GB","os":"iOS 18","weight":"227g","charger":"45W有线+25W无线"}',
 'https://picsum.photos/seed/iphone16pm/400/400', '["https://picsum.photos/seed/iphone16pm1/400/400","https://picsum.photos/seed/iphone16pm2/400/400"]',
 'iPhone 16 Pro Max，搭载A18 Pro芯片，4800万像素融合式摄像头系统，钛金属设计，支持USB-C。', 2560, 1),

(1002, 'iPhone 16 Pro', 'Apple', '智能手机', 7999.00, 7999.00,
 '{"cpu":"A18 Pro","screen":"6.3英寸 OLED 120Hz","battery":"3582mAh","camera":"4800万三摄","ram":"8GB","storage":"128GB","os":"iOS 18","weight":"199g","charger":"45W有线+25W无线"}',
 'https://picsum.photos/seed/iphone16p/400/400', '["https://picsum.photos/seed/iphone16p1/400/400","https://picsum.photos/seed/iphone16p2/400/400"]',
 'iPhone 16 Pro，专业级影像系统，更小尺寸的旗舰选择。', 3200, 1),

(1003, 'iPhone 16', 'Apple', '智能手机', 5999.00, 5999.00,
 '{"cpu":"A18","screen":"6.1英寸 OLED 60Hz","battery":"3561mAh","camera":"4800万双摄","ram":"8GB","storage":"128GB","os":"iOS 18","weight":"170g","charger":"30W有线+15W无线"}',
 'https://picsum.photos/seed/iphone16/400/400', '["https://picsum.photos/seed/iphone161/400/400","https://picsum.photos/seed/iphone162/400/400"]',
 'iPhone 16，灵动岛，A18芯片，4800万像素双摄系统。', 5200, 1),

(1004, 'Samsung Galaxy S25 Ultra', 'Samsung', '智能手机', 9699.00, 10199.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.8英寸 Dynamic AMOLED 120Hz","battery":"5000mAh","camera":"2亿像素四摄","ram":"12GB","storage":"256GB","os":"One UI 7 (Android 15)","weight":"219g","charger":"45W有线"}',
 'https://picsum.photos/seed/s25ultra/400/400', '["https://picsum.photos/seed/s25ultra1/400/400","https://picsum.photos/seed/s25ultra2/400/400"]',
 '三星Galaxy S25 Ultra，AI智能旗舰，S Pen内置，2亿像素长焦。', 1890, 1),

(1005, 'Samsung Galaxy S25+', 'Samsung', '智能手机', 7499.00, 7999.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.7英寸 Dynamic AMOLED 120Hz","battery":"4900mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"One UI 7 (Android 15)","weight":"196g","charger":"45W有线"}',
 'https://picsum.photos/seed/s25plus/400/400', '["https://picsum.photos/seed/s25plus1/400/400","https://picsum.photos/seed/s25plus2/400/400"]',
 '三星Galaxy S25+，大屏旗舰，顶级的Dynamic AMOLED屏幕。', 980, 1),

(1006, 'Xiaomi 15 Pro', 'Xiaomi', '智能手机', 5299.00, 5499.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.73英寸 LTPO AMOLED 120Hz","battery":"4880mAh","camera":"徕卡5000万三摄","ram":"12GB","storage":"256GB","os":"HyperOS 2.0","weight":"213g","charger":"120W有线+50W无线"}',
 'https://picsum.photos/seed/mi15pro/400/400', '["https://picsum.photos/seed/mi15pro1/400/400","https://picsum.photos/seed/mi15pro2/400/400"]',
 '小米15 Pro，徕卡光学镜头，骁龙8 Gen 4，120W超级快充。', 3450, 1),

(1007, 'Xiaomi 15', 'Xiaomi', '智能手机', 3999.00, 4299.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.36英寸 LTPO AMOLED 120Hz","battery":"4610mAh","camera":"徕卡5000万三摄","ram":"12GB","storage":"256GB","os":"HyperOS 2.0","weight":"191g","charger":"90W有线+50W无线"}',
 'https://picsum.photos/seed/mi15/400/400', '["https://picsum.photos/seed/mi151/400/400","https://picsum.photos/seed/mi152/400/400"]',
 '小米15，小屏旗舰标杆，徕卡影像，手感出众。', 6100, 1),

(1008, 'Redmi K80 Pro', 'Redmi', '智能手机', 3299.00, 3599.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.67英寸 OLED 120Hz","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"HyperOS","weight":"208g","charger":"120W有线"}',
 'https://picsum.photos/seed/k80pro/400/400', '["https://picsum.photos/seed/k80pro1/400/400","https://picsum.photos/seed/k80pro2/400/400"]',
 'Redmi K80 Pro，极致性价比，骁龙8 Gen 3，120W闪充。', 7200, 1),

(1009, 'Redmi Note 14 Pro+', 'Redmi', '智能手机', 1899.00, 2099.00,
 '{"cpu":"骁龙7s Gen 3","screen":"6.67英寸 AMOLED 120Hz","battery":"5100mAh","camera":"2亿像素三摄","ram":"8GB","storage":"256GB","os":"HyperOS","weight":"190g","charger":"67W有线"}',
 'https://picsum.photos/seed/note14pro/400/400', '["https://picsum.photos/seed/note14pro1/400/400","https://picsum.photos/seed/note14pro2/400/400"]',
 'Redmi Note 14 Pro+，千元机皇，2亿像素影像，大电池。', 12500, 1),

(1010, 'Huawei Mate 70 Pro', 'Huawei', '智能手机', 6999.00, 6999.00,
 '{"cpu":"麒麟9100","screen":"6.82英寸 LTPO OLED 120Hz","battery":"5500mAh","camera":"5000万XMAGE四摄","ram":"12GB","storage":"256GB","os":"HarmonyOS NEXT","weight":"221g","charger":"100W有线+80W无线"}',
 'https://picsum.photos/seed/mate70pro/400/400', '["https://picsum.photos/seed/mate70pro1/400/400","https://picsum.photos/seed/mate70pro2/400/400"]',
 '华为Mate 70 Pro，麒麟9100，XMAGE影像，卫星通信。', 2800, 1),

(1011, 'Huawei Pura 80 Pro', 'Huawei', '智能手机', 6499.00, 6299.00,
 '{"cpu":"麒麟9100","screen":"6.8英寸 LTPO OLED 120Hz","battery":"5200mAh","camera":"5000万超聚光四摄","ram":"12GB","storage":"256GB","os":"HarmonyOS NEXT","weight":"205g","charger":"100W有线+80W无线"}',
 'https://picsum.photos/seed/pura80pro/400/400', '["https://picsum.photos/seed/pura80pro1/400/400","https://picsum.photos/seed/pura80pro2/400/400"]',
 '华为Pura 80 Pro，超聚光影像，艺术美学设计。', 1650, 1),

(1012, 'Huawei nova 14 Pro', 'Huawei', '智能手机', 3499.00, 3699.00,
 '{"cpu":"麒麟8200","screen":"6.7英寸 OLED 120Hz","battery":"4800mAh","camera":"5000万双摄","ram":"8GB","storage":"256GB","os":"HarmonyOS NEXT","weight":"186g","charger":"100W有线"}',
 'https://picsum.photos/seed/nova14pro/400/400', '["https://picsum.photos/seed/nova14pro1/400/400","https://picsum.photos/seed/nova14pro2/400/400"]',
 '华为nova 14 Pro，轻薄自拍旗舰，超感光前置。', 4200, 1),

(1013, 'OPPO Find X8 Pro', 'OPPO', '智能手机', 5999.00, 5999.00,
 '{"cpu":"天玑9400","screen":"6.78英寸 LTPO AMOLED 120Hz","battery":"5000mAh","camera":"哈苏5000万四摄","ram":"12GB","storage":"256GB","os":"ColorOS 15","weight":"215g","charger":"100W有线+50W无线"}',
 'https://picsum.photos/seed/findx8pro/400/400', '["https://picsum.photos/seed/findx8pro1/400/400","https://picsum.photos/seed/findx8pro2/400/400"]',
 'OPPO Find X8 Pro，哈苏人像大师，双潜望长焦。', 1450, 1),

(1014, 'OPPO Reno 14 Pro+', 'OPPO', '智能手机', 3299.00, 3499.00,
 '{"cpu":"天玑8300","screen":"6.7英寸 AMOLED 120Hz","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"ColorOS 15","weight":"184g","charger":"80W有线"}',
 'https://picsum.photos/seed/reno14pro/400/400', '["https://picsum.photos/seed/reno14pro1/400/400","https://picsum.photos/seed/reno14pro2/400/400"]',
 'OPPO Reno 14 Pro+，人像摄影专家，轻薄时尚。', 3500, 1),

(1015, 'vivo X200 Pro', 'vivo', '智能手机', 5999.00, 5999.00,
 '{"cpu":"天玑9400","screen":"6.78英寸 LTPO AMOLED 120Hz","battery":"5000mAh","camera":"蔡司2亿像素APO长焦","ram":"12GB","storage":"256GB","os":"OriginOS 5","weight":"208g","charger":"100W有线+50W无线"}',
 'https://picsum.photos/seed/x200pro/400/400', '["https://picsum.photos/seed/x200pro1/400/400","https://picsum.photos/seed/x200pro2/400/400"]',
 'vivo X200 Pro，蔡司超级长焦，2亿像素APO镜头，演唱会神器。', 2100, 1),

(1016, 'vivo S20 Pro', 'vivo', '智能手机', 3499.00, 3699.00,
 '{"cpu":"天玑8300","screen":"6.78英寸 AMOLED 120Hz","battery":"5000mAh","camera":"5000万柔光三摄","ram":"8GB","storage":"256GB","os":"OriginOS 5","weight":"186g","charger":"80W有线"}',
 'https://picsum.photos/seed/s20pro/400/400', '["https://picsum.photos/seed/s20pro1/400/400","https://picsum.photos/seed/s20pro2/400/400"]',
 'vivo S20 Pro，前置柔光自拍，人像自然美颜。', 2900, 1),

(1017, 'iQOO 14', 'iQOO', '智能手机', 4299.00, 4599.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.78英寸 AMOLED 144Hz","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"OriginOS 5","weight":"207g","charger":"120W有线"}',
 'https://picsum.photos/seed/iqoo14/400/400', '["https://picsum.photos/seed/iqoo141/400/400","https://picsum.photos/seed/iqoo142/400/400"]',
 'iQOO 14，电竞性能旗舰，144Hz高刷屏，120W闪充。', 4300, 1),

(1018, 'OnePlus 14', 'OnePlus', '智能手机', 4999.00, 5299.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.82英寸 LTPO AMOLED 120Hz","battery":"5400mAh","camera":"哈苏5000万三摄","ram":"12GB","storage":"256GB","os":"OxygenOS 15","weight":"210g","charger":"100W有线+50W无线"}',
 'https://picsum.photos/seed/oneplus14/400/400', '["https://picsum.photos/seed/oneplus141/400/400","https://picsum.photos/seed/oneplus142/400/400"]',
 '一加14，哈苏影像旗舰，5400mAh超大电池。', 1980, 1),

(1019, 'OnePlus Ace 5 Pro', 'OnePlus', '智能手机', 2999.00, 3199.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.78英寸 AMOLED 120Hz","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"OxygenOS 15","weight":"200g","charger":"100W有线"}',
 'https://picsum.photos/seed/ace5pro/400/400', '["https://picsum.photos/seed/ace5pro1/400/400","https://picsum.photos/seed/ace5pro2/400/400"]',
 '一加Ace 5 Pro，性价比性能机，骁龙8 Gen 3。', 5600, 1),

(1020, 'Honor Magic7 Pro', 'Honor', '智能手机', 5699.00, 5999.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.8英寸 LTPO OLED 120Hz","battery":"5600mAh","camera":"5000万鹰眼四摄","ram":"12GB","storage":"256GB","os":"MagicOS 9.0","weight":"218g","charger":"100W有线+66W无线"}',
 'https://picsum.photos/seed/magic7pro/400/400', '["https://picsum.photos/seed/magic7pro1/400/400","https://picsum.photos/seed/magic7pro2/400/400"]',
 '荣耀Magic7 Pro，青海湖电池，鹰眼抓拍，AI护眼。', 1850, 1),

(1021, 'Honor 200 Pro', 'Honor', '智能手机', 3499.00, 3799.00,
 '{"cpu":"骁龙8s Gen 3","screen":"6.7英寸 AMOLED 120Hz","battery":"5200mAh","camera":"5000万雅顾三摄","ram":"12GB","storage":"256GB","os":"MagicOS 9.0","weight":"187g","charger":"100W有线+66W无线"}',
 'https://picsum.photos/seed/honor200pro/400/400', '["https://picsum.photos/seed/honor200pro1/400/400","https://picsum.photos/seed/honor200pro2/400/400"]',
 '荣耀200 Pro，雅顾人像，MagicOS智慧体验。', 2700, 1),

(1022, 'realme GT 7 Pro', 'realme', '智能手机', 3799.00, 4099.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.78英寸 AMOLED 120Hz","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"realme UI 6.0","weight":"199g","charger":"120W有线"}',
 'https://picsum.photos/seed/gt7pro/400/400', '["https://picsum.photos/seed/gt7pro1/400/400","https://picsum.photos/seed/gt7pro2/400/400"]',
 'realme GT 7 Pro，越级性能旗舰，120W光速秒充。', 3100, 1),

(1023, 'realme GT Neo 6', 'realme', '智能手机', 1899.00, 2099.00,
 '{"cpu":"骁龙8s Gen 3","screen":"6.74英寸 AMOLED 120Hz","battery":"5500mAh","camera":"5000万双摄","ram":"8GB","storage":"256GB","os":"realme UI 6.0","weight":"191g","charger":"120W有线"}',
 'https://picsum.photos/seed/gtneo6/400/400', '["https://picsum.photos/seed/gtneo61/400/400","https://picsum.photos/seed/gtneo62/400/400"]',
 'realme GT Neo 6，千元性能之王，120W闪充+5500mAh。', 8900, 1),

(1024, 'Sony Xperia 1 VI', 'Sony', '智能手机', 8999.00, 8999.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.5英寸 4K OLED 120Hz","battery":"5000mAh","camera":"4800万三摄(可变光圈)","ram":"12GB","storage":"256GB","os":"Android 15","weight":"192g","charger":"30W有线+15W无线"}',
 'https://picsum.photos/seed/xperia1vi/400/400', '["https://picsum.photos/seed/xperia1vi1/400/400","https://picsum.photos/seed/xperia1vi2/400/400"]',
 '索尼Xperia 1 VI，4K HDR OLED，专业影像，影音旗舰。', 520, 1),

(1025, 'Nubia Z80 Ultra', 'Nubia', '智能手机', 3999.00, 4299.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.8英寸 AMOLED 120Hz屏下摄像头","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"MyOS 15","weight":"218g","charger":"80W有线"}',
 'https://picsum.photos/seed/z80ultra/400/400', '["https://picsum.photos/seed/z80ultra1/400/400","https://picsum.photos/seed/z80ultra2/400/400"]',
 '努比亚Z80 Ultra，真全面屏，屏下摄像头，无挖孔。', 1100, 1),

(1026, 'ZTE Axon 60 Ultra', 'ZTE', '智能手机', 5699.00, 5999.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.78英寸 AMOLED 120Hz","battery":"5000mAh","camera":"5000万三主摄","ram":"12GB","storage":"256GB","os":"MyOS 15","weight":"205g","charger":"80W有线"}',
 'https://picsum.photos/seed/axon60/400/400', '["https://picsum.photos/seed/axon601/400/400","https://picsum.photos/seed/axon602/400/400"]',
 '中兴Axon 60 Ultra，卫星通信，独立安全芯片，商务旗舰。', 680, 1),

(1027, 'Motorola Edge 60 Pro', 'Motorola', '智能手机', 3499.00, 4499.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.7英寸 pOLED 144Hz","battery":"4600mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"Android 15","weight":"185g","charger":"68W有线+15W无线"}',
 'https://picsum.photos/seed/edge60pro/400/400', '["https://picsum.photos/seed/edge60pro1/400/400","https://picsum.photos/seed/edge60pro2/400/400"]',
 '摩托罗拉Edge 60 Pro，纯净Android体验，纤薄设计。', 780, 1),

(1028, 'Nothing Phone (3)', 'Nothing', '智能手机', 3799.00, 4599.00,
 '{"cpu":"骁龙8s Gen 3","screen":"6.7英寸 LTPO OLED 120Hz","battery":"5000mAh","camera":"5000万双摄","ram":"8GB","storage":"256GB","os":"Nothing OS 3.0","weight":"195g","charger":"45W有线+15W无线"}',
 'https://picsum.photos/seed/nothing3/400/400', '["https://picsum.photos/seed/nothing31/400/400","https://picsum.photos/seed/nothing32/400/400"]',
 'Nothing Phone (3)，Glyph灯效系统，极简设计风格。', 890, 1),

(1029, 'Meizu 22 Pro', 'Meizu', '智能手机', 3999.00, 4299.00,
 '{"cpu":"骁龙8 Gen 3","screen":"6.79英寸 AMOLED 120Hz","battery":"5000mAh","camera":"5000万三摄","ram":"12GB","storage":"256GB","os":"Flyme 11","weight":"208g","charger":"80W有线+50W无线"}',
 'https://picsum.photos/seed/meizu22pro/400/400', '["https://picsum.photos/seed/meizu22pro1/400/400","https://picsum.photos/seed/meizu22pro2/400/400"]',
 '魅族22 Pro，Flyme系统，超声波指纹，美学旗舰。', 650, 1),

(1030, 'ROG Phone 9 Pro', 'ASUS', '智能手机', 5999.00, 5999.00,
 '{"cpu":"骁龙8 Gen 4","screen":"6.78英寸 AMOLED 165Hz","battery":"5500mAh","camera":"5000万三摄","ram":"16GB","storage":"512GB","os":"ROG UI (Android 15)","weight":"239g","charger":"65W有线"}',
 'https://picsum.photos/seed/rog9pro/400/400', '["https://picsum.photos/seed/rog9pro1/400/400","https://picsum.photos/seed/rog9pro2/400/400"]',
 'ROG Phone 9 Pro，专业电竞手机，165Hz高刷，肩键设计。', 1450, 1);

-- =====================================================
-- 库存数据 (每款手机初始库存100-500台)
-- =====================================================
INSERT INTO t_stock (product_id, total, locked, available) VALUES
(1001, 500, 0, 500),
(1002, 400, 0, 400),
(1003, 600, 0, 600),
(1004, 300, 0, 300),
(1005, 200, 0, 200),
(1006, 350, 0, 350),
(1007, 500, 0, 500),
(1008, 450, 0, 450),
(1009, 800, 0, 800),
(1010, 250, 0, 250),
(1011, 300, 0, 300),
(1012, 400, 0, 400),
(1013, 200, 0, 200),
(1014, 350, 0, 350),
(1015, 250, 0, 250),
(1016, 400, 0, 400),
(1017, 300, 0, 300),
(1018, 200, 0, 200),
(1019, 450, 0, 450),
(1020, 250, 0, 250),
(1021, 300, 0, 300),
(1022, 350, 0, 350),
(1023, 500, 0, 500),
(1024, 100, 0, 100),
(1025, 200, 0, 200),
(1026, 150, 0, 150),
(1027, 200, 0, 200),
(1028, 180, 0, 180),
(1029, 150, 0, 150),
(1030, 200, 0, 200);

-- =====================================================
-- 测试用户通过注册接口创建（使用BCrypt加密密码）
-- =====================================================
