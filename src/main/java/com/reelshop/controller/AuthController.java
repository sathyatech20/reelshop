package com.reelshop.controller;

import com.reelshop.dto.request.AuthRequest;
import com.reelshop.dto.response.ApiResponse;
import com.reelshop.dto.response.AuthResponse;
import com.reelshop.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/send-otp
     * Body: { "phoneNumber": "9876543210" }
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<AuthResponse.SendOtp>> sendOtp(
            @Valid @RequestBody AuthRequest.SendOtp request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        AuthResponse.SendOtp response = authService.sendOtp(request.getPhoneNumber(), ip);
        return ResponseEntity.ok(ApiResponse.ok("OTP sent successfully", response));
    }

    /**
     * POST /api/v1/auth/verify-otp
     * Body: { "phoneNumber": "9876543210", "otp": "347291" }
     *
     * Returns JWT + isNewUser flag so frontend knows
     * whether to go to setup screen or dashboard.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse.VerifyOtp>> verifyOtp(
            @Valid @RequestBody AuthRequest.VerifyOtp request) {

        AuthResponse.VerifyOtp response = authService.verifyOtp(
                request.getPhoneNumber(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    // ── Util ──────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
