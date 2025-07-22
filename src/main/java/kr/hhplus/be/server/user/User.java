package kr.hhplus.be.server.user;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.InsufficientBalanceException;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "point_balance", nullable = false)
    private Integer pointBalance;

    @Column(name = "user_grade", nullable = false)
    private String userGrade = "NORMAL";

    // 포인트 충전
    public void chargePoint(int amount){
        this.pointBalance += amount;
    }

    // 포인트 사용
    public void usePoint(int amount){
        if(this.pointBalance < amount){
            throw new InsufficientBalanceException("포인트가 부족합니다.");
        }
        this.pointBalance -= amount;
    }
}