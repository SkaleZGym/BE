package com.skalez.global.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.skalez.global.security.CorsProperties;
import com.skalez.global.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 쿠키 세션이 아닌 토큰 방식이라 CSRF 불필요
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // 나이스페이 결제창이 결과를 보내는 주소 (로그인 토큰 없음, 서명으로 검증)
                        .requestMatchers(HttpMethod.POST, "/api/payments/nice/return").permitAll()
                        // 관리자 로그인은 로그인 전이라 열어 두고, 나머지 /api/admin/** 은 ADMIN만
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**", "/api/categories/**", "/api/banners/**", "/api/site-texts/**",
                                "/api/notices/**", "/api/faqs/**", "/api/shipping/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * 비밀번호 해시. 기본은 BCrypt이고 저장값 앞에 {bcrypt}가 붙는다.
     * 나중에 더 강한 방식(예: Argon2)으로 바꿔도 기존 회원 비밀번호를 그대로 검증할 수 있다.
     * MD5·SHA-256 같은 단순 해시는 너무 빨라서 비밀번호에는 쓰면 안 된다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * 브라우저에서 API를 부를 수 있는 FE 주소. 로컬·운영이 달라서 코드에 박지 않고 설정(skalez.cors.allowed-origins)에서 읽는다.
     * 운영은 환경변수 CORS_ALLOWED_ORIGINS. 비어 있으면 아무 브라우저도 API를 못 쓰게 되므로 서버 시작 시 바로 실패시킨다
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = corsProperties.allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException("CORS 허용 오리진이 없습니다. 환경변수 CORS_ALLOWED_ORIGINS에 FE 주소를 넣어 주세요");
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L); // 브라우저가 사전 요청(preflight) 결과를 1시간 기억

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** 토큰의 roles 클레임(["USER"])을 Spring 권한(ROLE_USER)으로 변환 */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** HS256은 32바이트 이상 키가 필요하다. 짧으면 서버 시작 시 바로 실패시킨다 */
    private SecretKey secretKey() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalStateException("JWT 비밀키가 없습니다. 환경변수 JWT_SECRET을 설정해 주세요");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT 비밀키는 32바이트 이상이어야 합니다");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
