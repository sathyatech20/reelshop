package com.reelshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sellers", indexes = {
    @Index(name = "idx_seller_phone", columnList = "phone_number", unique = true),
    @Index(name = "idx_seller_slug",  columnList = "shop_slug",    unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller extends BaseEntity {

    // ── Auth ──────────────────────────────────────────────
    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "is_phone_verified", nullable = false)
    @Builder.Default
    private Boolean isPhoneVerified = false;

    // ── Shop Identity ─────────────────────────────────────
    @Column(name = "shop_name", length = 100)
    private String shopName;

    /**
     * URL-safe slug: reelshop.in/s/{shopSlug}
     * Auto-generated from shopName; unique across platform.
     */
    @Column(name = "shop_slug", unique = true, length = 100)
    private String shopSlug;

    @Column(name = "shop_bio", length = 250)
    private String shopBio;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "profile_image_public_id")   // Cloudinary public_id for deletion
    private String profileImagePublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private ShopCategory category;

    // ── Contact ───────────────────────────────────────────
    @Column(name = "whatsapp_number", length = 15)
    private String whatsappNumber;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    // ── Theme ─────────────────────────────────────────────
    @Column(name = "theme_id", length = 30)
    @Builder.Default
    private String themeId = "midnight";   // matches frontend theme keys

    // ── Status ────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SellerStatus status = SellerStatus.ACTIVE;

    @Column(name = "is_setup_complete", nullable = false)
    @Builder.Default
    private Boolean isSetupComplete = false;

    // ── Stats (denormalised for fast reads) ───────────────
    @Column(name = "total_qr_scans", nullable = false)
    @Builder.Default
    private Long totalQrScans = 0L;

    @Column(name = "total_whatsapp_clicks", nullable = false)
    @Builder.Default
    private Long totalWhatsappClicks = 0L;

    // ── Relations ─────────────────────────────────────────
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    // ── Enums ─────────────────────────────────────────────
    public enum ShopCategory {
        FASHION, JEWELRY, FOOD_SNACKS, HOME_DECOR, BEAUTY, OTHER
    }

    public enum SellerStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
