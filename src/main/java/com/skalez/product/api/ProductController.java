package com.skalez.product.api;

import com.skalez.global.web.PageResponse;
import com.skalez.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @GetMapping
    public PageResponse<ProductResponse> getProducts(@RequestParam(name = "category", required = false) String categoryCode, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, Locale locale) {return productService.getProducts(categoryCode, pageable, locale);}
    /** 예: GET /api/products/sku/SKZ-166 */
    @GetMapping("/sku/{sku}")
    public ProductResponse getProductBySku(@PathVariable String sku, Locale locale) {return productService.getProductBySku(sku, locale);}
    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id, Locale locale) {return productService.getProduct(id, locale);}
}