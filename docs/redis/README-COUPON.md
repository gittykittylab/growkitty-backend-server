# Redis 기반 선착순 쿠폰 발급 시스템 기술 보고서

## Redis 도입 배경

### 기존 방식의 문제점
- **성능 병목**: DB Lock으로 인한 처리 성능 저하
- **동시성 이슈**: 대량 동시 요청 시 데드락 및 타임아웃 발생
- **확장성 제한**: DB 커넥션 풀 한계로 인한 처리량 제약

### Redis 도입 이유
- **고성능**: 인메모리 기반으로 빠른 읽기/쓰기 성능
- **원자성 보장**: 단일 스레드 모델로 Race Condition 방지
- **다양한 자료구조**: List, Set 등을 활용한 효율적인 큐 관리
- **스크립팅 지원**: Lua 스크립트를 통한 복잡한 원자적 연산

## 핵심 자료구조 설계

### 1. 대기열 관리 (List)
```java
private static final String COUPON_QUEUE = "coupon:queue:%d";
```
- **구조**: Redis List
- **용도**: 사용자 요청 순서 관리
- **특징**: FIFO(First In, First Out) 방식으로 공정한 선착순 보장

### 2. 발급 완료 사용자 추적 (Set)
```java
private static final String COUPON_ISSUED = "coupon:issued:%d";
```
- **구조**: Redis Set
- **용도**: 중복 발급 방지를 위한 발급 완료 사용자 저장
- **특징**: O(1) 시간복잡도로 빠른 중복 체크

### 3. 재고 관리 (List)
```java
private static final String COUPON_STOCK = "coupon:stock:%d";
```
- **구조**: Redis List
- **용도**: 발급 가능한 쿠폰 재고 토큰 관리
- **특징**: 토큰 기반으로 정확한 재고 제어

### 4. 중복 요청 방지 (Set)
```java
private static final String COUPON_SET = "coupon:queue:set:%d";
```
- **구조**: Redis Set
- **용도**: 대기열 진입 시 중복 방지
- **특징**: 동일 사용자의 여러 요청 차단

## 핵심 기능 구현

### 1. 쿠폰 발급 요청 처리

**플로우**:
1. 이미 발급받은 사용자 체크 (`SISMEMBER`)
2. 대기열 중복 진입 방지 (`SADD`)
3. 대기열에 사용자 추가 (`LPUSH`)
4. 현재 대기 위치 반환

**핵심 코드**:
```java
public WaitingQueueResponse requestCoupon(Long policyId, Long userId) {
    // 1. 중복 발급 체크
    if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(issuedKey, userId.toString()))) {
        return WaitingQueueResponse.completed("이미 발급받은 쿠폰입니다.");
    }
    
    // 2. Set으로 중복 방지
    Long addResult = redisTemplate.opsForSet().add(queueSetKey, userId.toString());
    
    if (addResult != null && addResult > 0) {
        redisTemplate.opsForList().leftPush(queueKey, userId.toString());
    }
}
```

### 2. 재고 초기화 전략

**동적 재고 관리**:
```java
private void initializeStockIfNeeded(Long policyId, String stockKey) {
    if (redisTemplate.hasKey(stockKey)) {
        return; // 이미 초기화됨
    }
    
    // 현재 발급된 수량 조회
    long issuedCount = couponRepository.countByPolicyId(policyId);
    int remainingStock = totalQuantity - (int) issuedCount;
    
    // Redis에 재고 토큰 추가
    String[] stockTokens = new String[remainingStock];
    Arrays.fill(stockTokens, "STOCK");
    redisTemplate.opsForList().leftPushAll(stockKey, stockTokens);
}
```

## 스케줄러 기반 비동기 처리

### 처리 전략
- **주기**: 2초마다 실행 (`@Scheduled(fixedDelay = 2000)`)
- **배치 처리**: 한 번에 최대 10명씩 처리
- **예외 격리**: 정책별 독립적 처리로 장애 전파 방지

### 스케줄러 동작 흐름
1. 활성화된 모든 쿠폰 정책 조회
2. 정책별 대기열 순차 처리
3. Lua 스크립트를 통한 원자적 처리
4. DB 저장 및 발급 완료 처리

```java
@Scheduled(fixedDelay = 2000)
public void processWaitingQueues() {
    List<CouponPolicy> activePolicies = couponPolicyRepository.findAll();
    
    for (CouponPolicy policy : activePolicies) {
        try {
            processQueue(policy.getPolicyId());
        } catch (Exception e) {
            log.error("대기열 처리 실패 - 정책: {}", policy.getPolicyId());
        }
    }
}
```

## 동시성 제어 및 원자성 보장

### Lua 스크립트 활용

**원자성이 필요한 이유**:
- 재고 차감과 대기열 처리가 동시에 실행되어야 함
- 부분 실패 시 데이터 불일치 발생 가능
- Redis 명령어 간의 경쟁 상태 방지

