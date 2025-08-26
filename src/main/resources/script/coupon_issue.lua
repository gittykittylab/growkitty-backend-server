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
