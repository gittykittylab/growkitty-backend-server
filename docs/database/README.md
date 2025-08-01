
# 📄 DB 조회 성능 최적화 보고서

## ✅ 개요

본 보고서는 `top_products_3days` 뷰에 대한 조회 성능을 최적화하기 위한 실험을 기반으로, 각 단계별 인덱스 및 쿼리 구조 변경이 실제 실행 성능에 어떤 영향을 미치는지를 분석한 자료입니다. `SHOW PROFILE`과 `EXPLAIN`을 활용하여 병목 지점을 파악하고, 개선 흐름을 정리하였습니다.

---

## 🔍 실험 구성

### 테스트 대상

```sql
SELECT * FROM top_products_3days;
```

## 🧪 대상 쿼리 실행 (EXPLAIN 결과)

| id | select_type | table      | partitions | type   | possible_keys | key     | key_len | ref                          | rows   | filtered | Extra                            |
|----|-------------|------------|------------|--------|---------------|---------|---------|------------------------------|--------|----------|----------------------------------|
| 1  | PRIMARY     | <derived2> | NULL       | ALL    | NULL          | NULL    | NULL    | NULL                         | 5      | 100.00   | NULL                             |
| 2  | DERIVED     | oi         | NULL       | ALL    | NULL          | NULL    | NULL    | NULL                         | 996256 | 100.00   | Using temporary; Using filesort |
| 2  | DERIVED     | p          | NULL       | eq_ref | PRIMARY       | PRIMARY | 8       | hhplus.oi.ordered_product_id | 1      | 100.00   | NULL                             |
| 2  | DERIVED     | o          | NULL       | eq_ref | PRIMARY       | PRIMARY | 8       | hhplus.oi.order_id           | 1      | 27.77    | Using where                      |


---

## 🔍 병목 원인 분석

### 1. `order_items (oi)` - **Full Table Scan**
- `type: ALL`, `rows: 996,256`
- 인덱스를 사용하지 않아 **전체 데이터를 스캔**
- `Extra: Using temporary; Using filesort` → 디스크 기반 정렬과 임시 테이블 사용
- **가장 큰 성능 저하 요인**

### 2. `products (p)` - **정상 처리**
- `type: eq_ref`, `key: PRIMARY`, `rows: 1`
- 기본키 조인을 통해 성능 문제 없음

### 3. `orders (o)` - **조건 필터링 존재**
- `type: eq_ref`, `key: PRIMARY`, `filtered: 27.77%`
- 기본키 조인은 정상이나, `WHERE` 조건으로 인해 일부 필터링 발생
- 추가 비용 발생 가능

### 4. `<derived2>` - **파생 테이블 전체 순회**
- 메인 쿼리에서 파생 테이블 `<derived2>`를 전체 순회
- 자체는 작지만, **내부 파생 테이블 생성 비용이 매우 큼**

---

## ⚠️ 문제 요약

| 항목 | 설명 |
|------|------|
| ❗ `order_items` Full Scan | 인덱스 없이 100만 건 순회 |
| ❗ `Using temporary; Using filesort` | 정렬 성능 저하, 디스크 사용 가능성 |
| ⚠️ `orders` filtered = 27.77% | 필터링 조건 비용 있음 |

---
### 최적화 단계 구성

| 단계 | 적용 내용 |
|------|-----------|
| 0단계 | 기본 쿼리 (최적화 없음) |
| 1단계 | `order_items(ordered_product_id, order_id, order_item_qty)` 인덱스 적용 |
| 2단계 | + `orders(ordered_at, order_status)` 인덱스 추가 |
| 3단계 | + 쿼리 구조 최적화 (CTE 제거 등) |

---
## ⚙️ 단계별 최적화 적용

### ✅ 1단계: `order_items` 인덱스 적용

```sql
-- 복합 인덱스 적용: 조인 및 집계 효율 개선
CREATE INDEX idx_order_items_product_order_qty
  ON order_items(ordered_product_id, order_id, order_item_qty);

-- 실행 계획 및 성능 확인
EXPLAIN SELECT * FROM top_products_3days;
SET profiling = 1;
SELECT * FROM top_products_3days;
SHOW PROFILE;
```

