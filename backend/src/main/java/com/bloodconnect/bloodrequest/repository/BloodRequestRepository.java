package com.bloodconnect.bloodrequest.repository;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long>, JpaSpecificationExecutor<BloodRequest> {

    long countByStatus(RequestStatus status);
}
