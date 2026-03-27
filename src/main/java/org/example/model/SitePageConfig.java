package org.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "site_page_configs")
public class SitePageConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String routePath;

    @Column(nullable = false, length = 80)
    private String pageName;

    @Column(nullable = false)
    private boolean navVisible = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int navOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LayoutMode layoutMode = LayoutMode.SIMPLE;

    @Column(length = 200)
    private String heroTitle;

    @Column(length = 500)
    private String heroSubtitle;

    @Column(length = 500)
    private String bannerImageUrl;

    @Column(length = 80)
    private String heroPrimaryButtonText;

    @Column(length = 200)
    private String heroPrimaryButtonLink;

    @Column(length = 80)
    private String heroSecondaryButtonText;

    @Column(length = 200)
    private String heroSecondaryButtonLink;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoutePath() { return routePath; }
    public void setRoutePath(String routePath) { this.routePath = routePath; }
    public String getPageName() { return pageName; }
    public void setPageName(String pageName) { this.pageName = pageName; }
    public boolean isNavVisible() { return navVisible; }
    public void setNavVisible(boolean navVisible) { this.navVisible = navVisible; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getNavOrder() { return navOrder; }
    public void setNavOrder(int navOrder) { this.navOrder = navOrder; }
    public LayoutMode getLayoutMode() { return layoutMode; }
    public void setLayoutMode(LayoutMode layoutMode) { this.layoutMode = layoutMode; }
    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }
    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }
    public String getBannerImageUrl() { return bannerImageUrl; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }
    public String getHeroPrimaryButtonText() { return heroPrimaryButtonText; }
    public void setHeroPrimaryButtonText(String heroPrimaryButtonText) { this.heroPrimaryButtonText = heroPrimaryButtonText; }
    public String getHeroPrimaryButtonLink() { return heroPrimaryButtonLink; }
    public void setHeroPrimaryButtonLink(String heroPrimaryButtonLink) { this.heroPrimaryButtonLink = heroPrimaryButtonLink; }
    public String getHeroSecondaryButtonText() { return heroSecondaryButtonText; }
    public void setHeroSecondaryButtonText(String heroSecondaryButtonText) { this.heroSecondaryButtonText = heroSecondaryButtonText; }
    public String getHeroSecondaryButtonLink() { return heroSecondaryButtonLink; }
    public void setHeroSecondaryButtonLink(String heroSecondaryButtonLink) { this.heroSecondaryButtonLink = heroSecondaryButtonLink; }
}
