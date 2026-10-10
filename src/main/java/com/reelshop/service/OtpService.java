package com.reelshop.service;

import com.reelshop.entity.OtpRecord;
import com.reelshop.exception.ReelShopException;
import com.reelshop.repository.OtpRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_KEY_PREFIX      = "otp:";
    private static final String RATE_KEY_PREFIX     = "otp_rate:";
    private static final String ATTEMPT_KEY_PREFIX  = "otp_attempts:";
    private static final int    MAX_PER_HOUR        = 5;

    private final StringRedisTemplate   redis;
    private final OtpRecordRepository   otpRecordRepository;
    private final SmsService            smsService;
    private final SecureRandom          secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-seconds}")
    private int otpExpirySeconds;

    @Value("${app.otp.length}")
    private int otpLength;

    // Already in application.yml (app.otp.max-attempts: 3) but was never enforced
    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    // ── Send OTP ──────────────────────────────────────────

    public void sendOtp(String phoneNumber, String ipAddress) {
        checkRateLimit(phoneNumber);

        String otp = generateOtp();
        String redisKey = OTP_KEY_PREFIX + phoneNumber;

        // Store in Redis with TTL; a fresh OTP starts with a fresh attempt counter
        redis.opsForValue().set(redisKey, otp, otpExpirySeconds, TimeUnit.SECONDS);
        redis.delete(ATTEMPT_KEY_PREFIX + phoneNumber);

        // Log audit record (no OTP value stored in DB)
        otpRecordRepository.save(OtpRecord.builder()
                .phoneNumber(phoneNumber)
                .ipAddress(ipAddress)
                .status(OtpRecord.OtpStatus.SENT)
                .build());

        // Send SMS
        smsService.sendOtp(phoneNumber, otp);

        log.info("OTP sent to +91{}", phoneNumber);
    }

    // ── Verify OTP ────────────────────────────────────────

    public boolean verifyOtp(String phoneNumber, String submittedOtp) {
        String redisKey   = OTP_KEY_PREFIX + phoneNumber;
        String attemptKey = ATTEMPT_KEY_PREFIX + phoneNumber;
        String storedOtp  = redis.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw ReelShopException.otpExpired();
        }

        boolean matches = MessageDigest.isEqual(
                storedOtp.getBytes(StandardCharsets.UTF_8),
                submittedOtp.getBytes(StandardCharsets.UTF_8));

        if (!matches) {
            Long attempts = redis.opsForValue().increment(attemptKey);
            if (attempts != null && attempts == 1) {
                redis.expire(attemptKey, otpExpirySeconds, TimeUnit.SECONDS);
            }
            if (attempts != null && attempts >= maxAttempts) {
                // Too many wrong guesses: kill this OTP. The user must request a new one.
                redis.delete(redisKey);
                redis.delete(attemptKey);
                log.warn("OTP invalidated after {} wrong attempts for +91{}", attempts, phoneNumber);
                throw ReelShopException.otpExpired();
            }
            return false;
        }

        // Consume OTP — delete so it can't be reused
        redis.delete(redisKey);
        redis.delete(attemptKey);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────

    private void checkRateLimit(String phoneNumber) {
        String rateKey = RATE_KEY_PREFIX + phoneNumber;
        Long count = redis.opsForValue().increment(rateKey);

        if (count == 1) {
            // First request — set expiry window of 1 hour
            redis.expire(rateKey, 1, TimeUnit.HOURS);
        }

        if (count != null && count > MAX_PER_HOUR) {
            throw ReelShopException.otpRateLimited();
        }
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp   = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }
}
