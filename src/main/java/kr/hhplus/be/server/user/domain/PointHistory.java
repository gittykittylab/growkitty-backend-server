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
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "point_type", nullable = false)
    private String pointType;

    @Column(name = "created_dt")
    private LocalDateTime createdAt;
}
