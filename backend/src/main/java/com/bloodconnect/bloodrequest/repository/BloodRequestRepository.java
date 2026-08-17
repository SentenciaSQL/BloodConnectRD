package com.bloodconnect.bloodrequest.repository;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.common.enums.RequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long>, JpaSpecificationExecutor<BloodRequest> {

    long countByStatus(RequestStatus status);

    @EntityGraph(attributePaths = {"province", "municipality"})
    List<BloodRequest> findByStatusIn(Collection<RequestStatus> statuses, Pageable pageable);
}
