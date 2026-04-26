package com.placify.repository;

import com.placify.model.AtsConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AtsConfigurationRepository extends JpaRepository<AtsConfiguration, Long> {
    Optional<AtsConfiguration> findByConfigKey(String configKey);
    List<AtsConfiguration> findByIsActiveTrue();
}
