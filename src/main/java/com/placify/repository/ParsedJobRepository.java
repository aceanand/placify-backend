package com.placify.repository;

import com.placify.model.ParsedJob;
import com.placify.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParsedJobRepository extends JpaRepository<ParsedJob, Long> {
    Page<ParsedJob> findByUserOrderByReceivedAtDesc(User user, Pageable pageable);
    Page<ParsedJob> findByUserAndSourceOrderByReceivedAtDesc(User user, String source, Pageable pageable);
    boolean existsByUserAndEmailMessageId(User user, Long emailMessageId);
    long countByUser(User user);
    long countByUserAndSource(User user, String source);
    List<ParsedJob> findTop5ByUserOrderByReceivedAtDesc(User user);
}
