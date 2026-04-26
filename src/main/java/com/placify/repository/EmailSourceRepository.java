package com.placify.repository;

import com.placify.model.EmailSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSourceRepository extends JpaRepository<EmailSource, Long> {
    List<EmailSource> findByIsActiveTrue();
    Optional<EmailSource> findByName(String name);
    List<EmailSource> findAllByOrderByNameAsc();
}
