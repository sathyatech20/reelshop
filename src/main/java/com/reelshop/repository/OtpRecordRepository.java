package com.reelshop.repository;

import com.reelshop.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {

    // Rate limit check: how many OTPs sent to this phone in last N minutes?
    @Query("""
        SELECT COUNT(o) FROM OtpRecord o
        WHERE o.phoneNumber = :phone
          AND o.createdAt >= :since
    """)
    long countRecentByPhone(String phone, LocalDateTime since);
}
