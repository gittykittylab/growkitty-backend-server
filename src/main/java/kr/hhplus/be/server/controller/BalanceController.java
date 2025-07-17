package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.BalanceRequest;
import kr.hhplus.be.server.dto.BalanceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/balance")
@Tag(name = "Balance", description = "포인트 잔액 조회 API")
public class BalanceController {
    // mock 저장
    private final ConcurrentHashMap<Long, Long> balances = new ConcurrentHashMap<>();
    private static final long MAX_CHARGE_LIMIT = 10_000L;

    public BalanceController() {
        balances.put(1L, 5000L);  // 초기 데이터
    }
    //잔액 조회
    @Operation(summary = "잔액 조회", description = "유저의 현재 포인트 잔액을 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {
        Long balance = balances.getOrDefault(userId, 0L);
        return ResponseEntity.ok(new BalanceResponse(userId, balance, "조회 성공"));
    }
    //잔액 충전
    @PostMapping("/charge")
    @Operation(summary = "포인트 충전", description = "유저에게 포인트를 충전합니다.")
    public ResponseEntity<BalanceResponse> chargeBalance(@RequestBody BalanceRequest request){
        Long userId = request.getUserId();
        Long amount = request.getAmount();

        // 기존 잔액 조회 및 충전
        Long current = balances.getOrDefault(userId, 0L);
        Long updated = current + amount;

        if (amount < 0){
            return ResponseEntity.badRequest()
                    .body(new BalanceResponse(userId, 0L, "잘못된 충전 금액입니다."));
        }
        if (updated > MAX_CHARGE_LIMIT){
            return ResponseEntity.badRequest()
                    .body(new BalanceResponse(userId, 0L, "충전한도를 초과했습니다."));
        }
        //충전 결과
        balances.put(userId, updated);

        return ResponseEntity.ok(new BalanceResponse(userId, updated, "충전 성공"));
    }
}
