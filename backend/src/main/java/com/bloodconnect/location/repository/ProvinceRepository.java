package com.bloodconnect.location.repository;

import com.bloodconnect.location.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvinceRepository extends JpaRepository<Province, Long> {

    List<Province> findAllByOrderByNameAsc();
}
