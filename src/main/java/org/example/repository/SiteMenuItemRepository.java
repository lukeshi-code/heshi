package org.example.repository;

import org.example.model.SiteMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteMenuItemRepository extends JpaRepository<SiteMenuItem, Long> {
    List<SiteMenuItem> findAllByOrderBySortOrderAscIdAsc();
}
