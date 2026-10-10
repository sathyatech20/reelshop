package com.reelshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Immutable scan event log.
 * One row per QR scan / short-link visit.
 * Never updated — only inserted.
 */
@Entity
@Table(name = "qr_scans", indexes = {
    @Index(name = "idx_scan_product",   columnList = "product_id"),
    @Index(name = "idx_scan_seller",    columnList = "seller_id"),
    @Index(name = "idx_scan_created",   columnList = "scanned_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class QrScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    // Source: where did the scan come from?
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_source", length = 20)
    @Builder.Default
    private ScanSource scanSource = ScanSource.QR_CODE;

    // Device info (best-effort, from User-Agent)
    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreatedDate
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private LocalDateTime scannedAt;

    public enum ScanSource {
        QR_CODE,       // scanned QR physically
        SHORT_LINK,    // tapped link in bio/caption
        DIRECT         // typed URL
    }
}
