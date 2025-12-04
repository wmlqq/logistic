package com.software.logistic.repository;

import com.software.logistic.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    // 根据位置代码查询
    Optional<Location> findByLocationCode(String locationCode);
}
