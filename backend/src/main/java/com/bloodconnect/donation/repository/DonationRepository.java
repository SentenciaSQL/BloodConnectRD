package com.bloodconnect.donation.repository;

import com.bloodconnect.common.enums.DonationStatus;
import com.bloodconnect.donation.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {

    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    Page<Donation> findByDonorId(Long donorId, Pageable pageable);

    List<Donation> findByBloodRequestIdOrderByCreatedAtDesc(Long bloodRequestId);

    List<Donation> findByBloodRequestIdAndDonorIdOrderByCreatedAtDesc(Long bloodRequestId, Long donorId);

    boolean existsByBloodRequestIdAndDonorIdAndStatusIn(
            Long bloodRequestId,
            Long donorId,
            Collection<DonationStatus> statuses
    );

    @Query("""
            select coalesce(sum(d.confirmedUnits), 0)
            from Donation d
            where d.bloodRequest.id = :requestId
              and d.status <> :cancelled
            """)
    long sumConfirmedUnitsByRequest(
            @Param("requestId") Long requestId,
            @Param("cancelled") DonationStatus cancelled
    );

    default long sumConfirmedUnitsByRequest(Long requestId) {
        return sumConfirmedUnitsByRequest(requestId, DonationStatus.CANCELLED);
    }
}
