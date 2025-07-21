package kr.hhplus.be.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "point_balance", nullable = false)
    private Integer pointBalance;

    @Column(name = "user_grade", nullable = false)
    private String userGrade = "NORMAL";
}