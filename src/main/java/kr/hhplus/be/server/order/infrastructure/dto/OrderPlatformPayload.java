package kr.hhplus.be.server.order.infrastructure.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPlatformPayload {
    private String order_id;        // String으로 변경 (접두어 포함)
    private Long customer_id;       // 필드명 변경
    private Integer amount_krw;     // 필드명 변경
    private String timestamp;       // String으로 변경
}