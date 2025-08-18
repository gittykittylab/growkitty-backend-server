-- 사용자 데이터 삽입
INSERT INTO users (user_id, point_balance)
VALUES (1, 100000),
       (2, 50000),
       (3, 30000);

-- 상품 데이터 삽입
INSERT INTO products (product_id, product_name, product_price, stock_qty, created_at)
VALUES
    (1, '테스트 상품 1', 10000, 5, NOW()),
    (2, '테스트 상품 2', 20000, 20, NOW());