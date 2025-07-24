# 서버구축 - 소프트웨어 설계

본 프로젝트의 아키텍처는 Layered Architecture를 참고하였으며, 일부 원칙을 완전히 따르진 않았지만 유연성과 실용성을 고려해 아래와 같은 구조로 설계하였습니다.

```
Controller → (Facade) → Service → Repository → DB
```

---

## ✍️ 설계 의도

> "이상적인 구조와 실용성 사이에서 균형을 잡는 것이 중요하다고 생각했습니다."

* 이번 과제는 완전히 습득하지 못한 이상적인 아키텍처 원칙을 무작정 따르기보다는, **과제의 목적과 현실적인 제약**을 고려한 구조 설계를 목표로 했습니다.
* ORM 방식이나 데이터베이스 종류가 변경될 가능성이 낮은 환경임을 고려하여, 과도한 추상화나 불필요한 계층 추가는 지양했습니다.
---

## ✅ 계층별 구성 및 책임

| 계층         | 책임 설명                      | 구현 예시                               |
| ---------- |----------------------------| ----------------------------------- |
| Controller | 클라이언트 요청 수신 및 응답 반환        | `OrderController`, `UserController` |
| Facade     | 여러 서비스 로직을 조합한 유스케이스 처리    | `OrderFacade.createOrder()`         |
| Service    | 핵심 비즈니스 로직 처리     | `OrderService`, `UserService`       |
| Repository | JPA 기반의 DB 접근 로직 구현        | `OrderRepository`, `UserRepository` |
| DTO        | Controller ↔ Service 간 데이터 전달 및 표현 분리 | `OrderRequest`, `OrderResponse` 등   |

---

## 🛠  설계를 반영한 주요 변경점

### 1. **Controller 의존성 축소**

* `OrderController`가 `OrderService`, `OrderFacade`를 모두 의존하던 구조에서 → `Facade`로 통합
* 단일 진입점 구성으로 **Controller의 책임 명확화**
* 
### 2. **Controller의 직접 의존 제거 및 계층 분리**

* `UserController`가 `UserRepository`를 직접 참조하던 구조에서 → 해당 로직을`application 계층으로 이전`
* 책임 분리와 계층 간 역할 명확해짐

### 3. **Repository 직접 의존에 대한 실용적 선택**

- `UserController`는 포인트 잔액 조회 시 `Repository`를 직접 호출합니다.
- 이는 **DIP 원칙을 일부 위반**할 수 있는 구조지만, 다음과 같은 상황을 고려하였습니다:
>- 단순한 조회 로직의 경우, 별도의 Service 계층을 도입하는 것은 오히려 과한 추상화가 될 수 있음
>- 제한된 시간과 구현 역량 내에서 구조적 이상보다 **개발 효율성과 현실적인 구현 가능성**을 우선 고려함
>- 향후 외부 API 연동 등 구조 변화가 필요한 시점에 **추상화를 도입하는 것이 더 합리적**이라 판단

## ⚠️ 위반 사항 및 보완 포인트

| 항목          | 설명                                                                                  |
| ----------- |-------------------------------------------------------------------------------------|
| DIP 위반      | `Service`가 JPA Repository에 직접 의존 → 의도적인 인터페이스 추상화 미도입(변경 가능성이 낮은 환경에서 불필요한 추상화를 지양) |
| 의존성 분산      | `OrderController`에서 `OrderService`, `OrderFacade`를 동시에 의존하던 구조 → `Facade`로 통합       |
| 레이어 침범      | `UserController`가 `UserRepository`를 직접 사용하는 구조 → `UserService` 로 해당 로직 이동           |


---

## 돌아보며
> 이번 아키텍처 설계는 구조적 이상을 무조건 추구하기보다는 현실적인 조건 속에서 무엇을 어떤 기준으로 분리할 것인지에 대해 고민하고 직접 적용해보는 소중한 경험이었습니다.
