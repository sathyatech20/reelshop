package com.reelshop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReelShopException extends RuntimeException {

    private final String    errorCode;
    private final HttpStatus httpStatus;

    public ReelShopException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    // ── Factory methods ───────────────────────────────────

    public static ReelShopException invalidOtp() {
        return new ReelShopException(
            "Invalid OTP. Please check and try again.",
            "OTP_INVALID",
            HttpStatus.UNAUTHORIZED
        );
    }

    public static ReelShopException otpExpired() {
        return new ReelShopException(
            "OTP has expired. Please request a new one.",
            "OTP_EXPIRED",
            HttpStatus.GONE
        );
    }

    public static ReelShopException otpRateLimited() {
        return new ReelShopException(
            "Too many OTP requests. Please wait before trying again.",
            "OTP_RATE_LIMITED",
            HttpStatus.TOO_MANY_REQUESTS
        );
    }

    public static ReelShopException notFound(String resource) {
        return new ReelShopException(
            resource + " not found.",
            "NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }

    public static ReelShopException slugTaken(String slug) {
        return new ReelShopException(
            "Shop URL '" + slug + "' is already taken. Try a different name.",
            "SLUG_TAKEN",
            HttpStatus.CONFLICT
        );
    }

    public static ReelShopException forbidden() {
        return new ReelShopException(
            "You don't have permission to do this.",
            "FORBIDDEN",
            HttpStatus.FORBIDDEN
        );
    }
}
