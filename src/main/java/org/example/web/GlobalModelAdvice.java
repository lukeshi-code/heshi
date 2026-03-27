package org.example.web;

import org.example.service.PagePermissionService;
import org.example.service.HomeModuleConfigService;
import org.example.service.SiteMenuService;
import org.example.service.SitePageConfigService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.example.model.SitePageConfig;
import org.example.model.HomeModuleConfig;
import org.example.model.SiteMenuItem;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {
    private final PagePermissionService pagePermissionService;
    private final SitePageConfigService sitePageConfigService;
    private final HomeModuleConfigService homeModuleConfigService;
    private final SiteMenuService siteMenuService;

    public GlobalModelAdvice(PagePermissionService pagePermissionService,
                             SitePageConfigService sitePageConfigService,
                             HomeModuleConfigService homeModuleConfigService,
                             SiteMenuService siteMenuService) {
        this.pagePermissionService = pagePermissionService;
        this.sitePageConfigService = sitePageConfigService;
        this.homeModuleConfigService = homeModuleConfigService;
        this.siteMenuService = siteMenuService;
    }

    @ModelAttribute("authenticated")
    public boolean authenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal());
    }

    @ModelAttribute("adminUser")
    public boolean adminUser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @ModelAttribute("editorUser")
    public boolean editorUser(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_EDITOR".equals(authority.getAuthority())
                || "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @ModelAttribute("canAccessArticleCreate")
    public boolean canAccessArticleCreate(Authentication authentication) {
        return pagePermissionService.canAccess("/articles/new", authentication);
    }

    @ModelAttribute("navPages")
    public List<SitePageConfig> navPages() {
        return sitePageConfigService.navPages();
    }

    @ModelAttribute("navMenus")
    public List<SiteMenuItem> navMenus() {
        return siteMenuService.visibleTopMenus();
    }

    @ModelAttribute("homePageConfig")
    public SitePageConfig homePageConfig() {
        return sitePageConfigService.homeConfig();
    }

    @ModelAttribute("homeModules")
    public List<HomeModuleConfig> homeModules() {
        return homeModuleConfigService.findHomeModules();
    }

    @ModelAttribute("currentPageConfig")
    public SitePageConfig currentPageConfig(HttpServletRequest request) {
        if (request == null) return null;
        String path = request.getRequestURI();
        if (path == null || path.trim().isEmpty()) return null;
        path = path.trim();
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.startsWith("/admin") || path.startsWith("/api")
            || path.startsWith("/css") || path.startsWith("/js")
            || path.startsWith("/uploads")) {
            return null;
        }
        try {
            return sitePageConfigService.findByRoutePath(path);
        } catch (Exception ex) {
            return null;
        }
    }
}
