package com.skalez.audit.service;

import com.skalez.audit.domain.AdminAccessLog;
import com.skalez.audit.repository.AdminAccessLogRepository;
import com.skalez.global.web.PageResponse;
import com.skalez.member.domain.Member;
import com.skalez.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccessLogService {
    static final Duration KEEP = Duration.ofDays(730);
    private final AdminAccessLogRepository accessLogRepository;
    private final MemberRepository memberRepository;
    public record Row(Long id, Long memberId, String memberEmail, String method, String path, String query, int status, String ip, String clientIp, Instant accessedAt){
        static Row from(AdminAccessLog log, String memberEmail){
            return new Row(log.getId(), log.getMemberId(), memberEmail, log.getMethod(), log.getPath(), log.getQuery(), log.getStatus(), log.getIp(), log.getClientIp(), log.getAccessedAt());
        }
    }
    @Transactional
    public void record(AdminAccessLog log){accessLogRepository.save(log);}
    @Transactional(readOnly = true)
    public PageResponse<Row> list(Pageable pageable){
        Page<AdminAccessLog> logs = accessLogRepository.findAllByOrderByIdDesc(pageable);
        Set<Long> memberIds = logs.stream().map(AdminAccessLog::getMemberId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> emails = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getEmail));
        return PageResponse.from(logs.map(log -> Row.from(log, emails.get(log.getMemberId()))));
    }
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpired(){
        int deleted = accessLogRepository.deleteOlderThan(Instant.now().minus(KEEP));
        if(deleted > 0){
            log.info("2년 지난 관리자 접속기록 삭제: {}건", deleted);
        }
    }
}
