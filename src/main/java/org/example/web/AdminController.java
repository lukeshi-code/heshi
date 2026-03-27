package org.example.web;

import org.example.model.AccessLevel;
import org.example.model.LayoutMode;
import org.example.model.Role;
import org.example.model.SitePageConfig;
import org.example.model.SitePageVersion;
import org.example.model.UserAccount;
import org.example.service.HomeModuleConfigService;
import org.example.service.PagePermissionService;
import org.example.service.SiteOpsService;
import org.example.service.SitePageConfigService;
import org.example.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class AdminController {
    private final UserAccountService userAccountService;
    private final PagePermissionService pagePermissionService;
    private final SitePageConfigService sitePageConfigService;
    private final HomeModuleConfigService homeModuleConfigService;
    private final SiteOpsService siteOpsService;

    public AdminController(UserAccountService userAccountService,
                           PagePermissionService pagePermissionService,
                           SitePageConfigService sitePageConfigService,
                           HomeModuleConfigService homeModuleConfigService,
                           SiteOpsService siteOpsService) {
        this.userAccountService = userAccountService;
        this.pagePermissionService = pagePermissionService;
        this.sitePageConfigService = sitePageConfigService;
        this.homeModuleConfigService = homeModuleConfigService;
        this.siteOpsService = siteOpsService;
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", userAccountService.findAllUsers());
        model.addAttribute("allRoles", Role.values());
        return "admin-users";
    }

    @PostMapping("/admin/users/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(value = "roles", required = false) Set<Role> roles,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Set<Role> nextRoles = roles == null || roles.isEmpty() ? new HashSet<Role>() : new HashSet<Role>(roles);
        if (!nextRoles.contains(Role.ROLE_USER)) nextRoles.add(Role.ROLE_USER);
        if (nextRoles.contains(Role.ROLE_ADMIN) && !nextRoles.contains(Role.ROLE_EDITOR)) nextRoles.add(Role.ROLE_EDITOR);
        if (authentication != null) {
            UserAccount current = userAccountService.getByUsername(authentication.getName());
            if (current.getId().equals(id) && !nextRoles.contains(Role.ROLE_ADMIN)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot remove your own admin role.");
                return "redirect:/admin/users";
            }
        }
        userAccountService.updateRoles(id, nextRoles);
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/page-permissions")
    public String pagePermissionsRedirect() {
        return "redirect:/admin/visual-pages";
    }

    @GetMapping("/admin/visual-pages")
    public String visualPages(Model model) {
        model.addAttribute("pages", sitePageConfigService.findAll());
        model.addAttribute("allLayoutModes", sitePageConfigService.allLayoutModes());
        model.addAttribute("allLevels", pagePermissionService.allLevels());
        model.addAttribute("pageLevelByPath", pagePermissionService.levelByPath());
        model.addAttribute("homeModules", homeModuleConfigService.findHomeModules());
        model.addAttribute("logs", siteOpsService.recentLogs());
        return "admin-visual-pages";
    }

    @PostMapping("/admin/visual-pages/{id}")
    public String updateVisualPage(@PathVariable Long id,
                                   @RequestParam("pageName") String pageName,
                                   @RequestParam("navOrder") Integer navOrder,
                                   @RequestParam(value = "navVisible", defaultValue = "false") boolean navVisible,
                                   @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                                   @RequestParam("layoutMode") LayoutMode layoutMode,
                                   @RequestParam(value = "heroTitle", required = false) String heroTitle,
                                   @RequestParam(value = "heroSubtitle", required = false) String heroSubtitle,
                                   @RequestParam(value = "bannerImageUrl", required = false) String bannerImageUrl,
                                   @RequestParam(value = "heroPrimaryButtonText", required = false) String heroPrimaryButtonText,
                                   @RequestParam(value = "heroPrimaryButtonLink", required = false) String heroPrimaryButtonLink,
                                   @RequestParam(value = "heroSecondaryButtonText", required = false) String heroSecondaryButtonText,
                                   @RequestParam(value = "heroSecondaryButtonLink", required = false) String heroSecondaryButtonLink,
                                   @RequestParam("requiredLevel") AccessLevel requiredLevel,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        SitePageConfig before = sitePageConfigService.findById(id);
        siteOpsService.savePageSnapshot(before, homeModuleConfigService.findHomeModules());
        sitePageConfigService.update(id, pageName, navOrder, navVisible, enabled, layoutMode, heroTitle, heroSubtitle,
            bannerImageUrl, heroPrimaryButtonText, heroPrimaryButtonLink, heroSecondaryButtonText, heroSecondaryButtonLink);
        SitePageConfig page = sitePageConfigService.findById(id);
        pagePermissionService.updateByPathPattern(page.getRoutePath(), requiredLevel);
        siteOpsService.log(operator(authentication), "UPDATE_PAGE", page.getRoutePath(), "Updated visual config");
        redirectAttributes.addFlashAttribute("successMessage", "页面配置已保存：" + page.getPageName());
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/visual-pages/home-decor")
    public String saveHomeDecor(@RequestParam(value = "bannerImageUrl", required = false) String bannerImageUrl,
                                @RequestParam(value = "heroTitle", required = false) String heroTitle,
                                @RequestParam(value = "heroSubtitle", required = false) String heroSubtitle,
                                @RequestParam(value = "heroPrimaryButtonText", required = false) String heroPrimaryButtonText,
                                @RequestParam(value = "heroPrimaryButtonLink", required = false) String heroPrimaryButtonLink,
                                @RequestParam(value = "heroSecondaryButtonText", required = false) String heroSecondaryButtonText,
                                @RequestParam(value = "heroSecondaryButtonLink", required = false) String heroSecondaryButtonLink,
                                @RequestParam(value = "moduleId", required = false) List<Long> moduleIds,
                                @RequestParam(value = "moduleTitle", required = false) List<String> moduleTitles,
                                @RequestParam(value = "moduleContent", required = false) List<String> moduleContents,
                                @RequestParam(value = "moduleButtonText", required = false) List<String> moduleButtonTexts,
                                @RequestParam(value = "moduleButtonLink", required = false) List<String> moduleButtonLinks,
                                @RequestParam(value = "moduleEnabled", required = false) List<String> moduleEnabledValues,
                                @RequestParam(value = "moduleSort", required = false) List<Integer> moduleSorts,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        SitePageConfig home = sitePageConfigService.findByRoutePath("/");
        siteOpsService.savePageSnapshot(home, homeModuleConfigService.findHomeModules());
        sitePageConfigService.update(home.getId(), home.getPageName(), home.getNavOrder(), home.isNavVisible(),
            home.isEnabled(), home.getLayoutMode(), heroTitle, heroSubtitle, bannerImageUrl,
            heroPrimaryButtonText, heroPrimaryButtonLink, heroSecondaryButtonText, heroSecondaryButtonLink);

        if (moduleIds != null) {
            for (int i = 0; i < moduleIds.size(); i++) {
                Long id = moduleIds.get(i);
                String title = listValue(moduleTitles, i);
                String content = listValue(moduleContents, i);
                String buttonText = listValue(moduleButtonTexts, i);
                String buttonLink = listValue(moduleButtonLinks, i);
                boolean enabled = boolValue(listValue(moduleEnabledValues, i), true);
                Integer sortOrder = listIntValue(moduleSorts, i, i + 1);
                homeModuleConfigService.update(id, title, content, buttonText, buttonLink, enabled, sortOrder);
            }
        }
        siteOpsService.log(operator(authentication), "UPDATE_HOME_DECOR", "/", "Updated home visual decorator");
        redirectAttributes.addFlashAttribute("successMessage", "首页装修保存成功。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/visual-pages/permissions/batch")
    public String batchUpdatePermissions(@RequestParam("pathPattern") List<String> pathPatterns,
                                         @RequestParam("requiredLevel") List<AccessLevel> levels,
                                         @RequestParam(value = "pageName", required = false) List<String> pageNames,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        int total = Math.min(pathPatterns == null ? 0 : pathPatterns.size(), levels == null ? 0 : levels.size());
        for (int i = 0; i < total; i++) {
            String path = listValue(pathPatterns, i);
            AccessLevel level = levels.get(i);
            String pageName = listValue(pageNames, i);
            pagePermissionService.upsertByPathPattern(path, pageName, level);
        }
        siteOpsService.log(operator(authentication), "BATCH_UPDATE_PERMISSION", "/admin/visual-pages", "Batch updated page levels");
        redirectAttributes.addFlashAttribute("successMessage", "页面权限已批量保存。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/visual-pages/modules/{id}")
    public String updateHomeModule(@PathVariable Long id,
                                   @RequestParam("title") String title,
                                   @RequestParam(value = "content", required = false) String content,
                                   @RequestParam(value = "buttonText", required = false) String buttonText,
                                   @RequestParam(value = "buttonLink", required = false) String buttonLink,
                                   @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                                   @RequestParam("sortOrder") Integer sortOrder,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        homeModuleConfigService.update(id, title, content, buttonText, buttonLink, enabled, sortOrder);
        siteOpsService.log(operator(authentication), "UPDATE_MODULE", "home-module#" + id, "Updated module config");
        redirectAttributes.addFlashAttribute("successMessage", "模块配置已保存。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/visual-pages/{id}/rollback-latest")
    public String rollbackLatest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        SitePageConfig page = sitePageConfigService.findById(id);
        SitePageVersion version = siteOpsService.latestSnapshot(page.getRoutePath());
        Map<String, Object> snap = siteOpsService.parseSnapshot(version);
        if (snap == null || !snap.containsKey("page")) {
            redirectAttributes.addFlashAttribute("errorMessage", "No available snapshot for rollback.");
            return "redirect:/admin/visual-pages";
        }
        try {
            Map<?, ?> pageMap = (Map<?, ?>) snap.get("page");
            page.setPageName(stringValue(pageMap.get("pageName"), page.getPageName()));
            page.setNavOrder(intValue(pageMap.get("navOrder"), page.getNavOrder()));
            page.setNavVisible(boolValue(pageMap.get("navVisible"), page.isNavVisible()));
            page.setEnabled(boolValue(pageMap.get("enabled"), page.isEnabled()));
            String lm = stringValue(pageMap.get("layoutMode"), page.getLayoutMode().name());
            page.setLayoutMode(LayoutMode.valueOf(lm));
            page.setHeroTitle(stringOrNull(pageMap.get("heroTitle")));
            page.setHeroSubtitle(stringOrNull(pageMap.get("heroSubtitle")));
            page.setBannerImageUrl(stringOrNull(pageMap.get("bannerImageUrl")));
            page.setHeroPrimaryButtonText(stringOrNull(pageMap.get("heroPrimaryButtonText")));
            page.setHeroPrimaryButtonLink(stringOrNull(pageMap.get("heroPrimaryButtonLink")));
            page.setHeroSecondaryButtonText(stringOrNull(pageMap.get("heroSecondaryButtonText")));
            page.setHeroSecondaryButtonLink(stringOrNull(pageMap.get("heroSecondaryButtonLink")));
            sitePageConfigService.save(page);

            Object modulesObj = snap.get("modules");
            if (modulesObj instanceof java.util.List) {
                java.util.List<?> modules = (java.util.List<?>) modulesObj;
                for (Object mObj : modules) {
                    if (!(mObj instanceof Map)) continue;
                    Map<?, ?> m = (Map<?, ?>) mObj;
                    String moduleKey = stringValue(m.get("moduleKey"), "");
                    if (moduleKey.isEmpty()) continue;
                    homeModuleConfigService.restoreByModuleKey("/",
                        moduleKey,
                        stringValue(m.get("title"), moduleKey),
                        stringOrNull(m.get("content")),
                        stringOrNull(m.get("buttonText")),
                        stringOrNull(m.get("buttonLink")),
                        boolValue(m.get("enabled"), true),
                        intValue(m.get("sortOrder"), 0));
                }
            }
            siteOpsService.log(operator(authentication), "ROLLBACK_PAGE", page.getRoutePath(), "Rollback to latest snapshot");
            redirectAttributes.addFlashAttribute("successMessage", "页面已回滚到最近快照。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rollback failed.");
        }
        return "redirect:/admin/visual-pages";
    }

    private String operator(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return "system";
        return authentication.getName();
    }

    private String stringValue(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v);
        return s.trim().isEmpty() ? def : s.trim();
    }

    private String stringOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private int intValue(Object v, int def) {
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ex) {
            return def;
        }
    }

    private boolean boolValue(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    private String listValue(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return "";
        String value = values.get(index);
        return value == null ? "" : value;
    }

    private Integer listIntValue(List<Integer> values, int index, int def) {
        if (values == null || index < 0 || index >= values.size()) return def;
        Integer value = values.get(index);
        return value == null ? def : value;
    }
}
