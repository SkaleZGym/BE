package com.skalez.audit.api;
import com.skalez.audit.domain.AdminAccessLog;
import com.skalez.audit.service.AdminAccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class AdminAccessLogFilter extends OncePerRequestFilter {
    public static final String CLIENT_IP_HEADER = "X-Client-Ip";
    private final AdminAccessLogService accessLogService;
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        return !request.getRequestURI().startsWith("/api/admin");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException{
        try{
            chain.doFilter(request, response);
        }finally{
            record(request, response);
        }
    }
    private void record(HttpServletRequest request, HttpServletResponse response){
        try{
            accessLogService.record(new AdminAccessLog(memberId(), request.getMethod(), request.getRequestURI(),
                    request.getQueryString(), response.getStatus(), request.getRemoteAddr(), request.getHeader(CLIENT_IP_HEADER)));
        }catch(RuntimeException e){
            log.error("관리자 접속기록 저장 실패: {} {}", request.getMethod(), request.getRequestURI(), e);
        }
    }
    private static Long memberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? Long.valueOf(jwt.getToken().getSubject()) : null;
    }
}
