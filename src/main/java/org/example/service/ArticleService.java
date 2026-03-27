package org.example.service;

import org.example.model.Article;
import org.example.model.ArticleComment;
import org.example.model.UserAccount;
import org.example.repository.ArticleCommentRepository;
import org.example.repository.ArticleRepository;
import org.example.web.ArticleForm;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository articleCommentRepository;
    private final FileStorageService fileStorageService;

    public ArticleService(ArticleRepository articleRepository,
                          ArticleCommentRepository articleCommentRepository,
                          FileStorageService fileStorageService) {
        this.articleRepository = articleRepository;
        this.articleCommentRepository = articleCommentRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Article> findAll() {
        return articleRepository.findAllByOrderByCreatedAtDesc();
    }

    public Article findById(Long id) {
        return articleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Article not found"));
    }

    public Article create(ArticleForm form, MultipartFile file, UserAccount author) {
        Article article = new Article();
        article.setTitle(form.getTitle());
        article.setContent(form.getContent());
        article.setAuthor(author);
        if (file != null && !file.isEmpty()) {
            article.setAttachmentOriginalName(file.getOriginalFilename());
            article.setAttachmentPath(fileStorageService.store(file));
        }
        return articleRepository.save(article);
    }

    public List<ArticleComment> findComments(Long articleId) {
        return articleCommentRepository.findByArticleIdOrderByCreatedAtAsc(articleId);
    }

    public void addComment(Article article, UserAccount author, String content) {
        ArticleComment comment = new ArticleComment();
        comment.setArticle(article);
        comment.setAuthor(author);
        comment.setContent(content);
        articleCommentRepository.save(comment);
    }
}
