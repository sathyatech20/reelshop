package com.reelshop.dto.response;

import lombok.Builder;
import lombok.Data;

public class AuthResponse {

    @Data
    @Builder
    public static class SendOtp {
        private String message;
        private int expiresInSeconds;
    }

    @Data
    @Builder
    public static class VerifyOtp {
        private String token;           // JWT
        private boolean isNewUser;      // frontend decides: go to setup or dashboard
        private boolean isSetupComplete;
        private SellerSummary seller;
    }

    @Data
    @Builder
    public static class SellerSummary {
        private Long id;
        private String phoneNumber;
        private String shopName;
        private String shopSlug;
        private String profileImageUrl;
        private String themeId;
    }
}
