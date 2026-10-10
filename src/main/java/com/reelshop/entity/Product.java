package com.reelshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_seller",   columnList = "seller_id"),
    @Index(name = "idx_product_code",     columnList = "short_code", unique = true),
    @Index(name = "idx_product_status",   columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    // ── Core Info ─────────────────────────────────────────
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // ── Short URL ─────────────────────────────────────────
    /**
     * 6-char alphanumeric code for short URL: reelshop.in/p/{shortCode}
     * Generated on product creation, immutable.
     */
    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    // ── Images (stored as JSON array of Cloudinary URLs) ──
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // ── Stock ─────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false, length = 20)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.AVAILABLE;

    // ── Status ────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    // ── QR Stats ──────────────────────────────────────────
    @Column(name = "qr_scan_count", nullable = false)
    @Builder.Default
    private Long qrScanCount = 0L;

    @Column(name = "whatsapp_click_count", nullable = false)
    @Builder.Default
    private Long whatsappClickCount = 0L;

    @Column(name = "upi_click_count", nullable = false)
    @Builder.Default
    private Long upiClickCount = 0L;

    // ── QR Image (cached Cloudinary URL) ─────────────────
    @Column(name = "qr_image_url")
    private String qrImageUrl;

    @Column(name = "qr_image_public_id")
    private String qrImagePublicId;

    // ── Relation ──────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    // ── Enums ─────────────────────────────────────────────
    public enum StockStatus {
        AVAILABLE, OUT_OF_STOCK
    }

    public enum ProductStatus {
        ACTIVE, DELETED
    }

    // ── Embeddable: Image ──────────────────────────────────
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImage {

        @Column(name = "image_url", nullable = false)
        private String imageUrl;

        @Column(name = "public_id")           // Cloudinary public_id for deletion
        private String publicId;

        @Column(name = "display_order")
        private Integer displayOrder;
    }
}
