package com.reelshop.service;

import com.reelshop.dto.response.AuthResponse;
import com.reelshop.entity.Seller;
import com.reelshop.exception.ReelShopException;
import com.reelshop.repository.SellerRepository;
import com.reelshop.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService       otpService;
    private final SellerRepository sellerRepository;
    private final JwtUtil          jwtUtil;

    // ── Send OTP ──────────────────────────────────────────

    public AuthResponse.SendOtp sendOtp(String phoneNumber, String ipAddress) {
        otpService.sendOtp(phoneNumber, ipAddress);
        return AuthResponse.SendOtp.builder()
                .message("OTP sent to +91" + phoneNumber)
                .expiresInSeconds(300)
                .build();
    }

    // ── Verify OTP + Login / Register ─────────────────────

    @Transactional
    public AuthResponse.VerifyOtp verifyOtp(String phoneNumber, String otp) {
        boolean valid = otpService.verifyOtp(phoneNumber, otp);
        if (!valid) {
            throw ReelShopException.invalidOtp();
        }

        boolean isNewUser = !sellerRepository.existsByPhoneNumber(phoneNumber);

        Seller seller = sellerRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createNewSeller(phoneNumber));

        // Mark phone as verified
        seller.setIsPhoneVerified(true);
        sellerRepository.save(seller);

        String token = jwtUtil.generateToken(seller.getId(), seller.getPhoneNumber());

        return AuthResponse.VerifyOtp.builder()
                .token(token)
                .isNewUser(isNewUser)
                .isSetupComplete(seller.getIsSetupComplete())
                .seller(toSellerSummary(seller))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────

    private Seller createNewSeller(String phoneNumber) {
        Seller seller = Seller.builder()
                .phoneNumber(phoneNumber)
                .whatsappNumber(phoneNumber)  // default same as login number
                .build();
        return sellerRepository.save(seller);
    }

    private AuthResponse.SellerSummary toSellerSummary(Seller s) {
        return AuthResponse.SellerSummary.builder()
                .id(s.getId())
                .phoneNumber(s.getPhoneNumber())
                .shopName(s.getShopName())
                .shopSlug(s.getShopSlug())
                .profileImageUrl(s.getProfileImageUrl())
                .themeId(s.getThemeId())
                .build();
    }
}
