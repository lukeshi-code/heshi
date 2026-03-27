package org.example.repository;

import org.example.model.SiteOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteOperationLogRepository extends JpaRepository<SiteOperationLog, Long> {
    List<SiteOperationLog> findTop20ByOrderByCreatedAtDesc();
}

