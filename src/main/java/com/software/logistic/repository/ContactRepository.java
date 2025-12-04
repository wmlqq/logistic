package com.software.logistic.repository;

import com.software.logistic.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    // 可以根据需要添加自定义查询方法
}