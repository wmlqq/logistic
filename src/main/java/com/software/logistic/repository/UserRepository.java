package com.software.logistic.repository;

import com.software.logistic.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    
    /**
     * 根据角色查询用户列表
     */
    List<User> findByRole(String role);
    
    /**
     * 根据角色分页查询用户列表
     */
    Page<User> findByRole(String role, Pageable pageable);
    
    /**
     * 根据状态分页查询用户列表
     */
    Page<User> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 根据角色和状态分页查询用户列表
     */
    Page<User> findByRoleAndStatus(String role, Integer status, Pageable pageable);
    
    /**
     * 根据用户名、邮箱或手机号模糊查询用户列表
     */
    @Query("select u from User u where u.username like %:username% or u.email like %:email% or u.phone like %:phone%")
    Page<User> findByUsernameContainingOrEmailContainingOrPhoneContaining(@Param("username") String username, @Param("email") String email, @Param("phone") String phone, Pageable pageable);
    
    /**
     * 根据状态统计用户数量
     */
    long countByStatus(Integer status);
}