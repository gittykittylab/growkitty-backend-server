package kr.hhplus.be.server.coupon.domain.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WaitingQueueResponse {
    private String status; // WAITING, ACTIVE, COMPLETED
    private long position; // 대기 순번
    private long totalWaiting; // 전체 대기 인원
    private String message;

    public static WaitingQueueResponse waiting(long position, long totalWaiting) {
        return new WaitingQueueResponse("WAITING", position, totalWaiting,
                "대기 중입니다. 순번: " + position);
    }

    public static WaitingQueueResponse active() {
        return new WaitingQueueResponse("ACTIVE", 0, 0, "쿠폰 발급이 가능합니다.");
    }

    public static WaitingQueueResponse completed(String message) {
        return new WaitingQueueResponse("COMPLETED", 0, 0, message);
    }
}