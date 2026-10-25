package com.skalez.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** 브라우저가 실제 요청 전에 보내는 사전 요청(preflight)이 설정한 FE 주소에서만 통과하는지 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void 설정한_FE_주소의_사전_요청은_통과한다() {
        assertThat(preflight("http://localhost:3002"))
                .hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "http://localhost:3002");
    }

    @Test
    void 와일드카드_패턴에_맞는_미리보기_주소도_통과한다() {
        assertThat(preflight("https://skalez-git-main.vercel.app"))
                .hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "https://skalez-git-main.vercel.app");
    }

    @Test
    void 설정에_없는_주소는_거부된다() {
        assertThat(preflight("https://evil.example.com"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .doesNotContainHeader("Access-Control-Allow-Origin");
    }

    private MvcTestResult preflight(String origin) {
        return mvc.options().uri("/api/products")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .exchange();
    }
}
