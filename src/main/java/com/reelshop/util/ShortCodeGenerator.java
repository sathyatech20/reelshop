package com.reelshop.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    // Removed confusable chars: 0/O, 1/l/I
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Generate URL-safe shop slug from shop name.
     * "Priya's Boutique!" → "priyasboutique"
     */
    public String toSlug(String shopName) {
        return shopName.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .substring(0, Math.min(shopName.replaceAll("[^a-z0-9]", "").length(), 30));
    }
}
