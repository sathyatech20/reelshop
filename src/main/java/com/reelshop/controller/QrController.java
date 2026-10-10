package com.reelshop.controller;

import com.reelshop.dto.response.ApiResponse;
import com.reelshop.entity.Seller;
import com.reelshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
public class QrController {

    private final ProductService productService;

    /**
     * GET /api/v1/qr/history
     * Returns all seller products with scan stats for QR History screen
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ProductService.ProductDto>>> history(
            @AuthenticationPrincipal Seller seller) {
        return ResponseEntity.ok(
            ApiResponse.ok(productService.listProducts(seller.getId())));
    }
}
