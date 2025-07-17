package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.BalanceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/balance")
@Tag(name = "Balance", description = "포인트 잔액 조회 API")
public class BalanceController {
    // mock 저장
    private final ConcurrentHashMap<Long, Long> balances = new ConcurrentHashMap<>();

    public BalanceController() {
        balances.put(1L, 5000L);  // 초기 데이터
    }
    //잔액 조회
    @Operation(summary = "잔액 조회", description = "유저의 현재 포인트 잔액을 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {
        Long balance = balances.getOrDefault(userId, 0L);
        BalanceResponse response = new BalanceResponse(userId, balance, "조회 성공");
        return ResponseEntity.ok(response);
    }

}
