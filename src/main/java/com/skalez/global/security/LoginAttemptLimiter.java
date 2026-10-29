package com.skalez.global.security;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비밀번호 대입 막기: 같은 이메일로 15분 안에 5번 틀리면 15분 동안 로그인을 막는다.
 * 서버 한 대 기준 메모리에 둔다 (서버를 다시 켜면 초기화). 로그인에 성공하면 기록을 지운다
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    /** 막힌 상태면 TOO_MANY_LOGIN_ATTEMPTS */
    public void check(String email) {
        Deque<Instant> recent = failures.get(email);
        if (recent == null) {
            return;
        }
        synchronized (recent) {
            prune(recent, Instant.now());
            if (recent.size() >= MAX_FAILURES) {
                throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Instant> recent = failures.computeIfAbsent(email, key -> new ArrayDeque<>());
        synchronized (recent) {
            Instant now = Instant.now();
            prune(recent, now);
            recent.addLast(now);
        }
        // 메모리가 계속 늘지 않게 가끔 오래된 기록을 정리한다
        if (failures.size() > 10_000) {
            Instant now = Instant.now();
            failures.values().removeIf(deque -> {
                synchronized (deque) {
                    prune(deque, now);
                    return deque.isEmpty();
                }
            });
        }
    }

    public void reset(String email) {
        failures.remove(email);
    }

    private static void prune(Deque<Instant> recent, Instant now) {
        Instant from = now.minus(WINDOW);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(from)) {
            recent.pollFirst();
        }
    }
}
