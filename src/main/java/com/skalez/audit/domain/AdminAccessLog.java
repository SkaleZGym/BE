package com.skalez.audit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "admin_access_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id")
    private Long memberId;
    @Column(nullable = false, length = 10)
    private String method;
    @Column(nullable = false, length = 300)
    private String path;
    @Column(length = 500)
    private String query;
    @Column(nullable = false)
    private int status;
    @Column(nullable = false, length = 64)
    private String ip;
    @Column(length = 64)
    private String clientIp;
    @Column(nullable = false, updatable = false)
    private Instant accessedAt;
    public AdminAccessLog(Long memberId, String method, String path, String query, int status, String ip, String clientIp){
        this.memberId = memberId;
        this.method = method;
        this.path = cut(path, 300);
        this.query = cut(query, 500);
        this.status = status;
        this.ip = cut(ip, 64);
        this.clientIp = cut(clientIp, 64);
        this.accessedAt = Instant.now();
    }
    private static String cut(String value, int max){return value == null || value.length() <= max ? value : value.substring(0, max);}

}
