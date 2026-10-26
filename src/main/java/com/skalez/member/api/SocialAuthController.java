package com.skalez.member.api;

import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.domain.SocialProvider;
import com.skalez.member.oauth.SocialAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 간편 로그인 API. 브라우저는 FE 서버(/api/auth/oauth/...)를 거치고, 키·토큰 교환은 여기서만 한다.
 * (/api/auth/** 는 로그인 없이 부를 수 있다)
 */
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    public record ProviderResponse(String provider, boolean enabled) {}
    public record AuthorizeUrlResponse(String url) {}
    public record LoginRequest(@NotBlank @Size(max = 2000) String code, @NotBlank @Size(max = 500) String state, @NotBlank @Size(max = 500) String redirectUri) {}
    /** token 이 있으면 바로 로그인, ticket 이 있으면 약관 동의 화면으로 */
    public record LoginResponse(TokenResponse token, String ticket, String email, String name) {}
    public record SignupRequest(@NotBlank @Size(max = 100) String ticket, @Size(max = 50) String name,
                                @NotNull @AssertTrue Boolean agreeTerms, Boolean marketingConsent) {}

    /** 어떤 간편 로그인이 켜져 있는지 (FE 버튼 표시용) */
    @GetMapping("/providers")
    public List<ProviderResponse> providers() {
        return Arrays.stream(SocialProvider.values()).map(p -> new ProviderResponse(p.path(), socialAuthService.enabled(p))).toList();
    }

    @GetMapping("/{provider}/authorize-url")
    public AuthorizeUrlResponse authorizeUrl(@PathVariable String provider, @RequestParam String redirectUri, @RequestParam String state) {
        return new AuthorizeUrlResponse(socialAuthService.authorizeUrl(SocialProvider.fromPath(provider), redirectUri, state));
    }

    @PostMapping("/{provider}/login")
    public LoginResponse login(@PathVariable String provider, @Valid @RequestBody LoginRequest request) {
        SocialAuthService.Result result = socialAuthService.login(SocialProvider.fromPath(provider), request.code(), request.redirectUri(), request.state());
        return new LoginResponse(result.token(), result.ticket(), result.email(), result.name());
    }

    @PostMapping("/signup")
    public TokenResponse signup(@Valid @RequestBody SignupRequest request, Locale locale) {
        return socialAuthService.completeSignup(request.ticket(), request.name(), Boolean.TRUE.equals(request.marketingConsent()), locale);
    }
}
