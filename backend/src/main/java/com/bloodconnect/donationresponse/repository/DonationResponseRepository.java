package com.bloodconnect.donationresponse.repository;

import com.bloodconnect.common.enums.ResponseStatus;
import com.bloodconnect.donationresponse.entity.DonationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DonationResponseRepository extends JpaRepository<DonationResponse, Long> {

    List<DonationResponse> findByBloodRequestIdOrderByCreatedAtDesc(Long bloodRequestId);

    @Query("""
            select r from DonationResponse r
            join fetch r.bloodRequest br
            join fetch br.municipality
            join fetch br.province
            join fetch r.donor d
            join fetch d.user
            where d.id = :donorId
            order by r.createdAt desc
            """)
    List<DonationResponse> findByDonorIdOrderByCreatedAtDesc(@Param("donorId") Long donorId);

    boolean existsByBloodRequestIdAndDonorIdAndStatusIn(
            Long bloodRequestId,
            Long donorId,
            Collection<ResponseStatus> statuses
    );
}
