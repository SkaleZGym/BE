package com.skalez.product.api;

import com.skalez.global.web.PageResponse;
import com.skalez.product.api.AdminProductDtos.AdminProductResponse;
import com.skalez.product.api.AdminProductDtos.ChangeStatusRequest;
import com.skalez.product.api.AdminProductDtos.ChangeStockRequest;
import com.skalez.product.api.AdminProductDtos.ProductSaveRequest;
import com.skalez.product.domain.ProductStatus;
import com.skalez.product.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final AdminProductService adminProductService;
    @GetMapping
    public PageResponse<AdminProductResponse> getProducts(@RequestParam(required = false) ProductStatus status, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {return adminProductService.getProducts(status, pageable);}
    @GetMapping("/{productId}")
    public AdminProductResponse getProduct(@PathVariable Long productId) {return adminProductService.getProduct(productId);}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse create(@Valid @RequestBody ProductSaveRequest request) {return adminProductService.create(request);}
    @PutMapping("/{productId}")
    public AdminProductResponse update(@PathVariable Long productId, @Valid @RequestBody ProductSaveRequest request) {return adminProductService.update(productId, request);}
    @PatchMapping("/{productId}/status")
    public AdminProductResponse changeStatus(@PathVariable Long productId, @Valid @RequestBody ChangeStatusRequest request) {return adminProductService.changeStatus(productId, request.status());}
    @PatchMapping("/{productId}/variants/{variantId}/stock")
    public AdminProductResponse changeStock(@PathVariable Long productId, @PathVariable Long variantId, @Valid @RequestBody ChangeStockRequest request) {return adminProductService.changeStock(productId, variantId, request.stock());}
}