package com.reelshop.controller;

import com.reelshop.dto.response.ApiResponse;
import com.reelshop.entity.Seller;
import com.reelshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductService.ProductDto>>> list(
            @AuthenticationPrincipal Seller seller) {
        return ResponseEntity.ok(ApiResponse.ok(productService.listProducts(seller.getId())));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductService.ProductDto>> create(
            @AuthenticationPrincipal Seller seller,
            @RequestParam String name,
            @RequestParam String price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) List<MultipartFile> images) {

        var req = ProductService.CreateRequest.builder()
                .name(name).price(price)
                .description(description)
                .stockStatus(stockStatus)
                .images(images)
                .build();

        return ResponseEntity.ok(
            ApiResponse.ok("Product created!", productService.createProduct(seller.getId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductService.ProductDto>> update(
            @AuthenticationPrincipal Seller seller,
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String stockStatus) {

        var req = ProductService.UpdateRequest.builder()
                .name(name).price(price)
                .description(description).stockStatus(stockStatus)
                .build();

        return ResponseEntity.ok(
            ApiResponse.ok(productService.updateProduct(seller.getId(), id, req)));
    }

    @PatchMapping("/{id}/toggle-stock")
    public ResponseEntity<ApiResponse<ProductService.ProductDto>> toggleStock(
            @AuthenticationPrincipal Seller seller,
            @PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.ok(productService.toggleStock(seller.getId(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Seller seller,
            @PathVariable Long id) {
        productService.deleteProduct(seller.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getQr(
            @AuthenticationPrincipal Seller seller,
            @PathVariable Long id) throws Exception {
        byte[] png = productService.generateQrPng(seller.getId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reelshop-qr.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
