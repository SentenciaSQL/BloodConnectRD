package com.bloodconnect.bloodrequest.repository;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.common.enums.RequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long>, JpaSpecificationExecutor<BloodRequest> {

    long countByStatus(RequestStatus status);

    @EntityGraph(attributePaths = {"province", "municipality"})
    List<BloodRequest> findByStatusIn(Collection<RequestStatus> statuses, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BloodRequest r set r.status = :expiredStatus, r.updatedAt = :now "
            + "where r.status in :statuses and r.deadline < :now")
    int expireOverdue(
            @Param("statuses") Collection<RequestStatus> statuses,
            @Param("expiredStatus") RequestStatus expiredStatus,
            @Param("now") Instant now
    );
}
