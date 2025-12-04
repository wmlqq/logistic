package com.software.logistic.repository;

import com.software.logistic.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    /**
     * 根据用户ID查询地址列表
     */
    List<Address> findByUserId(Long userId);

    /**
     * 更新用户所有地址的默认状态
     */
    @Modifying
    @Query("update Address a set a.isDefault = ?1 where a.userId = ?2")
    void updateIsDefaultByUserId(Boolean isDefault, Long userId);
}