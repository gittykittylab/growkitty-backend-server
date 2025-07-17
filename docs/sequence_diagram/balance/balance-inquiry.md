```mermaid
---
config:
  theme: default
---
sequenceDiagram
    %% 포인트 잔액 조회 시나리오
    participant BalanceController
    participant BalanceService
    participant BalanceRepository

    BalanceController ->> BalanceService: 잔액 조회 요청 
    BalanceService ->> BalanceRepository: 현재 잔액 조회
    BalanceRepository -->> BalanceService: 현재 잔액 반환
    BalanceService -->> BalanceController: 잔액 응답
```