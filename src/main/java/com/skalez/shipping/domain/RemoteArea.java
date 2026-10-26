package com.skalez.shipping.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "remote_areas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RemoteArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 10)
    private RemoteAreaType type;
    @Column(nullable = false)
    private int zipFrom;
    @Column(nullable = false)
    private int zipTo;
    @Column(length = 100)
    private String memo;
    public RemoteArea(RemoteAreaType type, int zipFrom, int zipTo, String memo){
        if(zipFrom < 0 || zipTo > 99999 || zipFrom > zipTo){
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        this.type = type;
        this.zipFrom = zipFrom;
        this.zipTo = zipTo;
        this.memo = memo;
    }
}
