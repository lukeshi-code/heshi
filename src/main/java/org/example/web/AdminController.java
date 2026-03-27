package org.example.web;

import org.example.model.AccessLevel;
import org.example.model.DataSourceType;
import org.example.model.LayoutMode;
import org.example.model.LinkType;
import org.example.model.ModuleType;
import org.example.model.Role;
import org.example.model.SitePageConfig;
import org.example.model.SitePageVersion;
import org.example.model.UserAccount;
import org.example.service.HomeModuleConfigService;
import org.example.service.PagePermissionService;
import org.example.service.SiteMenuService;
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
    private final SiteMenuService siteMenuService;
    private final SiteOpsService siteOpsService;

    public AdminController(UserAccountService userAccountService,
                           PagePermissionService pagePermissionService,
                           SitePageConfigService sitePageConfigService,
                           HomeModuleConfigService homeModuleConfigService,
                           SiteMenuService siteMenuService,
                           SiteOpsService siteOpsService) {
        this.userAccountService = userAccountService;
        this.pagePermissionService = pagePermissionService;
        this.sitePageConfigService = sitePageConfigService;
        this.homeModuleConfigService = homeModuleConfigService;
        this.siteMenuService = siteMenuService;
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
                redirectAttributes.addFlashAttribute("errorMessage", "不能移除自己的管理员权限。");
                return "redirect:/admin/users";
            }
        }
        userAccountService.updateRoles(id, nextRoles);
        redirectAttributes.addFlashAttribute("successMessage", "用户角色已更新。");
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
        model.addAttribute("allModules", homeModuleConfigService.findAllModules());
        model.addAttribute("moduleTemplates", homeModuleConfigService.findTemplates());
        model.addAttribute("allModuleTypes", homeModuleConfigService.allModuleTypes());
        model.addAttribute("allDataSourceTypes", homeModuleConfigService.allDataSourceTypes());
        model.addAttribute("menuItems", siteMenuService.findAll());
        model.addAttribute("allLinkTypes", siteMenuService.allLinkTypes());
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
        siteOpsService.savePageSnapshot(before, homeModuleConfigService.findByPagePath(before.getRoutePath()));
        sitePageConfigService.update(id, pageName, navOrder, navVisible, enabled, layoutMode, heroTitle, heroSubtitle,
            bannerImageUrl, heroPrimaryButtonText, heroPrimaryButtonLink, heroSecondaryButtonText, heroSecondaryButtonLink);
        SitePageConfig page = sitePageConfigService.findById(id);
        pagePermissionService.updateByPathPattern(page.getRoutePath(), requiredLevel);
        siteOpsService.log(operator(authentication), "UPDATE_PAGE", page.getRoutePath(), "Updated page visual config");
        redirectAttributes.addFlashAttribute("successMessage", "页面配置已保存：" + page.getPageName());
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/menus/create")
    public String createMenu(@RequestParam(value = "menuName", required = false) String menuName,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        siteMenuService.create(menuName);
        siteOpsService.log(operator(authentication), "CREATE_MENU", "/admin/visual-pages", "Created menu item");
        redirectAttributes.addFlashAttribute("successMessage", "菜单已新增。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/menus/batch")
    public String batchSaveMenus(@RequestParam("menuId") List<Long> menuIds,
                                 @RequestParam("menuName") List<String> menuNames,
                                 @RequestParam(value = "parentId", required = false) List<Long> parentIds,
                                 @RequestParam("linkType") List<String> linkTypes,
                                 @RequestParam(value = "internalPath", required = false) List<String> internalPaths,
                                 @RequestParam(value = "externalUrl", required = false) List<String> externalUrls,
                                 @RequestParam(value = "visible", required = false) List<String> visibles,
                                 @RequestParam(value = "openInNewWindow", required = false) List<String> openWindows,
                                 @RequestParam("sortOrder") List<Integer> sortOrders,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        siteMenuService.batchSave(menuIds, menuNames, parentIds, linkTypes, internalPaths, externalUrls, visibles, openWindows, sortOrders);
        siteOpsService.log(operator(authentication), "BATCH_SAVE_MENU", "/admin/visual-pages", "Saved menu tree and configs");
        redirectAttributes.addFlashAttribute("successMessage", "菜单配置已保存。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/modules/create")
    public String createModule(@RequestParam("pagePath") String pagePath,
                               @RequestParam(value = "title", required = false) String title,
                               @RequestParam("moduleType") ModuleType moduleType,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        homeModuleConfigService.create(pagePath, title, moduleType);
        siteOpsService.log(operator(authentication), "CREATE_MODULE", pagePath, "Created module");
        redirectAttributes.addFlashAttribute("successMessage", "模块已新增。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/modules/{id}/advanced")
    public String updateModuleAdvanced(@PathVariable Long id,
                                       @RequestParam("title") String title,
                                       @RequestParam("moduleType") ModuleType moduleType,
                                       @RequestParam(value = "content", required = false) String content,
                                       @RequestParam(value = "buttonText", required = false) String buttonText,
                                       @RequestParam(value = "buttonLink", required = false) String buttonLink,
                                       @RequestParam("dataSourceType") DataSourceType dataSourceType,
                                       @RequestParam(value = "backgroundColor", required = false) String backgroundColor,
                                       @RequestParam(value = "backgroundImageUrl", required = false) String backgroundImageUrl,
                                       @RequestParam(value = "fontColor", required = false) String fontColor,
                                       @RequestParam(value = "fontSize", required = false) String fontSize,
                                       @RequestParam(value = "textAlign", required = false) String textAlign,
                                       @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                                       @RequestParam("sortOrder") Integer sortOrder,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        homeModuleConfigService.updateAdvanced(id, title, moduleType, content, buttonText, buttonLink, dataSourceType,
            backgroundColor, backgroundImageUrl, fontColor, fontSize, textAlign, enabled, sortOrder);
        siteOpsService.log(operator(authentication), "UPDATE_MODULE", "module#" + id, "Updated module advanced config");
        redirectAttributes.addFlashAttribute("successMessage", "模块配置已保存。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/modules/{id}/save-template")
    public String saveModuleTemplate(@PathVariable Long id,
                                     @RequestParam("templateName") String templateName,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        homeModuleConfigService.saveAsTemplate(id, templateName);
        siteOpsService.log(operator(authentication), "SAVE_TEMPLATE", "module#" + id, "Saved module as template");
        redirectAttributes.addFlashAttribute("successMessage", "模块模板已保存。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/modules/{id}/apply-template")
    public String applyModuleTemplate(@PathVariable Long id,
                                      @RequestParam("sourceTemplateId") Long sourceTemplateId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        homeModuleConfigService.applyTemplate(id, sourceTemplateId);
        siteOpsService.log(operator(authentication), "APPLY_TEMPLATE", "module#" + id, "Applied module template");
        redirectAttributes.addFlashAttribute("successMessage", "模板已应用到模块。");
        return "redirect:/admin/visual-pages";
    }

    @PostMapping("/admin/visual-pages/{id}/rollback-latest")
    public String rollbackLatest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        SitePageConfig page = sitePageConfigService.findById(id);
        SitePageVersion version = siteOpsService.latestSnapshot(page.getRoutePath());
        Map<String, Object> snap = siteOpsService.parseSnapshot(version);
        if (snap == null || !snap.containsKey("page")) {
            redirectAttributes.addFlashAttribute("errorMessage", "没有可回滚快照。");
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
            siteOpsService.log(operator(authentication), "ROLLBACK_PAGE", page.getRoutePath(), "Rollback to latest snapshot");
            redirectAttributes.addFlashAttribute("successMessage", "页面已回滚到最近快照。");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "回滚失败。");
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
}
