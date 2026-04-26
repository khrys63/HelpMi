package com.helpmi.repository;

import com.helpmi.domain.ConfigValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfigValueRepository extends JpaRepository<ConfigValue, UUID> {
    List<ConfigValue> findByCategoryOrderByPosition(String category);
    List<ConfigValue> findByCategoryAndActiveTrueOrderByPosition(String category);
    boolean existsByCategoryAndCode(String category, String code);
}
