package com.bloodconnect.donationresponse.repository;

import com.bloodconnect.common.enums.ResponseStatus;
import com.bloodconnect.donationresponse.entity.DonationResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DonationResponseRepository extends JpaRepository<DonationResponse, Long> {

    List<DonationResponse> findByBloodRequestIdOrderByCreatedAtDesc(Long bloodRequestId);

    List<DonationResponse> findByBloodRequestIdAndDonorIdOrderByCreatedAtDesc(
            Long bloodRequestId,
            Long donorId
    );

    List<DonationResponse> findByBloodRequestIdAndDonorIdAndStatusIn(
            Long bloodRequestId,
            Long donorId,
            Collection<ResponseStatus> statuses
    );

    boolean existsByBloodRequestIdAndDonorIdAndStatusIn(
            Long bloodRequestId,
            Long donorId,
            Collection<ResponseStatus> statuses
    );
}
