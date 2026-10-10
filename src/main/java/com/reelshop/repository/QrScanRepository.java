package com.reelshop.repository;

import com.reelshop.entity.QrScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QrScanRepository extends JpaRepository<QrScan, Long> {

    long countByProductId(Long productId);

    long countBySellerId(Long sellerId);

    // Last 30 days scans per product — for QR history screen
    @Query("""
        SELECT q FROM QrScan q
        WHERE q.seller.id = :sellerId
          AND q.scannedAt >= :since
        ORDER BY q.scannedAt DESC
    """)
    List<QrScan> findRecentBySeller(Long sellerId, LocalDateTime since);
}
