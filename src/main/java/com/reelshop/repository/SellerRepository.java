package com.reelshop.repository;

import com.reelshop.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByPhoneNumber(String phoneNumber);

    Optional<Seller> findByShopSlug(String shopSlug);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByShopSlug(String shopSlug);

    // Increment scan count atomically — avoids optimistic lock issues
    @Modifying
    @Query("UPDATE Seller s SET s.totalQrScans = s.totalQrScans + 1 WHERE s.id = :id")
    void incrementQrScans(Long id);

    @Modifying
    @Query("UPDATE Seller s SET s.totalWhatsappClicks = s.totalWhatsappClicks + 1 WHERE s.id = :id")
    void incrementWhatsappClicks(Long id);
}
