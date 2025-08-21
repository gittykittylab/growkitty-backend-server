-- 사용자 데이터 삽입
INSERT INTO users (user_id, point_balance)
VALUES (1, 100000),
       (2, 50000),
       (3, 30000),
       (4, 75000),
       (5, 120000),
       (6, 25000),
       (7, 80000),
       (8, 60000),
       (9, 45000),
       (10, 90000);

-- 상품 데이터 삽입
INSERT INTO products (product_id, product_name, product_price, stock_qty, created_at)
VALUES
    (1, '테스트 상품 1', 10000, 100, NOW()),
    (2, '테스트 상품 2', 20000, 100, NOW()),
    (3, '태블릿', 600000, 30, NOW()),
    (4, '무선 이어폰', 200000, 50, NOW()),
    (5, '스마트워치', 400000, 20, NOW()),
    (6, '키보드', 150000, 40, NOW()),
    (7, '마우스', 80000, 60, NOW()),
    (8, '모니터', 350000, 18, NOW()),
    (9, '웹캠', 120000, 35, NOW()),
    (10, '스피커', 250000, 28, NOW());

-- 주문 데이터 삽입 (오늘 날짜)
INSERT INTO orders (order_id, user_id, coupon_id, order_status, ordered_at, total_amount, coupon_discount_amount)
VALUES
    (1, 1, NULL, 'PAID', NOW(), 1800000, 0),
    (2, 2, NULL, 'PAID', NOW(), 1040000, 0),
    (3, 3, NULL, 'PAID', NOW(), 840000, 0),
    (4, 4, NULL, 'PAID', NOW(), 1100000, 0),
    (5, 7, NULL, 'PAID', NOW(), 270000, 0);

-- 주문 아이템 데이터 삽입 (오늘 날짜)
INSERT INTO order_items (order_item_id, order_id, product_id, ordered_product_name, ordered_product_price, order_item_price, order_item_qty)
VALUES
    (1, 1, 3, '태블릿', 600000, 600000, 2),
    (2, 1, 4, '무선 이어폰', 200000, 200000, 3),
    (3, 2, 5, '스마트워치', 400000, 400000, 1),
    (4, 2, 7, '마우스', 80000, 80000, 8),
    (5, 3, 6, '키보드', 150000, 150000, 4),
    (6, 3, 9, '웹캠', 120000, 120000, 2),
    (7, 4, 8, '모니터', 350000, 350000, 1),
    (8, 4, 10, '스피커', 250000, 250000, 3),
    (9, 5, 1, '테스트 상품 1', 10000, 10000, 15),
    (10, 5, 2, '테스트 상품 2', 20000, 20000, 6);

-- 주문 데이터 삽입 (어제 날짜)
INSERT INTO orders (order_id, user_id, coupon_id, order_status, ordered_at, total_amount, coupon_discount_amount)
VALUES
    (6, 5, NULL, 'DELIVERED', DATE_SUB(NOW(), INTERVAL 1 DAY), 800000, 0),
    (7, 6, NULL, 'DELIVERED', DATE_SUB(NOW(), INTERVAL 1 DAY), 1560000, 0),
    (8, 8, NULL, 'SHIPPING', DATE_SUB(NOW(), INTERVAL 1 DAY), 1400000, 0),
    (9, 9, NULL, 'PREPARING', DATE_SUB(NOW(), INTERVAL 1 DAY), 3020000, 0);

-- 주문 아이템 데이터 삽입 (어제 날짜)
INSERT INTO order_items (order_item_id, order_id, product_id, ordered_product_name, ordered_product_price, order_item_price, order_item_qty)
VALUES
    (11, 6, 5, '스마트워치', 400000, 400000, 2),
    (12, 7, 7, '마우스', 80000, 80000, 12),
    (13, 7, 9, '웹캠', 120000, 120000, 5),
    (14, 8, 4, '무선 이어폰', 200000, 200000, 7),
    (15, 9, 3, '태블릿', 600000, 600000, 3),
    (16, 9, 6, '키보드', 150000, 150000, 6),
    (17, 9, 7, '마우스', 80000, 80000, 4);

-- 주문 데이터 삽입 (2일 전 날짜)
INSERT INTO orders (order_id, user_id, coupon_id, order_status, ordered_at, total_amount, coupon_discount_amount)
VALUES
    (10, 10, NULL, 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY), 2600000, 0),
    (11, 1, NULL, 'DELIVERED', DATE_SUB(NOW(), INTERVAL 2 DAY), 800000, 0),
    (12, 2, NULL, 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY), 2420000, 0);

-- 주문 아이템 데이터 삽입 (2일 전 날짜)
INSERT INTO order_items (order_item_id, order_id, product_id, ordered_product_name, ordered_product_price, order_item_price, order_item_qty)
VALUES
    (18, 10, 3, '태블릿', 600000, 600000, 4),
    (19, 10, 1, '테스트 상품 1', 10000, 10000, 20),
    (20, 11, 8, '모니터', 350000, 350000, 2),
    (21, 11, 1, '테스트 상품 1', 10000, 10000, 10),
    (22, 12, 7, '마우스', 80000, 80000, 9),
    (23, 12, 4, '무선 이어폰', 200000, 200000, 1),
    (24, 12, 6, '키보드', 150000, 150000, 8),
    (25, 12, 2, '테스트 상품 2', 20000, 20000, 25);