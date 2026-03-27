package org.example.service;

import org.example.model.LayoutMode;
import org.example.model.SitePageConfig;
import org.example.repository.SitePageConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class SitePageConfigService {
    private final SitePageConfigRepository repository;

    private static final List<Seed> DEFAULT_PAGES = Collections.unmodifiableList(Arrays.asList(
        new Seed("/", "首页", 1, true, true, LayoutMode.SIMPLE,
            "企业官网与后台管理，一套系统完成",
            "支持用户分级、页面权限配置、内容发布与运营管理。",
            "",
            "查看解决方案", "/solutions",
            "查看案例", "/cases"),
        new Seed("/about", "关于我们", 2, true, true, LayoutMode.SIMPLE,
            null, null, "", "了解更多", "/about", "联系我们", "/contact"),
        new Seed("/solutions", "解决方案", 3, true, true, LayoutMode.SIMPLE,
            null, null, "", "查看方案", "/solutions", "联系咨询", "/contact"),
        new Seed("/cases", "案例展示", 4, true, true, LayoutMode.SIMPLE,
            null, null, "", "查看案例", "/cases", "返回首页", "/"),
        new Seed("/news", "新闻动态", 5, true, true, LayoutMode.SIMPLE,
            null, null, "", "查看新闻", "/news", "发布内容", "/articles/new"),
        new Seed("/stock-sim", "模拟股票", 6, true, true, LayoutMode.CARD,
            null, null, "", "进入模块", "/stock-sim", "返回首页", "/"),
        new Seed("/contact", "联系我们", 7, true, true, LayoutMode.SIMPLE,
            null, null, "", "联系我们", "/contact", "返回首页", "/")
    ));

    public SitePageConfigService(SitePageConfigRepository repository) {
        this.repository = repository;
    }

    public void ensureDefaults() {
        for (Seed seed : DEFAULT_PAGES) {
            Optional<SitePageConfig> existing = repository.findByRoutePath(seed.routePath);
            if (!existing.isPresent()) {
                SitePageConfig row = new SitePageConfig();
                applySeed(row, seed);
                repository.save(row);
                continue;
            }
            SitePageConfig row = existing.get();
            boolean changed = repairKnownMojibake(row, seed);
            if (changed) {
                repository.save(row);
            }
        }
    }

    public List<SitePageConfig> findAll() {
        List<SitePageConfig> rows = repository.findAll();
        rows.sort((a, b) -> Integer.compare(a.getNavOrder(), b.getNavOrder()));
        return rows;
    }

    public List<SitePageConfig> navPages() {
        return repository.findAllByEnabledTrueAndNavVisibleTrueOrderByNavOrderAscIdAsc();
    }

    public SitePageConfig homeConfig() {
        return repository.findByRoutePath("/")
            .orElseThrow(() -> new IllegalStateException("Home page config not found"));
    }

    public SitePageConfig findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Page config not found"));
    }

    public SitePageConfig findByRoutePath(String path) {
        return repository.findByRoutePath(path).orElseThrow(() -> new IllegalArgumentException("Page config not found"));
    }

    public void save(SitePageConfig row) {
        repository.save(row);
    }

    public void update(Long id, String pageName, Integer navOrder, boolean navVisible,
                       boolean enabled, LayoutMode layoutMode, String heroTitle, String heroSubtitle,
                       String bannerImageUrl,
                       String heroPrimaryButtonText, String heroPrimaryButtonLink,
                       String heroSecondaryButtonText, String heroSecondaryButtonLink) {
        SitePageConfig row = findById(id);
        row.setPageName(trimOrDefault(pageName, row.getPageName()));
        row.setNavOrder(navOrder == null ? row.getNavOrder() : navOrder);
        row.setNavVisible(navVisible);
        row.setEnabled(enabled);
        row.setLayoutMode(layoutMode == null ? LayoutMode.SIMPLE : layoutMode);
        row.setHeroTitle(trimOrNull(heroTitle));
        row.setHeroSubtitle(trimOrNull(heroSubtitle));
        row.setBannerImageUrl(trimOrNull(bannerImageUrl));
        row.setHeroPrimaryButtonText(trimOrNull(heroPrimaryButtonText));
        row.setHeroPrimaryButtonLink(trimOrNull(heroPrimaryButtonLink));
        row.setHeroSecondaryButtonText(trimOrNull(heroSecondaryButtonText));
        row.setHeroSecondaryButtonLink(trimOrNull(heroSecondaryButtonLink));
        repository.save(row);
    }

    public List<LayoutMode> allLayoutModes() {
        return Arrays.asList(LayoutMode.values());
    }

    private void applySeed(SitePageConfig row, Seed seed) {
        row.setRoutePath(seed.routePath);
        row.setPageName(seed.pageName);
        row.setNavOrder(seed.navOrder);
        row.setNavVisible(seed.navVisible);
        row.setEnabled(seed.enabled);
        row.setLayoutMode(seed.layoutMode);
        row.setHeroTitle(seed.heroTitle);
        row.setHeroSubtitle(seed.heroSubtitle);
        row.setBannerImageUrl(seed.bannerImageUrl);
        row.setHeroPrimaryButtonText(seed.heroPrimaryButtonText);
        row.setHeroPrimaryButtonLink(seed.heroPrimaryButtonLink);
        row.setHeroSecondaryButtonText(seed.heroSecondaryButtonText);
        row.setHeroSecondaryButtonLink(seed.heroSecondaryButtonLink);
    }

    private boolean repairKnownMojibake(SitePageConfig row, Seed seed) {
        boolean changed = false;
        if (looksMojibake(row.getPageName())) {
            row.setPageName(seed.pageName);
            changed = true;
        }
        if (looksMojibake(row.getHeroTitle())) {
            row.setHeroTitle(seed.heroTitle);
            changed = true;
        }
        if (looksMojibake(row.getHeroSubtitle())) {
            row.setHeroSubtitle(seed.heroSubtitle);
            changed = true;
        }
        if (looksMojibake(row.getHeroPrimaryButtonText())) {
            row.setHeroPrimaryButtonText(seed.heroPrimaryButtonText);
            changed = true;
        }
        if (looksMojibake(row.getHeroSecondaryButtonText())) {
            row.setHeroSecondaryButtonText(seed.heroSecondaryButtonText);
            changed = true;
        }
        return changed;
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

    private String trimOrDefault(String v, String d) {
        if (v == null) return d;
        String t = v.trim();
        return t.isEmpty() ? d : t;
    }

    private String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static class Seed {
        private final String routePath;
        private final String pageName;
        private final int navOrder;
        private final boolean navVisible;
        private final boolean enabled;
        private final LayoutMode layoutMode;
        private final String heroTitle;
        private final String heroSubtitle;
        private final String bannerImageUrl;
        private final String heroPrimaryButtonText;
        private final String heroPrimaryButtonLink;
        private final String heroSecondaryButtonText;
        private final String heroSecondaryButtonLink;

        private Seed(String routePath, String pageName, int navOrder, boolean navVisible, boolean enabled,
                     LayoutMode layoutMode, String heroTitle, String heroSubtitle, String bannerImageUrl,
                     String heroPrimaryButtonText, String heroPrimaryButtonLink,
                     String heroSecondaryButtonText, String heroSecondaryButtonLink) {
            this.routePath = routePath;
            this.pageName = pageName;
            this.navOrder = navOrder;
            this.navVisible = navVisible;
            this.enabled = enabled;
            this.layoutMode = layoutMode;
            this.heroTitle = heroTitle;
            this.heroSubtitle = heroSubtitle;
            this.bannerImageUrl = bannerImageUrl;
            this.heroPrimaryButtonText = heroPrimaryButtonText;
            this.heroPrimaryButtonLink = heroPrimaryButtonLink;
            this.heroSecondaryButtonText = heroSecondaryButtonText;
            this.heroSecondaryButtonLink = heroSecondaryButtonLink;
        }
    }
}
