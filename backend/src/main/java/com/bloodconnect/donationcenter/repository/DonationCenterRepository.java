package com.bloodconnect.donationcenter.repository;

import com.bloodconnect.donationcenter.entity.DonationCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DonationCenterRepository
        extends JpaRepository<DonationCenter, Long>, JpaSpecificationExecutor<DonationCenter> {
}
