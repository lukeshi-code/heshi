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
@Table(name = "page_permissions")
public class PagePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String pathPattern;

    @Column(nullable = false, length = 80)
    private String pageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessLevel requiredLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
    public String getPageName() { return pageName; }
    public void setPageName(String pageName) { this.pageName = pageName; }
    public AccessLevel getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(AccessLevel requiredLevel) { this.requiredLevel = requiredLevel; }
}
