package com.placify.repository;

import com.placify.model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findByJobRole(String jobRole);
    List<InterviewQuestion> findByJobRoleAndRoundType(String jobRole, String roundType);
    List<InterviewQuestion> findByRoundType(String roundType);

    @Query("SELECT DISTINCT q.jobRole FROM InterviewQuestion q ORDER BY q.jobRole")
    List<String> findDistinctJobRoles();

    @Query("SELECT DISTINCT q.roundType FROM InterviewQuestion q ORDER BY q.roundType")
    List<String> findDistinctRoundTypes();
}
