# 📊 STEP08 - DB 성능 개선 보고서

## 1. 분석 대상 기능 요약
- 주문 목록 조회 API
- 인기 상품 조회 API

## 2. 주요 느린 쿼리 및 EXPLAIN 분석
### 2-1. 주문 목록 조회
```sql
SELECT o.* FROM orders o WHERE o.user_id = ? ORDER BY o.ordered_at DESC;
