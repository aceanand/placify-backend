package com.placify.repository;

import com.placify.model.SalaryBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalaryBreakdownRepository extends JpaRepository<SalaryBreakdown, Long> {
    List<SalaryBreakdown> findByUserId(Long userId);
}
