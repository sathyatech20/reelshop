package com.reelshop.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_BUYER  = "BUYER";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /** Seller token. Kept so existing callers (AuthService) keep working unchanged. */
    public String generateToken(Long sellerId, String phoneNumber) {
        return generateToken(sellerId, phoneNumber, ROLE_SELLER);
    }

    /** Role-aware token: role is ROLE_SELLER or ROLE_BUYER. */
    public String generateToken(Long userId, String phoneNumber, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phoneNumber)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }

    /** Old name kept for backward compatibility. */
    public Long extractSellerId(String token) {
        return extractUserId(token);
    }

    /**
     * Tokens issued before roles existed have no "role" claim.
     * Treat them as SELLER so sellers who are already logged in are not kicked out.
     */
    public String extractRole(String token) {
        String role = parse(token).get("role", String.class);
        return role != null ? role : ROLE_SELLER;
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
