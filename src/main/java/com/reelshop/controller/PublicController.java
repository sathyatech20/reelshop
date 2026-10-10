package com.reelshop.controller;

import com.reelshop.dto.response.ApiResponse;
import com.reelshop.entity.Seller;
import com.reelshop.repository.SellerRepository;
import com.reelshop.service.ProductService;
import com.reelshop.service.SellerService;
import com.reelshop.exception.ReelShopException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicController {

    private final SellerRepository sellerRepository;
    private final SellerService    sellerService;
    private final ProductService   productService;

    /** GET /api/v1/s/{slug} — public shop page */
    @GetMapping("/s/{slug}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShop(@PathVariable String slug) {
        Seller seller = sellerRepository.findByShopSlug(slug)
                .orElseThrow(() -> ReelShopException.notFound("Shop"));

        List<ProductService.ProductDto> products = productService.listProducts(seller.getId());

        ShopResponse response = new ShopResponse(
            sellerService.toDto(seller),
            products
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** GET /api/v1/p/{shortCode} — public product page */
    @GetMapping("/p/{shortCode}")
    public ResponseEntity<ApiResponse<ProductService.ProductDto>> getProduct(
            @PathVariable String shortCode) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getByShortCode(shortCode)));
    }

    /** POST /api/v1/track/scan/{shortCode} — track QR scan */
    @PostMapping("/track/scan/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> trackScan(
            @PathVariable String shortCode,
            @RequestBody(required = false) Map<String, String> body) {
        productService.trackScan(shortCode);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** POST /api/v1/track/whatsapp/{productId} — track WhatsApp click */
    @PostMapping("/track/whatsapp/{productId}")
    public ResponseEntity<ApiResponse<Void>> trackWhatsapp(@PathVariable Long productId) {
        productService.trackWhatsapp(productId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record ShopResponse(
        SellerService.SellerDto seller,
        List<ProductService.ProductDto> products
    ) {}
}
