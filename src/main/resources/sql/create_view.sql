CREATE VIEW top_products_3days AS
SELECT
    p.product_id,
    p.product_name,
    p.product_price,
    p.stock_qty,
    COUNT(DISTINCT o.order_id) as order_count,
    SUM(oi.order_item_qty) as total_quantity
FROM
    products p
JOIN
    order_items oi ON p.product_id = oi.ordered_product_id
JOIN
    orders o ON oi.order_id = o.order_id
WHERE
    o.ordered_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
    AND o.order_status != 'CANCELED'
GROUP BY
    p.product_id, p.product_name, p.product_price, p.stock_qty
ORDER BY
    total_quantity DESC, order_count DESC
LIMIT 5;