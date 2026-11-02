package com.skalez.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PointType type;
    @Column(nullable = false)
    private long amount;
    @Column(nullable = false)
    private long balanceAfter;
    @Column(length = 30)
    private String orderNumber;
    @Column(length = 200)
    private String memo;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    public PointHistory(Long memberId, PointType type, long amount, long balanceAfter, String orderNumber, String memo) {
        this.memberId = memberId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.orderNumber = orderNumber;
        this.memo = memo;
        this.createdAt = Instant.now();
    }
}