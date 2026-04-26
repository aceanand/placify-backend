package com.placify.repository;

import com.placify.model.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRoleRepository extends JpaRepository<JobRole, Long> {
    List<JobRole> findByIsActiveTrue();
    Optional<JobRole> findByTitle(String title);
    boolean existsByTitle(String title);
}
