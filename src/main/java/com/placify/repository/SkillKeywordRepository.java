package com.placify.repository;

import com.placify.model.SkillKeyword;
import com.placify.model.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillKeywordRepository extends JpaRepository<SkillKeyword, Long> {
    List<SkillKeyword> findByJobRoleAndIsActiveTrue(JobRole jobRole);
    List<SkillKeyword> findByJobRoleIdAndIsActiveTrue(Long jobRoleId);
    List<SkillKeyword> findByJobRoleAndCategoryAndIsActiveTrue(JobRole jobRole, String category);
    void deleteByJobRoleId(Long jobRoleId);
}
