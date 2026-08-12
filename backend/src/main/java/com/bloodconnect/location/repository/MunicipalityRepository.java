package com.bloodconnect.location.repository;

import com.bloodconnect.location.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {

    List<Municipality> findByProvinceIdOrderByNameAsc(Long provinceId);
}
