package org.example.repository;

import org.example.model.ArticleComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {
    List<ArticleComment> findByArticleIdOrderByCreatedAtAsc(Long articleId);
}
