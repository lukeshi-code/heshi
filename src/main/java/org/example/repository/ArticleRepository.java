package org.example.repository;

import org.example.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    java.util.List<Article> findAllByOrderByCreatedAtDesc();
}
