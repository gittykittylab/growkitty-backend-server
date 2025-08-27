package kr.hhplus.be.server.order.infrastructure.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StatusChangePlatformPayload {
    private Long orderId;
    private String previousStatus;
    private String currentStatus;
    private String changeReason;
    private LocalDateTime changedAt;
}
