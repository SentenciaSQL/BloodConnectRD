package com.bloodconnect.donation.repository;

import com.bloodconnect.common.enums.DonationStatus;
import com.bloodconnect.donation.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {

    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    Page<Donation> findByDonorId(Long donorId, Pageable pageable);

    @Query("""
            select coalesce(sum(d.units), 0)
            from Donation d
            where d.bloodRequest.id = :requestId and d.status = :status
            """)
    long sumUnitsByRequestAndStatus(
            @Param("requestId") Long requestId,
            @Param("status") DonationStatus status
    );
}
