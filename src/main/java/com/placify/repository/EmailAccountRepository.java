package com.placify.repository;

import com.placify.model.EmailAccount;
import com.placify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long> {
    
    List<EmailAccount> findByUser(User user);
    
    List<EmailAccount> findByUserAndIsActive(User user, Boolean isActive);
    
    Optional<EmailAccount> findByUserAndEmailAddress(User user, String emailAddress);
    
    List<EmailAccount> findByIsActive(Boolean isActive);
}
