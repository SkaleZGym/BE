package com.skalez.category.service;

import com.skalez.category.api.AdminCategoryDtos.AdminCategoryResponse;
import com.skalez.category.api.AdminCategoryDtos.CategorySaveRequest;
import com.skalez.category.domain.Category;
import com.skalez.category.repository.CategoryRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.product.domain.Product;
import com.skalez.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional // 테스트마다 롤백돼서 로컬 샘플 데이터와 섞이지 않는다
class AdminCategoryServiceTest {

    @Autowired
    AdminCategoryService service;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void 최상위와_하위_카테고리를_만들고_수정한다() {
        AdminCategoryResponse top = service.create(request("TEST_KIDS", null, 90, Map.of("ko", "키즈", "en", "Kids")));
        AdminCategoryResponse child = service.create(request("TEST_KIDS_TOP", "TEST_KIDS", 91, Map.of("ko", "키즈 상의")));

        assertThat(top.parentCode()).isNull();
        assertThat(top.names()).containsEntry("ko", "키즈").containsEntry("en", "Kids");
        assertThat(child.parentCode()).isEqualTo("TEST_KIDS");

        AdminCategoryResponse updated = service.update(child.id(),
                request("TEST_KIDS_TEE", "TEST_KIDS", 92, Map.of("ko", "키즈 티셔츠", "ja", "キッズTシャツ")));

        assertThat(updated.code()).isEqualTo("TEST_KIDS_TEE");
        assertThat(updated.sortOrder()).isEqualTo(92);
        assertThat(updated.names()).containsOnlyKeys("ko", "ja");
        assertThat(service.getCategories()).extracting(AdminCategoryResponse::code).contains("TEST_KIDS", "TEST_KIDS_TEE");
    }

    @Test
    void 코드가_겹치거나_한국어_이름이_없거나_상위가_없으면_만들_수_없다() {
        service.create(request("TEST_DUP", null, 1, Map.of("ko", "중복")));

        assertThatThrownBy(() -> service.create(request("TEST_DUP", null, 2, Map.of("ko", "또 중복"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_CODE_DUPLICATED);
        assertThatThrownBy(() -> service.create(request("TEST_NO_KO", null, 3, Map.of("en", "English only"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.create(request("TEST_ORPHAN", "NO_SUCH_PARENT", 4, Map.of("ko", "고아"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void 카테고리는_두_단계까지만_허용된다() {
        AdminCategoryResponse top = service.create(request("TEST_A", null, 1, Map.of("ko", "A")));
        AdminCategoryResponse child = service.create(request("TEST_A_B", "TEST_A", 2, Map.of("ko", "A-B")));
        AdminCategoryResponse other = service.create(request("TEST_X", null, 3, Map.of("ko", "X")));

        // 하위의 하위
        assertThatThrownBy(() -> service.create(request("TEST_A_B_C", "TEST_A_B", 4, Map.of("ko", "A-B-C"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_PARENT_INVALID);
        // 자기 자신을 상위로
        assertThatThrownBy(() -> service.update(other.id(), request("TEST_X", "TEST_X", 3, Map.of("ko", "X"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_PARENT_INVALID);
        // 하위가 있는 카테고리를 다른 카테고리 아래로
        assertThatThrownBy(() -> service.update(top.id(), request("TEST_A", "TEST_X", 1, Map.of("ko", "A"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_PARENT_INVALID);

        // 하위를 다른 최상위 아래로 옮기는 것은 된다
        assertThat(service.update(child.id(), request("TEST_A_B", "TEST_X", 2, Map.of("ko", "A-B"))).parentCode())
                .isEqualTo("TEST_X");
    }

    @Test
    void 상품이나_하위가_있으면_지울_수_없고_없으면_지워진다() {
        AdminCategoryResponse top = service.create(request("TEST_DEL", null, 1, Map.of("ko", "삭제")));
        AdminCategoryResponse child = service.create(request("TEST_DEL_SUB", "TEST_DEL", 2, Map.of("ko", "삭제 하위")));
        AdminCategoryResponse leaf = service.create(request("TEST_LEAF", null, 3, Map.of("ko", "잎")));

        assertThatThrownBy(() -> service.delete(top.id()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_IN_USE);

        Category childEntity = categoryRepository.findByCode("TEST_DEL_SUB").orElseThrow();
        Product product = new Product("TEST-SKU-1", childEntity, null);
        product.addTranslation(SupportedLanguage.KO, "테스트 상품", null);
        product.putPrice("KRW", 10_000, null);
        product.addVariant("M", 1);
        productRepository.save(product);

        assertThatThrownBy(() -> service.delete(child.id()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_IN_USE);

        service.delete(leaf.id());
        assertThat(categoryRepository.findByCode("TEST_LEAF")).isEmpty();
    }

    private static CategorySaveRequest request(String code, String parentCode, int sortOrder, Map<String, String> names) {
        return new CategorySaveRequest(code, parentCode, sortOrder, names);
    }
}
