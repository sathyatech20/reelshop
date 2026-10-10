package com.reelshop.controller;

import com.reelshop.dto.response.ApiResponse;
import com.reelshop.entity.Seller;
import com.reelshop.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /** GET /api/v1/seller/check-slug?slug=xyz */
    @GetMapping("/check-slug")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkSlug(
            @RequestParam String slug) {
        boolean available = sellerService.isSlugAvailable(slug);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("available", available)));
    }

    /** POST /api/v1/seller/setup */
    @PostMapping(value = "/setup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SellerService.SellerDto>> setupShop(
            @AuthenticationPrincipal Seller seller,
            @RequestParam String shopName,
            @RequestParam String shopSlug,
            @RequestParam String category,
            @RequestParam String whatsappNumber,
            @RequestParam(required = false) String upiId,
            @RequestParam(required = false) String shopBio,
            @RequestParam(required = false) MultipartFile profileImage) {

        var req = SellerService.SetupRequest.builder()
                .shopName(shopName).shopSlug(shopSlug)
                .category(category).whatsappNumber(whatsappNumber)
                .upiId(upiId).shopBio(shopBio)
                .profileImage(profileImage)
                .build();

        return ResponseEntity.ok(
            ApiResponse.ok("Shop created!", sellerService.setupShop(seller.getId(), req)));
    }

    /** GET /api/v1/seller/me */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SellerService.SellerDto>> getMe(
            @AuthenticationPrincipal Seller seller) {
        return ResponseEntity.ok(ApiResponse.ok(sellerService.getSellerDto(seller.getId())));
    }

    /** PUT /api/v1/seller/me */
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SellerService.SellerDto>> updateProfile(
            @AuthenticationPrincipal Seller seller,
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String whatsappNumber,
            @RequestParam(required = false) String upiId,
            @RequestParam(required = false) String shopBio,
            @RequestParam(required = false) String themeId,
            @RequestParam(required = false) MultipartFile profileImage) {

        var req = SellerService.UpdateRequest.builder()
                .shopName(shopName).whatsappNumber(whatsappNumber)
                .upiId(upiId).shopBio(shopBio).themeId(themeId)
                .profileImage(profileImage)
                .build();

        return ResponseEntity.ok(
            ApiResponse.ok("Profile updated!", sellerService.updateProfile(seller.getId(), req)));
    }
}
