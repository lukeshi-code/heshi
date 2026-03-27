package org.example.repository;

import org.example.model.SitePageConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SitePageConfigRepository extends JpaRepository<SitePageConfig, Long> {
    Optional<SitePageConfig> findByRoutePath(String routePath);
    List<SitePageConfig> findAllByEnabledTrueAndNavVisibleTrueOrderByNavOrderAscIdAsc();
}
