package kr.hhplus.be.server.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_point_histories")
@Getter @Setter
public class PointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long pointHistoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "point_type", nullable = false)
    private String pointType;

    @Column(name = "created_at",  nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 포인트 내역 생성 - 충전
    public static PointHistory createChargeHistory(Long userId, int amount) {
        PointHistory history = new PointHistory();
        history.setUserId(userId);
        history.setAmount(amount);
        history.setPointType("CHARGE");
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }

    // 포인트 내역 생성 - 사용
    public static PointHistory createUseHistory(Long userId, int amount) {
        PointHistory history = new PointHistory();
        history.setUserId(userId);
        history.setAmount(-amount); // 음수로 저장
        history.setPointType("USE");
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }
}
