package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.CouponIssueResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/coupon")
@Tag(name = "Coupon", description = "쿠폰 발급 API")
public class CouponController {

    // Mock 저장소
    private final Map<String, Integer> couponStock = new ConcurrentHashMap<>();           // 쿠폰 재고
    private final Map<String, String> couponPolicy = new ConcurrentHashMap<>();           // 쿠폰 코드 → 등급
    private final Map<Long, String> userGrades = new ConcurrentHashMap<>();               // 유저 ID → 등급
    private final Map<Long, Set<String>> issuedCoupons = new ConcurrentHashMap<>();       // 유저 ID → 보유 쿠폰

    public CouponController() {
        // 초기 쿠폰 재고
        couponStock.put("WELCOME10", 5);
        couponStock.put("DISCOUNT50", 2);

        // 쿠폰 정책 (해당 등급 이상만 발급 가능)
        couponPolicy.put("WELCOME10", "GOLD");
        couponPolicy.put("DISCOUNT50", "SILVER");

        // 유저 등급
        userGrades.put(1L, "GOLD");
        userGrades.put(2L, "SILVER");
        userGrades.put(3L, "BRONZE");
    }

    @Operation(summary = "선착순 쿠폰 발급", description = "조건을 만족하는 유저에게 선착순 쿠폰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = """
            쿠폰 발급 실패 사유:
            - 쿠폰 수량 부족
            - 유저 등급 조건 불충족
            - 이미 발급받은 쿠폰 존재
            """,
                    content = @Content(schema = @Schema(implementation = CouponIssueResponse.class)) // 예시 DTO
            )
    })
    @PostMapping("/issue")
    public ResponseEntity<CouponIssueResponse> issueCoupon(
            @RequestParam Long userId,
            @RequestParam String couponCode
    ) {
        // 1. 수량 확인
        Integer stock = couponStock.getOrDefault(couponCode, 0);
        if (stock <= 0) {
            return ResponseEntity.badRequest().body(new CouponIssueResponse(userId, couponCode, "쿠폰 수량 부족"));
        }

        // 2. 등급 확인
        String requiredGrade = couponPolicy.get(couponCode);
        String userGrade = userGrades.get(userId);
        if (userGrade == null || !userGrade.equals(requiredGrade)) {
            return ResponseEntity.badRequest().body(new CouponIssueResponse(userId, couponCode, "발급 대상 아님"));
        }

        // 3. 보유 여부 확인
        Set<String> userCoupons = issuedCoupons.getOrDefault(userId, new HashSet<>());
        if (userCoupons.contains(couponCode)) {
            return ResponseEntity.badRequest().body(new CouponIssueResponse(userId, couponCode, "이미 발급된 쿠폰입니다."));
        }

        // 4. 발급 진행
        couponStock.computeIfPresent(couponCode, (k, v) -> v - 1);
        userCoupons.add(couponCode);
        issuedCoupons.put(userId, userCoupons);

        return ResponseEntity.ok(new CouponIssueResponse(userId, couponCode, "쿠폰 발급 성공"));
    }

}
