package com.bloodconnect.donor.repository;

import com.bloodconnect.common.enums.AvailabilityStatus;
import com.bloodconnect.common.enums.BloodType;
import com.bloodconnect.donor.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long>, JpaSpecificationExecutor<Donor> {

    Optional<Donor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByAvailability(AvailabilityStatus availability);

    @Query("""
            select d from Donor d
            join fetch d.user
            join fetch d.municipality
            join fetch d.province
            where d.availability = :availability
              and d.bloodType in :bloodTypes
            """)
    List<Donor> findByAvailabilityAndBloodTypeIn(
            @Param("availability") AvailabilityStatus availability,
            @Param("bloodTypes") Collection<BloodType> bloodTypes
    );
}
