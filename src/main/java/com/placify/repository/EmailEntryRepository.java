package com.placify.repository;

import com.placify.model.EmailEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailEntryRepository extends JpaRepository<EmailEntry, Long> {
    List<EmailEntry> findByUserIdOrderByReceivedAtDesc(Long userId);
    
    Page<EmailEntry> findByUserIdOrderByReceivedAtDesc(Long userId, Pageable pageable);
    
    long countByUserId(Long userId);
}
