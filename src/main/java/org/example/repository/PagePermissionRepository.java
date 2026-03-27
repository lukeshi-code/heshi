package org.example.repository;

import org.example.model.PagePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagePermissionRepository extends JpaRepository<PagePermission, Long> {
    Optional<PagePermission> findByPathPattern(String pathPattern);
}
