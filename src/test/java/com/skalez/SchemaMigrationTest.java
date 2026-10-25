package com.skalez;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway 마이그레이션이 로컬 H2(PostgreSQL 모드)에서 끝까지 적용되고,
 * 그 위에서 Hibernate validate(엔티티 ↔ 테이블 대조)를 통과해 컨텍스트가 뜨는지 확인한다.
 */
@SpringBootTest
class SchemaMigrationTest {

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 마이그레이션이_모두_성공_상태다() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(info -> assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }

    @Test
    void 샘플_데이터가_마이그레이션으로_만든_테이블에_들어간다() {
        // LocalDataInitializer가 넣은 카테고리·상품이 조회된다 = 테이블이 Flyway SQL로 만들어졌고 엔티티 매핑과 맞는다
        assertThat(jdbcTemplate.queryForObject("select count(*) from categories", Integer.class)).isPositive();
        assertThat(jdbcTemplate.queryForObject("select count(*) from product_variants", Integer.class)).isPositive();
    }
}
