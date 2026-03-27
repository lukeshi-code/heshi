package org.example.service;

import org.example.model.DataSourceType;
import org.example.model.HomeModuleConfig;
import org.example.model.ModuleType;
import org.example.repository.HomeModuleConfigRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HomeModuleConfigService {
    private final HomeModuleConfigRepository repository;

    private static final List<Seed> DEFAULT_MODULES = Collections.unmodifiableList(Arrays.asList(
        new Seed("/", "hero_banner", "顶部Banner", ModuleType.BANNER, "支持轮播和主视觉配置", "了解更多", "/about", true, 1),
        new Seed("/", "brand_intro", "品牌介绍", ModuleType.IMAGE_TEXT, "图文展示品牌价值和服务能力", "查看方案", "/solutions", true, 2),
        new Seed("/", "news_list", "新闻资讯", ModuleType.ARTICLE_LIST, "自动展示最新文章列表", "查看新闻", "/news", true, 3)
    ));

    public HomeModuleConfigService(HomeModuleConfigRepository repository) {
        this.repository = repository;
    }

    public void ensureDefaults() {
        for (Seed seed : DEFAULT_MODULES) {
            Optional<HomeModuleConfig> existing = repository.findByPagePathAndModuleKey(seed.pagePath, seed.moduleKey);
            if (!existing.isPresent()) {
                HomeModuleConfig row = new HomeModuleConfig();
                row.setPagePath(seed.pagePath);
                row.setModuleKey(seed.moduleKey);
                row.setTitle(seed.title);
                row.setModuleType(seed.moduleType);
                row.setContent(seed.content);
                row.setButtonText(seed.buttonText);
                row.setButtonLink(seed.buttonLink);
                row.setDataSourceType(DataSourceType.MANUAL);
                row.setBackgroundColor("#0f172a");
                row.setFontColor("#f8fafc");
                row.setFontSize("16px");
                row.setTextAlign("left");
                row.setEnabled(seed.enabled);
                row.setSortOrder(seed.sortOrder);
                repository.save(row);
            }
        }
    }

    public List<HomeModuleConfig> findHomeModules() {
        return repository.findAllByPagePathOrderBySortOrderAscIdAsc("/");
    }

    public List<HomeModuleConfig> findByPagePath(String pagePath) {
        return repository.findAllByPagePathOrderBySortOrderAscIdAsc(pagePath);
    }

    public List<HomeModuleConfig> findAllModules() {
        List<HomeModuleConfig> list = repository.findAll();
        list.sort((a, b) -> {
            int p = safe(a.getPagePath()).compareTo(safe(b.getPagePath()));
            if (p != 0) return p;
            int s = Integer.compare(a.getSortOrder(), b.getSortOrder());
            if (s != 0) return s;
            return Long.compare(a.getId(), b.getId());
        });
        return list;
    }

    public HomeModuleConfig findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Module not found"));
    }

    public HomeModuleConfig create(String pagePath, String title, ModuleType moduleType) {
        List<HomeModuleConfig> existing = findByPagePath(pagePath);
        HomeModuleConfig row = new HomeModuleConfig();
        row.setPagePath(pagePath);
        row.setModuleKey("module_" + System.currentTimeMillis());
        row.setTitle(trimOrDefault(title, "新模块"));
        row.setModuleType(moduleType == null ? ModuleType.IMAGE_TEXT : moduleType);
        row.setContent("");
        row.setButtonText("");
        row.setButtonLink("");
        row.setDataSourceType(DataSourceType.MANUAL);
        row.setBackgroundColor("#ffffff");
        row.setFontColor("#0f172a");
        row.setFontSize("16px");
        row.setTextAlign("left");
        row.setEnabled(true);
        row.setSortOrder(existing.size() + 1);
        return repository.save(row);
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

    public void updateAdvanced(Long id,
                               String title,
                               ModuleType moduleType,
                               String content,
                               String buttonText,
                               String buttonLink,
                               DataSourceType dataSourceType,
                               String backgroundColor,
                               String backgroundImageUrl,
                               String fontColor,
                               String fontSize,
                               String textAlign,
                               boolean enabled,
                               Integer sortOrder) {
        HomeModuleConfig row = findById(id);
        row.setTitle(trimOrDefault(title, row.getTitle()));
        row.setModuleType(moduleType == null ? row.getModuleType() : moduleType);
        row.setContent(trimOrDefault(content, row.getContent()));
        row.setButtonText(trimOrDefault(buttonText, row.getButtonText()));
        row.setButtonLink(trimOrDefault(buttonLink, row.getButtonLink()));
        row.setDataSourceType(dataSourceType == null ? DataSourceType.MANUAL : dataSourceType);
        row.setBackgroundColor(trimOrDefault(backgroundColor, row.getBackgroundColor()));
        row.setBackgroundImageUrl(trimOrNull(backgroundImageUrl));
        row.setFontColor(trimOrDefault(fontColor, row.getFontColor()));
        row.setFontSize(trimOrDefault(fontSize, row.getFontSize()));
        row.setTextAlign(trimOrDefault(textAlign, row.getTextAlign()));
        row.setEnabled(enabled);
        row.setSortOrder(sortOrder == null ? row.getSortOrder() : sortOrder);
        repository.save(row);
    }

    public void saveAsTemplate(Long id, String templateName) {
        HomeModuleConfig row = findById(id);
        row.setTemplateName(trimOrDefault(templateName, row.getTitle() + "_模板"));
        repository.save(row);
    }

    public List<HomeModuleConfig> findTemplates() {
        List<HomeModuleConfig> rows = repository.findAllByTemplateNameIsNotNullOrderByTemplateNameAscIdAsc();
        List<HomeModuleConfig> out = new ArrayList<HomeModuleConfig>();
        for (HomeModuleConfig row : rows) {
            if (row.getTemplateName() != null && !row.getTemplateName().trim().isEmpty()) {
                out.add(row);
            }
        }
        return out;
    }

    public void applyTemplate(Long targetId, Long sourceTemplateId) {
        HomeModuleConfig target = findById(targetId);
        HomeModuleConfig source = findById(sourceTemplateId);
        target.setModuleType(source.getModuleType());
        target.setContent(source.getContent());
        target.setButtonText(source.getButtonText());
        target.setButtonLink(source.getButtonLink());
        target.setDataSourceType(source.getDataSourceType());
        target.setBackgroundColor(source.getBackgroundColor());
        target.setBackgroundImageUrl(source.getBackgroundImageUrl());
        target.setFontColor(source.getFontColor());
        target.setFontSize(source.getFontSize());
        target.setTextAlign(source.getTextAlign());
        repository.save(target);
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
        if (row.getModuleType() == null) row.setModuleType(ModuleType.IMAGE_TEXT);
        if (row.getDataSourceType() == null) row.setDataSourceType(DataSourceType.MANUAL);
        repository.save(row);
    }

    public List<ModuleType> allModuleTypes() {
        return Arrays.asList(ModuleType.values());
    }

    public List<DataSourceType> allDataSourceTypes() {
        return Arrays.asList(DataSourceType.values());
    }

    private String trimOrDefault(String value, String def) {
        if (value == null) return def;
        String v = value.trim();
        return v.isEmpty() ? def : v;
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class Seed {
        private final String pagePath;
        private final String moduleKey;
        private final String title;
        private final ModuleType moduleType;
        private final String content;
        private final String buttonText;
        private final String buttonLink;
        private final boolean enabled;
        private final int sortOrder;

        private Seed(String pagePath, String moduleKey, String title, ModuleType moduleType, String content, String buttonText, String buttonLink, boolean enabled, int sortOrder) {
            this.pagePath = pagePath;
            this.moduleKey = moduleKey;
            this.title = title;
            this.moduleType = moduleType;
            this.content = content;
            this.buttonText = buttonText;
            this.buttonLink = buttonLink;
            this.enabled = enabled;
            this.sortOrder = sortOrder;
        }
    }
}