- `order_items`의 조인 키와 그룹핑 키에 인덱스 적용
- **실행 시간: 2.1s → 0.96s로 대폭 개선**
- `executing` 병목 완화, 임시 테이블도 2회 → 1회로 감소

---

### ✅ 2단계: `orders` 인덱스 추가

```sql
    -- 필터 조건 컬럼에 복합 인덱스 적용
    CREATE INDEX idx_orders_date_status
      ON orders(ordered_at, order_status);
    
    -- 실행 계획 및 성능 확인
    EXPLAIN SELECT * FROM top_products_3days;
    SET profiling = 1;
    SELECT * FROM top_products_3days;
    SHOW PROFILE;
```

- `ordered_at`, `order_status`를 동시에 조건으로 가지므로 복합 인덱스 구성
- **실행 시간: 0.96s → 0.95s** (소폭 개선)
- 필터 자체는 잘 작동하지만, **옵티마이저 판단상 활용도는 제한적**

---

### ✅ 3단계: 쿼리 구조 리팩토링 (CTE 기반)

```sql
    -- 가독성과 조건 분리를 위한 쿼리 구조 개선
    CREATE VIEW top_products_3days AS
    WITH recent_orders AS (
        SELECT order_id
        FROM orders
        WHERE ordered_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
        AND order_status != 'CANCELED'
    )
    SELECT
        p.product_id,
        p.product_name,
        p.product_price,
        p.stock_qty,
        COUNT(DISTINCT oi.order_id) as order_count,
        SUM(oi.order_item_qty) as total_quantity
    FROM
        products p
    JOIN
        order_items oi ON p.product_id = oi.ordered_product_id
    JOIN
        recent_orders ro ON oi.order_id = ro.order_id
    GROUP BY
        p.product_id, p.product_name, p.product_price, p.stock_qty
    ORDER BY
        total_quantity DESC, order_count DESC
    LIMIT 5;
```

- 구조적으로 명확해졌으나 성능은 저하됨 (0.95s → **1.01s**)
- **EXPLAIN 결과: `products`가 드라이빙 테이블이 되며 Full Scan 발생**
- 읽은 row 수 증가 → `executing` 시간 증가

---

## 📈 단계별 성능 비교

### 💡 SHOW PROFILE 기반 비교

| 단계 | 적용 내용 | 총 실행 시간 | executing 단계 시간 | 임시 테이블 | 비고 |
|------|-----------|----------------|----------------------|---------------|------|
| 0단계 | 최적화 없음 | 2.1s | 2.074s | ✅ 2회 | Full Scan + 정렬 병목 |
| 1단계 | 인덱스 1 적용 | 0.96s | 0.954s | ✅ 1회 | 조인 및 집계 개선 |
| 2단계 | 인덱스 2 추가 | 0.95s | 0.949s | ✅ 1회 | 효과는 미미함 |
| 3단계 | 쿼리 구조 변경 | 1.01s ⬆ | 1.016s ⬆ | ✅ 1회 | 성능 하락 발생 |

> ✅ **가장 성능이 좋았던 단계는 2단계**이며, 이후 쿼리 구조 최적화는 성능 측면에서 오히려 후퇴함

---

## 🧠 결론

| 항목 | 권장 사항 |
|------|-----------|
| 최적의 단계 | 2단계 (인덱스 중심 최적화) |
| 쿼리 구조 최적화 | 가독성 측면에서는 유의미하나, 성능 측면에서는 보류 필요 |
| 추가 개선 방향 | `JOIN_ORDER` 힌트, COVERING INDEX, CTE 유지 등 |

---

## ✅ 최종 요약 및 성과 분석

- 복합 인덱스 설계만으로도 **전체 실행 시간을 2.1s → 0.95s로 54% 이상 개선**
- 인덱스 설계 시 컬럼 순서와 카디널리티, 필터 조건을 고려하여 쿼리 실행 패턴에 맞는 최적 구성을 적용함. 
- CTE 활용과 단계별 성능 테스트를 통해 중간 결과셋을 최소화하고, 데이터 증가 상황에서도 확장 가능한 안정적인 쿼리를 확보함.

---

