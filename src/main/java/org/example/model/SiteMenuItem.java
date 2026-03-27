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
@Table(name = "site_menu_items")
public class SiteMenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long parentId;

    @Column(nullable = false, length = 80)
    private String menuName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkType linkType = LinkType.INTERNAL;

    @Column(length = 120)
    private String internalPath;

    @Column(length = 260)
    private String externalUrl;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private boolean openInNewWindow = false;

    @Column(nullable = false)
    private int sortOrder = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }
    public LinkType getLinkType() { return linkType; }
    public void setLinkType(LinkType linkType) { this.linkType = linkType; }
    public String getInternalPath() { return internalPath; }
    public void setInternalPath(String internalPath) { this.internalPath = internalPath; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isOpenInNewWindow() { return openInNewWindow; }
    public void setOpenInNewWindow(boolean openInNewWindow) { this.openInNewWindow = openInNewWindow; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getResolvedUrl() {
        if (linkType == LinkType.EXTERNAL) {
            return externalUrl == null || externalUrl.trim().isEmpty() ? "#" : externalUrl.trim();
        }
        return internalPath == null || internalPath.trim().isEmpty() ? "/" : internalPath.trim();
    }
}
