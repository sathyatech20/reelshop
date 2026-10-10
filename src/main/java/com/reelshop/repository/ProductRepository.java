package com.reelshop.repository;

import com.reelshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Seller's product list — active only, latest first
    List<Product> findBySellerIdAndStatusOrderByCreatedAtDesc(
            Long sellerId, Product.ProductStatus status);

    // Public shop page — available products only
    Page<Product> findBySellerIdAndStatusAndStockStatus(
            Long sellerId,
            Product.ProductStatus status,
            Product.StockStatus stockStatus,
            Pageable pageable);

    // QR / short-link lookup
    Optional<Product> findByShortCode(String shortCode);

    // Count products per seller
    long countBySellerIdAndStatus(Long sellerId, Product.ProductStatus status);

    // Atomic stat increments
    @Modifying
    @Query("UPDATE Product p SET p.qrScanCount = p.qrScanCount + 1 WHERE p.id = :id")
    void incrementQrScanCount(Long id);

    @Modifying
    @Query("UPDATE Product p SET p.whatsappClickCount = p.whatsappClickCount + 1 WHERE p.id = :id")
    void incrementWhatsappClickCount(Long id);

    @Modifying
    @Query("UPDATE Product p SET p.upiClickCount = p.upiClickCount + 1 WHERE p.id = :id")
    void incrementUpiClickCount(Long id);
}
