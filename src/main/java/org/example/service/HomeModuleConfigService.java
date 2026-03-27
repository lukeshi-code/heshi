package org.example.service;

import org.example.model.HomeModuleConfig;
import org.example.repository.HomeModuleConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HomeModuleConfigService {
    private final HomeModuleConfigRepository repository;

    private static final List<Seed> DEFAULT_MODULES = Collections.unmodifiableList(Arrays.asList(
        new Seed("hero_metrics", "核心能力", "支持权限管理、页面可视化、内容发布与运行监控。", "查看方案", "/solutions", true, 1),
        new Seed("member_center", "成员中心", "登录后可查看签到、发布内容与评论互动。", "去登录", "/login", true, 2),
        new Seed("news_stream", "最新动态", "展示最新文章与业务更新，支持快速跳转。", "查看新闻", "/news", true, 3)
    ));

    public HomeModuleConfigService(HomeModuleConfigRepository repository) {
        this.repository = repository;
    }

    public void ensureDefaults() {
        for (Seed seed : DEFAULT_MODULES) {
            Optional<HomeModuleConfig> existing = repository.findByPagePathAndModuleKey("/", seed.moduleKey);
            if (!existing.isPresent()) {
                HomeModuleConfig row = new HomeModuleConfig();
                row.setPagePath("/");
                row.setModuleKey(seed.moduleKey);
                row.setTitle(seed.title);
                row.setContent(seed.content);
                row.setButtonText(seed.buttonText);
                row.setButtonLink(seed.buttonLink);
                row.setEnabled(seed.enabled);
                row.setSortOrder(seed.sortOrder);
                repository.save(row);
                continue;
            }
            HomeModuleConfig row = existing.get();
            boolean changed = false;
            if (looksMojibake(row.getTitle())) {
                row.setTitle(seed.title);
                changed = true;
            }
            if (looksMojibake(row.getContent())) {
                row.setContent(seed.content);
                changed = true;
            }
            if (looksMojibake(row.getButtonText())) {
                row.setButtonText(seed.buttonText);
                changed = true;
            }
            if (changed) {
                repository.save(row);
            }
        }
    }

    public List<HomeModuleConfig> findHomeModules() {
        return repository.findAllByPagePathOrderBySortOrderAscIdAsc("/");
    }

    public HomeModuleConfig findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Module not found"));
    }

    public void update(Long id, String title, String content, String buttonText, String buttonLink, boolean enabled, Integer sortOrder) {
        HomeModuleConfig row = findById(id);
        row.setTitle(trimOrDefault(title, row.getTitle()));
        row.setContent(trimOrDefault(content, row.getContent()));
        row.setButtonText(trimOrDefault(buttonText, row.getButtonText()));
        row.setButtonLink(trimOrDefault(buttonLink, row.getButtonLink()));
        row.setEnabled(enabled);
        row.setSortOrder(sortOrder == null ? row.getSortOrder() : sortOrder);
        repository.save(row);
    }

    public void restoreByModuleKey(String pagePath, String moduleKey, String title, String content,
                                   String buttonText, String buttonLink, boolean enabled, int sortOrder) {
        HomeModuleConfig row = repository.findByPagePathAndModuleKey(pagePath, moduleKey).orElseGet(() -> {
            HomeModuleConfig n = new HomeModuleConfig();
            n.setPagePath(pagePath);
            n.setModuleKey(moduleKey);
            return n;
        });
        row.setTitle(title);
        row.setContent(content);
        row.setButtonText(buttonText);
        row.setButtonLink(buttonLink);
        row.setEnabled(enabled);
        row.setSortOrder(sortOrder);
        repository.save(row);
    }

    private boolean looksMojibake(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return value.contains("锛")
            || value.contains("銆")
            || value.contains("鈥")
            || value.contains("�");
    }

    private String trimOrDefault(String value, String def) {
        if (value == null) return def;
        String v = value.trim();
        return v.isEmpty() ? def : v;
    }

    private static class Seed {
        private final String moduleKey;
        private final String title;
        private final String content;
        private final String buttonText;
        private final String buttonLink;
        private final boolean enabled;
        private final int sortOrder;

        private Seed(String moduleKey, String title, String content, String buttonText, String buttonLink, boolean enabled, int sortOrder) {
            this.moduleKey = moduleKey;
            this.title = title;
            this.content = content;
            this.buttonText = buttonText;
            this.buttonLink = buttonLink;
            this.enabled = enabled;
            this.sortOrder = sortOrder;
        }
    }
}
