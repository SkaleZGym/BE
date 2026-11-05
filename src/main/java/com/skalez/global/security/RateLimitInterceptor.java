package com.skalez.global.security;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor, WebMvcConfigurer{
    public static final String CLIENT_IP_HEADER = "X-Client-Ip";
    public static final String BFF_SECRET_HEADER = "X-Bff-Secret";
    record Rule(String name, HttpMethod method, String pattern, int max, Duration window){}
    static final List<Rule> RULES = List.of(
            new Rule("login", HttpMethod.POST, "/api/auth/login", 20, Duration.ofMinutes(10)),
            new Rule("admin-login", HttpMethod.POST, "/api/admin/auth/login", 10, Duration.ofMinutes(10)),
            new Rule("signup", HttpMethod.POST, "/api/auth/signup", 5, Duration.ofHours(1)),
            new Rule("social", HttpMethod.POST, "/api/auth/oauth/**", 30, Duration.ofMinutes(10)),
            new Rule("password-reset", HttpMethod.POST, "/api/auth/password-reset", 5, Duration.ofHours(1)),
            new Rule("password-reset-confirm", HttpMethod.POST, "/api/auth/password-reset/confirm", 10, Duration.ofMinutes(10)),
            new Rule("coupon", HttpMethod.POST, "/api/coupons/preview", 20, Duration.ofMinutes(10)),
            new Rule("inquiry", HttpMethod.POST, "/api/inquiries", 10, Duration.ofHours(1)),
            new Rule("review", HttpMethod.POST, "/api/reviews", 10, Duration.ofHours(1)));
    private static final Duration LONGEST = Duration.ofHours(1);
    private static final AntPathMatcher PATHS = new AntPathMatcher();
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final byte[] bffSecret;
    public RateLimitInterceptor(@Value("${skalez.security.bff-secret:}")String bffSecret){this.bffSecret = bffSecret.getBytes(StandardCharsets.UTF_8);}
    @Override
    public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(this).addPathPatterns("/api/**");}
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        for (Rule rule : RULES) {if (rule.method().matches(request.getMethod()) && PATHS.match(rule.pattern(), path)) {hit(rule, clientIp(request));}}
        return true;
    }
    private void hit(Rule rule, String ip){
        Deque<Instant> recent = hits.computeIfAbsent(rule.name() + "|" + ip, key -> new ArrayDeque<>());
        Instant now = Instant.now();
        synchronized(recent){
            prune(recent, now.minus(rule.window()));
            if(recent.size() >= rule.max()){
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
            recent.addLast(now);
        }
    }
    String clientIp(HttpServletRequest request){
        String forwarded = request.getHeader(CLIENT_IP_HEADER);
        String secret = request.getHeader(BFF_SECRET_HEADER);
        if(bffSecret.length > 0 && forwarded != null && !forwarded.isBlank() && secret != null && MessageDigest.isEqual(bffSecret,secret.getBytes(StandardCharsets.UTF_8))){
            return forwarded.trim();
        }
        return request.getRemoteAddr();
    }
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void cleanUp(){
        Instant from = Instant.now().minus(LONGEST);
        hits.values().removeIf(recent -> {
            synchronized(recent){
                prune(recent, from);
                return recent.isEmpty();
            }
        });
    }
    private static void prune(Deque<Instant> recent, Instant from){
        while(!recent.isEmpty() && recent.peekFirst().isBefore(from)){
            recent.pollFirst();
        }
    }
}
