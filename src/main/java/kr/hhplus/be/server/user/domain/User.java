package kr.hhplus.be.server.user.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.InsufficientBalanceException;
import kr.hhplus.be.server.coupon.domain.Coupon;
import kr.hhplus.be.server.order.domain.Order;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "point_balance", nullable = false)
    private Integer pointBalance = 0;

//    @Version
//    private Long version;

    // 최대 포인트 한도 상수
    private static final int MAX_POINT_BALANCE = 1000000;

    // 포인트 충전
    public void chargePoint(int amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        if (this.pointBalance + amount > MAX_POINT_BALANCE) {
            throw new IllegalArgumentException("최대 포인트 한도(" + MAX_POINT_BALANCE + ")를 초과할 수 없습니다.");
        }

        this.pointBalance += amount;
    }

    // 포인트 사용
    public void usePoint(int amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        if(this.pointBalance < amount){
            throw new InsufficientBalanceException("포인트가 부족합니다.");
        }

        this.pointBalance -= amount;
    }
}