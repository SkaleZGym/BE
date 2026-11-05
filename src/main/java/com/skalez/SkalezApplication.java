package com.skalez;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkalezApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SkalezApplication.class);
		ApplicationListener<ApplicationEnvironmentPreparedEvent> guard = SkalezApplication::refuseUnsafeSettings;
		app.addListeners(guard);
		app.run(args);
	}

	/**
	 * 서버에서 설정 실수로 위험하게 뜨는 것을 막는다 (뜨지 않고 이유를 남긴다).
	 * - 운영 환경변수(DB_URL)가 있는데 local 프로필: SPRING_PROFILES_ACTIVE=prod 를 빠뜨린 것.
	 *   그대로 뜨면 메모리 DB·저장소에 공개된 비밀키·시험 관리자(admin1234)·가짜 결제로 돌아간다
	 * - local 이 아닌데 가짜 결제·가짜 간편 로그인·로컬 비밀키·짧은 관리자 비밀번호·FE 공유 비밀값(BFF_SECRET) 없음
	 */
	static void refuseUnsafeSettings(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment env = event.getEnvironment();
		if (env.matchesProfiles("local")) {
			if (env.containsProperty("DB_URL")) {
				throw new IllegalStateException("운영 환경변수(DB_URL)가 있는데 local 프로필로 실행됨 — SPRING_PROFILES_ACTIVE=prod 를 넣어 주세요");
			}
			return;
		}
		if ("fake".equals(env.getProperty("skalez.payment.provider"))) {
			throw new IllegalStateException("운영에서 가짜 결제(PAYMENT_PROVIDER=fake)는 쓸 수 없어요");
		}
		if (env.getProperty("skalez.oauth.fake", Boolean.class, false)) {
			throw new IllegalStateException("운영에서 가짜 간편 로그인(skalez.oauth.fake)은 쓸 수 없어요");
		}
		if (env.getProperty("JWT_SECRET", "").startsWith("local-only")) {
			throw new IllegalStateException("로컬용 JWT 비밀키로 운영 서버를 띄울 수 없어요 — JWT_SECRET 을 새로 만들어 넣어 주세요");
		}
		String adminPassword = env.getProperty("ADMIN_PASSWORD", "");
		if (!adminPassword.isEmpty() && adminPassword.length() < 12) {
			throw new IllegalStateException("첫 관리자 비밀번호(ADMIN_PASSWORD)는 12자 이상으로 넣어 주세요");
		}
		// 없으면 요청 횟수 제한이 손님 전체를 한 사람으로 세서, 몇 명만 로그인해도 모두 막힌다
		if (env.getProperty("BFF_SECRET", "").length() < 32) {
			throw new IllegalStateException("BFF_SECRET(32자 이상)을 넣어 주세요 — Vercel 환경변수 BFF_SECRET 과 같은 값");
		}
	}

}
