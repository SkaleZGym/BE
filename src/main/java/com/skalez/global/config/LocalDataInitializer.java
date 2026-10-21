package com.skalez.global.config;

import com.skalez.category.domain.Category;
import com.skalez.category.repository.CategoryRepository;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.product.domain.Badge;
import com.skalez.product.domain.Product;
import com.skalez.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.skalez.global.i18n.SupportedLanguage.*;

/** 로컬 실행 시 기존 skalez.com 상품 일부로 샘플 데이터를 만든다 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private static final String CDN = "https://cdn.imweb.me/thumbnail";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        Category men = save(new Category("MEN", null, 1)
                .name(KO, "남성").name(EN, "Men").name(JA, "メンズ").name(ZH_CN, "男装"));
        Category menTop = save(new Category("MEN_TOP", men, 2)
                .name(KO, "상의").name(EN, "Tops").name(JA, "トップス").name(ZH_CN, "上衣"));
        save(new Category("MEN_TIGHTS", men, 3)
                .name(KO, "타이즈").name(EN, "Tights").name(JA, "タイツ").name(ZH_CN, "紧身裤"));
        Category women = save(new Category("WOMEN", null, 4)
                .name(KO, "여성").name(EN, "Women").name(JA, "ウィメンズ").name(ZH_CN, "女装"));
        save(new Category("WOMEN_LEGGINGS", women, 5)
                .name(KO, "레깅스").name(EN, "Leggings").name(JA, "レギンス").name(ZH_CN, "打底裤"));
        save(new Category("GOODS", null, 6)
                .name(KO, "굿즈").name(EN, "Goods").name(JA, "グッズ").name(ZH_CN, "周边"));

        Product dragon = new Product("SKZ-292", menTop, CDN + "/20260927/413a3c65358d441c.png");
        dragon.addTranslation(KO, "드래곤 오버핏 티셔츠", "네추럴 보디빌더 곽현성(KAIDO)과 SKALEZ의 콜라보.");
        dragon.addTranslation(EN, "DRAGON SCALES Oversized T-shirt", "A collaboration between natural bodybuilder KAIDO and SKALEZ.");
        dragon.addTranslation(JA, "ドラゴンスケイル オーバーサイズTシャツ", "ナチュラルボディビルダーKAIDOとSKALEZのコラボ。");
        dragon.addTranslation(ZH_CN, "龙鳞宽松T恤", "自然健美运动员 KAIDO 与 SKALEZ 的联名。");
        dragon.putPrice("KRW", 69_000, 79_000L);
        dragon.putPrice("USD", 49_00, 59_00L);
        dragon.addBadge(Badge.NEW);
        dragon.addBadge(Badge.SALE);
        addSizes(dragon, "L", "XL");
        productRepository.save(dragon);

        Product madness = new Product("SKZ-291", menTop, CDN + "/20260927/5b5dac00c0fe4517.png");
        madness.addTranslation(KO, "매드니스 세미 오버핏 티셔츠", null);
        madness.addTranslation(EN, "MADNESS Semi-Oversized T-shirt", null);
        madness.addTranslation(JA, "マッドネス セミオーバーサイズTシャツ", null);
        madness.addTranslation(ZH_CN, "MADNESS 半宽松T恤", null);
        madness.putPrice("KRW", 59_000, 69_000L);
        madness.putPrice("USD", 39_00, 49_00L);
        madness.addBadge(Badge.NEW);
        madness.addBadge(Badge.SALE);
        addSizes(madness, "M", "L", "XL");
        productRepository.save(madness);

        Product heavy = new Product("SKZ-278", menTop, CDN + "/20250701/237cfa5bb37bd.jpg");
        heavy.addTranslation(KO, "헤비 웨이트 오버핏 티셔츠 화이트", null);
        heavy.addTranslation(EN, "Heavy Weight Oversized T-Shirt White", null);
        heavy.addTranslation(JA, "ヘビーウェイト オーバーサイズTシャツ ホワイト", null);
        heavy.addTranslation(ZH_CN, "重磅宽松T恤 白色", null);
        heavy.putPrice("KRW", 43_000, null);
        heavy.putPrice("USD", 42_18, null);
        heavy.addBadge(Badge.BEST);
        heavy.addBadge(Badge.HOT);
        addSizes(heavy, "M", "L", "XL", "2XL", "3XL");
        productRepository.save(heavy);

        Product sleeveless = new Product("SKZ-272", menTop, CDN + "/20250625/606212dc38d43.jpg");
        sleeveless.addTranslation(KO, "워크아웃 숄더라인 슬리브리스 블랙", null);
        sleeveless.addTranslation(EN, "Workout =/ Homework Sleeveless Black", null);
        sleeveless.addTranslation(JA, "ワークアウト スリーブレス ブラック", null);
        sleeveless.addTranslation(ZH_CN, "Workout 无袖背心 黑色", null);
        sleeveless.putPrice("KRW", 38_000, null);
        sleeveless.putPrice("USD", 36_91, null);
        sleeveless.addBadge(Badge.BEST);
        addSizes(sleeveless, "M", "L", "XL");
        productRepository.save(sleeveless);
    }

    private Category save(Category category) {
        return categoryRepository.save(category);
    }

    /** 샘플이라 모든 사이즈 재고 20개 */
    private void addSizes(Product product, String... sizes) {
        for (String size : sizes) {
            product.addVariant(size, 20);
        }
    }
}