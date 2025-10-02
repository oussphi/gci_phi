package com.project.repository;

import com.project.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    Optional<Admin> findByUsername(String username);
    
    Optional<Admin> findByEmail(String email);
    
    List<Admin> findByIsActive(boolean isActive);
    
    List<Admin> findByIsSuperAdmin(boolean isSuperAdmin);
    
    @Query("SELECT a FROM Admin a WHERE a.username LIKE %:searchTerm% OR a.fullName LIKE %:searchTerm% OR a.email LIKE %:searchTerm%")
    List<Admin> searchAdmins(@Param("searchTerm") String searchTerm);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