**Lua 스크립트 구조**:
```lua
-- coupon_issue.lua
-- KEYS[1]: 재고 키 (stockKey)
-- KEYS[2]: 대기열 키 (queueKey)

-- 1. 재고 리스트에서 아이템을 하나 꺼냅니다.
local stock = redis.call('LPOP', KEYS[1])

-- 1-1. 만약 재고가 없다면, nil을 반환하여 로직을 중단합니다.
if not stock then
  return nil
end

-- 2. 대기열 리스트에서 사용자 ID를 하나 꺼냅니다.
local userId = redis.call('RPOP', KEYS[2])

-- 2-1. 만약 대기열에 사용자가 없다면,
if not userId then
  -- 이전에 꺼냈던 재고를 다시 원상 복구합니다.
  redis.call('LPUSH', KEYS[1], stock)
  return nil
end

-- 3. 모든 작업이 성공했으므로, {재고, 사용자ID}를 반환합니다.
return {stock, userId}
```

### 스크립트의 장점
1. **원자성**: 모든 연산이 하나의 트랜잭션으로 실행
2. **일관성**: 재고와 대기열 상태의 동기화 보장
3. **롤백**: 실패 시 자동 상태 복구
4. **성능**: 네트워크 왕복 최소화

## 중복 발급 방지 메커니즘

### 다층 중복 방지 구조

1. **Redis Set 기반 1차 방지**:
   ```java
   // 발급 완료 사용자 체크
   redisTemplate.opsForSet().isMember(issuedKey, userId.toString())
   ```

2. **대기열 진입 중복 방지**:
   ```java
   // 대기열 Set으로 중복 진입 차단
   redisTemplate.opsForSet().add(queueSetKey, userId.toString())
   ```

3. **DB 레벨 최종 방지**:
   ```java
   // 트랜잭션 내에서 최종 중복 체크
   if (couponRepository.existsByUserIdAndPolicyId(userId, policyId)) {
       throw new RuntimeException("이미 발급받은 쿠폰입니다.");
   }
   ```

### 비관적 락 vs Redis 방식 비교

| 구분 | 비관적 락 방식 | Redis 방식 |
|------|---------------|------------|
| 성능 | DB 락으로 인한 병목 | 인메모리 고속 처리 |
| 확장성 | 커넥션 풀 제한 | 높은 동시 처리 능력 |
| 안정성 | 데드락 위험 | 단일 스레드 모델 |
| 복잡도 | 단순한 구조 | 복합적 자료구조 |

## 장애 복구 및 안정성

### 실패 시나리오 대응

1. **Lua 스크립트 실행 실패**:
    - 로그 기록 후 다음 스케줄링에서 재시도
    - 시스템 전체 중단 방지

2. **DB 저장 실패**:
   ```java
   catch (Exception e) {
       // 재고 복구 (다시 앞쪽에 추가)
       redisTemplate.opsForList().leftPush(stockKey, stock);
       // 사용자를 대기열 뒤쪽에 다시 추가 (재시도 기회 제공)
       redisTemplate.opsForList().rightPush(queueKey, userId);
   }
   ```

3. **Redis 연결 실패**:
    - Connection Pool 및 재연결 메커니즘
    - 폴백으로 DB 기반 처리 가능

### TTL 기반 자동 정리
```java
// 24시간 TTL 설정으로 자동 정리
redisTemplate.expire(stockKey, 24, TimeUnit.HOURS);
```

## 성능 최적화 요소

### 1. 배치 처리
- 한 번에 최대 10명씩 처리하여 시스템 부하 분산
- 스케줄링 간격 조정으로 처리량 제어

### 2. 메모리 최적화
- 재고 토큰을 단순 문자열로 관리
- Set 자료구조로 O(1) 조회 성능

### 3. 네트워크 최적화
- Lua 스크립트로 Redis 왕복 횟수 최소화
- Pipeline 사용 가능한 구조 설계

## 모니터링 및 운영

### 로깅 전략
```java
log.info("쿠폰 발급 완료 - 사용자: {}, 정책: {}", userId, policyId);
log.error("대기열 처리 실패 - 정책: {}, 오류: {}", policy.getPolicyId(), e.getMessage());
```

### 주요 모니터링 지표
- 대기열 길이 추적
- 재고 소진 상황 모니터링
- 처리 지연시간 측정
- 실패율 및 재시도 횟수

## 결론

Redis 기반 선착순 쿠폰 발급 시스템은 다음과 같은 핵심 가치를 제공합니다:

**성능**: 인메모리 처리로 초당 수천 건의 요청 처리 가능
**안정성**: Lua 스크립트와 다층 중복 방지로 데이터 일관성 보장
**확장성**: 수평적 확장이 용이한 아키텍처 설계
**운영성**: 장애 복구와 모니터링이 용이한 구조

이러한 설계를 통해 대용량 트래픽 환경에서도 안정적이고 공정한 선착순 쿠폰 발급 서비스를 제공할 수 있습니다.