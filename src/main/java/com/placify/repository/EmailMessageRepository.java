package com.placify.repository;

import com.placify.model.EmailAccount;
import com.placify.model.EmailMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {
    
    Page<EmailMessage> findByAccount(EmailAccount account, Pageable pageable);
    
    Page<EmailMessage> findByAccountAndIsRead(EmailAccount account, Boolean isRead, Pageable pageable);
    
    Page<EmailMessage> findByAccountAndSenderEmailContainingIgnoreCase(EmailAccount account, String senderEmail, Pageable pageable);
    
    Page<EmailMessage> findByAccountAndIsReadAndSenderEmailContainingIgnoreCase(EmailAccount account, Boolean isRead, String senderEmail, Pageable pageable);
    
    Optional<EmailMessage> findByAccountAndMessageId(EmailAccount account, String messageId);
    
    Long countByAccountAndIsRead(EmailAccount account, Boolean isRead);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailMessage e WHERE e.account = :account")
    void deleteByAccount(@Param("account") EmailAccount account);
}
