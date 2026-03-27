package org.example.repository;

import org.example.model.SitePageVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SitePageVersionRepository extends JpaRepository<SitePageVersion, Long> {
    Optional<SitePageVersion> findTopByRoutePathOrderByCreatedAtDesc(String routePath);
}

