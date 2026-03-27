package org.example.repository;

import org.example.model.HomeModuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeModuleConfigRepository extends JpaRepository<HomeModuleConfig, Long> {
    List<HomeModuleConfig> findAllByPagePathOrderBySortOrderAscIdAsc(String pagePath);
    Optional<HomeModuleConfig> findByPagePathAndModuleKey(String pagePath, String moduleKey);
    List<HomeModuleConfig> findAllByTemplateNameIsNotNullOrderByTemplateNameAscIdAsc();
}
