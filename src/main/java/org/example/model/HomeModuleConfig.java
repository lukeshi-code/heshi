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
@Table(name = "home_module_configs")
public class HomeModuleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String pagePath = "/";

    @Column(nullable = false, length = 80)
    private String moduleKey;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModuleType moduleType = ModuleType.IMAGE_TEXT;

    @Column(length = 800)
    private String content;

    @Column(length = 80)
    private String buttonText;

    @Column(length = 200)
    private String buttonLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSourceType dataSourceType = DataSourceType.MANUAL;

    @Column(length = 40)
    private String backgroundColor;

    @Column(length = 260)
    private String backgroundImageUrl;

    @Column(length = 40)
    private String fontColor;

    @Column(length = 20)
    private String fontSize;

    @Column(length = 20)
    private String textAlign;

    @Column(length = 120)
    private String templateName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPagePath() { return pagePath; }
    public void setPagePath(String pagePath) { this.pagePath = pagePath; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ModuleType getModuleType() { return moduleType; }
    public void setModuleType(ModuleType moduleType) { this.moduleType = moduleType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }
    public String getButtonLink() { return buttonLink; }
    public void setButtonLink(String buttonLink) { this.buttonLink = buttonLink; }
    public DataSourceType getDataSourceType() { return dataSourceType; }
    public void setDataSourceType(DataSourceType dataSourceType) { this.dataSourceType = dataSourceType; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
    public String getBackgroundImageUrl() { return backgroundImageUrl; }
    public void setBackgroundImageUrl(String backgroundImageUrl) { this.backgroundImageUrl = backgroundImageUrl; }
    public String getFontColor() { return fontColor; }
    public void setFontColor(String fontColor) { this.fontColor = fontColor; }
    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }
    public String getTextAlign() { return textAlign; }
    public void setTextAlign(String textAlign) { this.textAlign = textAlign; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
