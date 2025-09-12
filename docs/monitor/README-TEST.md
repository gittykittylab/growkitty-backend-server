# 부하 테스트 문서

## 개요
> 황인태요 이커머스는 많은 사용자가 동시에 접근하여도 안정적으로 시스템이 작동해야한는 목표를 가지고 있다. 따라서, 부하
> 테스트를 통해 시스템의 병목 현상을 확인하고 개선하는데 목적이 있다.

## 1. 부하 테스트 대상 선정 기준
> 1. 사용자가 빈번하게 사용할 api
> 2. 잔액 충전, 상품 주문 등 동시성 문제가 발생하면 안되는 api
> 3. slow query가 발생할 수 있는 통계성 데이터 조회

## 2. 테스트 대상 목록
> 위의 기준으로 테스트 대상을 다음과 같이 선정하였다.

| **기능명** | **Method** | **Endpoint**                | **설명**                    |
|-------|------------|-----------------------------|---------------------------|
| 포인트 조회 | GET        | `/api/users/points`         | 사용자의 포인트를 조회합니다.          |
| 인기 상품 조회 | GET        | `/api/products/top-selling` | 인기 있는 상품 목록을 조회합니다.       |
| 상품 정보 조회 | GET        | `/api/products`             | 특정 상품 ID를 이용하여 상품을 조회합니다. |
| 주문 생성 | POST       | `/api/cash`                 | 상품 주문을 생성합니다              |
| 주문 조회 | POST       | `/api/orders`               | 상품 주문을 조회합니다.             |

## 3. 테스트 목표
> 1. 테스트 대상 목록의 성능 분석
> 2. 병목 현상 확인

## 4. 테스트 시나리오
> - order, order_items, products에 대해서 10만건, 100만건, 1000건등의 데이터를 준비한다.
> - 테스트 스크립트의 반복으로 부하 테스트 한다.

## 5. 테스트 스크립트
```js
### =================================================
### E2E 플로우 1: 사용자 1
### =================================================

### 1-1. 사용자 1 포인트 조회
GET {{host}}/api/users/1/points

> {%
console.log("사용자1 포인트 조회 - 응답시간: " + response.responseTime + "ms");
if (response.status === 200) {
    const balance = response.body.pointBalance || 0;
    console.log("사용자1 현재 포인트: " + balance + "원");
}
%}

###

### 1-2. 인기상품 조회
GET {{host}}/api/products/top-selling

> {%
console.log("인기상품 조회 - 응답시간: " + response.responseTime + "ms");
%}

###

### 1-3. 상품 1 정보 조회
GET {{host}}/api/products/1

> {%
console.log("상품1 조회 - 응답시간: " + response.responseTime + "ms");
%}

###

### 1-4. 사용자 1 주문 생성
POST {{host}}/api/orders
Content-Type: application/json

{
    "userId": 1,
    "orderItems": [
    {
        "productId": 1,
        "quantity": 3
    }
],
    "usedAmount": 30000
}

> {%
    const responseTime = response.timingPhases ? (response.timingPhases.wait + response.timingPhases.receive) : response.responseTime;
    console.log("사용자1 주문 생성 - 응답시간: " + responseTime + "ms");
    if (response.status === 200 || response.status === 201) {
        const orderId = response.body.orderId || response.body.id;
        client.global.set("orderId1", orderId);
        console.log("사용자1 주문 성공 - 주문 ID: " + orderId);
    } else {
        client.global.set("orderId1", "failed");
        console.log("사용자1 주문 실패 - 상태: " + response.status + ", 응답: " + JSON.stringify(response.body));
    }
%}

###

### 1-5. 사용자 1 주문 조회
GET {{host}}/api/orders/{{orderId1}}

> {%
    const orderId = client.global.get("orderId1");
    if (orderId && orderId !== "failed") {
        console.log("사용자1 주문 조회 - 응답시간: " + response.responseTime + "ms");
        if (response.status === 200) {
            const orderStatus = response.body.orderStatus || "확인불가";
            const totalAmount = response.body.totalAmount || 0;
            console.log("사용자1 주문 상태: " + orderStatus + ", 총 금액: " + totalAmount + "원");
        }
    } else {
        console.log("사용자1 주문 조회 스킵 - 주문 생성 실패");
    }
%}

###
```

## 6. 테스트 결과

### 6.1 분석
- Checks: 100.00% : 모든 테스트 성공
- slow query 발생
    - 인기 상품 조회 (최대: 8초, 평균: 4초)

### 7.2 플레임 그래프
![flamegraph](/src/docs/monitor/flamegraph.png)

### 7.3 호출트리
![calltree](/src/docs/monitor/calltree.png)

### 7.4 메서드 목록
![method](/src/docs/monitor/method.png)

## 개선방안
- 조회되는 데이터량이 많아 캐싱과 인덱스를 활용하여 데이터베이스 부하를 개선해보고자 한다.
    - 캐싱 : 인기 상품 조회