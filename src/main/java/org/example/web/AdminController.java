package org.example.web;

import org.example.model.AccessLevel;
import org.example.model.DataSourceType;
import org.example.model.LayoutMode;
import org.example.model.ModuleType;
import org.example.model.Role;
import org.example.model.SitePageConfig;
import org.example.model.SitePageVersion;
import org.example.model.UserAccount;
import org.example.repository.ArticleRepository;
import org.example.repository.HomeModuleConfigRepository;
import org.example.repository.SiteMenuItemRepository;
import org.example.repository.SitePageConfigRepository;
import org.example.repository.UserAccountRepository;
import org.example.service.ArticleService;
import org.example.service.FileStorageService;
import org.example.service.HomeModuleConfigService;
import org.example.service.PagePermissionService;
import org.example.service.SiteMenuService;
import org.example.service.SiteOpsService;
import org.example.service.SitePageConfigService;
import org.example.service.SiteSettingService;
import org.example.service.UserAccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Controller
public class AdminController {
    private final UserAccountService userAccountService;
    private final PagePermissionService pagePermissionService;
    private final SitePageConfigService sitePageConfigService;
    private final HomeModuleConfigService homeModuleConfigService;
    private final SiteMenuService siteMenuService;
    private final SiteOpsService siteOpsService;
    private final SiteSettingService siteSettingService;
    private final ArticleService articleService;
    private final FileStorageService fileStorageService;
    private final UserAccountRepository userAccountRepository;
    private final ArticleRepository articleRepository;
    private final SitePageConfigRepository sitePageConfigRepository;
    private final HomeModuleConfigRepository homeModuleConfigRepository;
    private final SiteMenuItemRepository siteMenuItemRepository;
    private final Path uploadRoot;

    public AdminController(UserAccountService userAccountService,
                           PagePermissionService pagePermissionService,
                           SitePageConfigService sitePageConfigService,
                           HomeModuleConfigService homeModuleConfigService,
                           SiteMenuService siteMenuService,
                           SiteOpsService siteOpsService,
                           SiteSettingService siteSettingService,
                           ArticleService articleService,
                           FileStorageService fileStorageService,
                           UserAccountRepository userAccountRepository,
                           ArticleRepository articleRepository,
                           SitePageConfigRepository sitePageConfigRepository,
                           HomeModuleConfigRepository homeModuleConfigRepository,
                           SiteMenuItemRepository siteMenuItemRepository,
                           @Value("${app.upload-dir}") String uploadDir) {
        this.userAccountService = userAccountService;
        this.pagePermissionService = pagePermissionService;
        this.sitePageConfigService = sitePageConfigService;
        this.homeModuleConfigService = homeModuleConfigService;
        this.siteMenuService = siteMenuService;
        this.siteOpsService = siteOpsService;
        this.siteSettingService = siteSettingService;
        this.articleService = articleService;
        this.fileStorageService = fileStorageService;
        this.userAccountRepository = userAccountRepository;
        this.articleRepository = articleRepository;
        this.sitePageConfigRepository = sitePageConfigRepository;
        this.homeModuleConfigRepository = homeModuleConfigRepository;
        this.siteMenuItemRepository = siteMenuItemRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }
    @PostMapping("/admin/users/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(value = "roles", required = false) Set<Role> roles,
                              @RequestParam(value = "redirectTab", required = false) String redirectTab,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        Set<Role> nextRoles = roles == null || roles.isEmpty() ? new HashSet<Role>() : new HashSet<Role>(roles);
        if (!nextRoles.contains(Role.ROLE_USER)) nextRoles.add(Role.ROLE_USER);
        if (nextRoles.contains(Role.ROLE_ADMIN) && !nextRoles.contains(Role.ROLE_EDITOR)) nextRoles.add(Role.ROLE_EDITOR);
        if (authentication != null) {
            UserAccount current = userAccountService.getByUsername(authentication.getName());
            if (current.getId().equals(id) && !nextRoles.contains(Role.ROLE_ADMIN)) {
                redirectAttributes.addFlashAttribute("errorMessage", "不能移除自己的管理员权限。");
                return redirectByTab(redirectTab);
            }
        }
        userAccountService.updateRoles(id, nextRoles);
        redirectAttributes.addFlashAttribute("successMessage", "用户角色已更新。");
        return redirectByTab(redirectTab);
    }

    @GetMapping("/admin/page-permissions")
    public String pagePermissionsRedirect() {
        return "redirect:/admin/visual-pages?tab=permission";
    }

    @GetMapping("/admin/visual-pages")
    public String visualPages(@RequestParam(value = "tab", defaultValue = "dashboard") String tab, Model model) {
        String activeTab = normalizeTab(tab);
        model.addAttribute("activeTab", activeTab);

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

        model.addAttribute("articles", articleService.findAll());
        model.addAttribute("settingMap", siteSettingService.map());
        model.addAttribute("assetFiles", listAssetFiles());

        model.addAttribute("metricUserCount", userAccountRepository.count());
        model.addAttribute("metricArticleCount", articleRepository.count());
        model.addAttribute("metricPageCount", sitePageConfigRepository.count());
        model.addAttribute("metricModuleCount", homeModuleConfigRepository.count());
        model.addAttribute("metricMenuCount", siteMenuItemRepository.count());
        model.addAttribute("users", userAccountService.findAllUsers());
        model.addAttribute("allRoles", Role.values());
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
        return "redirect:/admin/visual-pages?tab=page";
    }

    @PostMapping("/admin/menus/create")
    public String createMenu(@RequestParam(value = "menuName", required = false) String menuName,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        siteMenuService.create(menuName);
        siteOpsService.log(operator(authentication), "CREATE_MENU", "/admin/visual-pages", "Created menu item");
        redirectAttributes.addFlashAttribute("successMessage", "菜单已新增。");
        return "redirect:/admin/visual-pages?tab=navigation";
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
        return "redirect:/admin/visual-pages?tab=navigation";
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
        return "redirect:/admin/visual-pages?tab=module";
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
        return "redirect:/admin/visual-pages?tab=module";
    }

    @PostMapping("/admin/modules/{id}/save-template")
    public String saveModuleTemplate(@PathVariable Long id,
                                     @RequestParam("templateName") String templateName,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        homeModuleConfigService.saveAsTemplate(id, templateName);
        siteOpsService.log(operator(authentication), "SAVE_TEMPLATE", "module#" + id, "Saved module as template");
        redirectAttributes.addFlashAttribute("successMessage", "模块模板已保存。");
        return "redirect:/admin/visual-pages?tab=module";
    }

    @PostMapping("/admin/modules/{id}/apply-template")
    public String applyModuleTemplate(@PathVariable Long id,
                                      @RequestParam("sourceTemplateId") Long sourceTemplateId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        homeModuleConfigService.applyTemplate(id, sourceTemplateId);
        siteOpsService.log(operator(authentication), "APPLY_TEMPLATE", "module#" + id, "Applied module template");
        redirectAttributes.addFlashAttribute("successMessage", "模板已应用到模块。");
        return "redirect:/admin/visual-pages?tab=module";
    }

    @PostMapping("/admin/visual-pages/{id}/rollback-latest")
    public String rollbackLatest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        SitePageConfig page = sitePageConfigService.findById(id);
        SitePageVersion version = siteOpsService.latestSnapshot(page.getRoutePath());
        Map<String, Object> snap = siteOpsService.parseSnapshot(version);
        if (snap == null || !snap.containsKey("page")) {
            redirectAttributes.addFlashAttribute("errorMessage", "没有可回滚快照。");
            return "redirect:/admin/visual-pages?tab=page";
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
        return "redirect:/admin/visual-pages?tab=page";
    }

    @PostMapping("/admin/articles/{id}/delete")
    public String deleteArticle(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        articleService.deleteArticle(id);
        siteOpsService.log(operator(authentication), "DELETE_ARTICLE", "article#" + id, "Deleted article");
        redirectAttributes.addFlashAttribute("successMessage", "文章已删除。");
        return "redirect:/admin/visual-pages?tab=content";
    }

    @PostMapping("/admin/resources/upload")
    public String uploadResource(@RequestParam("file") MultipartFile file,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "请选择要上传的文件。");
            return "redirect:/admin/visual-pages?tab=assets";
        }
        String path = fileStorageService.store(file);
        siteOpsService.log(operator(authentication), "UPLOAD_RESOURCE", path, "Uploaded resource file");
        redirectAttributes.addFlashAttribute("successMessage", "资源上传成功：" + path);
        return "redirect:/admin/visual-pages?tab=assets";
    }

    @PostMapping("/admin/theme/save")
    public String saveTheme(@RequestParam(value = "primaryColor", required = false) String primaryColor,
                            @RequestParam(value = "accentColor", required = false) String accentColor,
                            @RequestParam(value = "logoText", required = false) String logoText,
                            @RequestParam(value = "bannerUrl", required = false) String bannerUrl,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        siteSettingService.save("theme.primaryColor", valueOrEmpty(primaryColor), "后台主色");
        siteSettingService.save("theme.accentColor", valueOrEmpty(accentColor), "后台强调色");
        siteSettingService.save("theme.logoText", valueOrEmpty(logoText), "后台 Logo 文案");
        siteSettingService.save("theme.bannerUrl", valueOrEmpty(bannerUrl), "后台默认 Banner 图 URL");
        siteOpsService.log(operator(authentication), "SAVE_THEME", "/admin/visual-pages", "Saved theme settings");
        redirectAttributes.addFlashAttribute("successMessage", "主题设置已保存。");
        return "redirect:/admin/visual-pages?tab=theme";
    }

    @PostMapping("/admin/system/save")
    public String saveSystem(@RequestParam(value = "siteTitle", required = false) String siteTitle,
                             @RequestParam(value = "footerText", required = false) String footerText,
                             @RequestParam(value = "maintenanceMode", required = false, defaultValue = "false") boolean maintenanceMode,
                             @RequestParam(value = "allowRegister", required = false, defaultValue = "false") boolean allowRegister,
                             @RequestParam(value = "stockDataProvider", required = false) String stockDataProvider,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        siteSettingService.save("system.siteTitle", valueOrEmpty(siteTitle), "站点名称");
        siteSettingService.save("system.footerText", valueOrEmpty(footerText), "页脚文案");
        siteSettingService.save("system.maintenanceMode", String.valueOf(maintenanceMode), "维护模式");
        siteSettingService.save("system.allowRegister", String.valueOf(allowRegister), "是否允许注册");
        siteSettingService.save("system.stockDataProvider", valueOrEmpty(stockDataProvider), "行情数据源说明");
        siteOpsService.log(operator(authentication), "SAVE_SYSTEM", "/admin/visual-pages", "Saved system settings");
        redirectAttributes.addFlashAttribute("successMessage", "系统设置已保存。");
        return "redirect:/admin/visual-pages?tab=system";
    }

    private List<AssetFileRow> listAssetFiles() {
        List<AssetFileRow> rows = new ArrayList<AssetFileRow>();
        if (!Files.exists(uploadRoot)) return rows;
        try (Stream<Path> stream = Files.list(uploadRoot)) {
            stream
                .filter(Files::isRegularFile)
                .forEach(path -> rows.add(toAssetRow(path)));
        } catch (IOException ignore) {
            return rows;
        }
        Collections.sort(rows, Comparator.comparing(AssetFileRow::getModifiedAt).reversed());
        return rows.size() > 30 ? rows.subList(0, 30) : rows;
    }

    private AssetFileRow toAssetRow(Path file) {
        AssetFileRow row = new AssetFileRow();
        row.setName(file.getFileName().toString());
        row.setUrl("/uploads/" + file.getFileName().toString());
        try {
            row.setSizeKb((long) Math.ceil(Files.size(file) / 1024.0));
            FileTime time = Files.getLastModifiedTime(file);
            Instant instant = time.toInstant();
            row.setModifiedAt(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
        } catch (IOException ex) {
            row.setSizeKb(0L);
            row.setModifiedAt(LocalDateTime.now());
        }
        return row;
    }

    private String normalizeTab(String tab) {
        if (tab == null) return "dashboard";
        String value = tab.trim().toLowerCase();
        if ("dashboard".equals(value) || "page".equals(value) || "content".equals(value)
            || "module".equals(value) || "assets".equals(value) || "navigation".equals(value)
            || "theme".equals(value) || "permission".equals(value) || "stats".equals(value)
            || "system".equals(value)) {
            return value;
        }
        return "dashboard";
    }

    private String operator(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return "system";
        return authentication.getName();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String redirectByTab(String tab) {
        if (tab != null && !tab.trim().isEmpty()) {
            return "redirect:/admin/visual-pages?tab=" + tab.trim();
        }
        return "redirect:/admin/visual-pages?tab=permission";
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

    public static class AssetFileRow {
        private String name;
        private String url;
        private Long sizeKb;
        private LocalDateTime modifiedAt;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Long getSizeKb() {
            return sizeKb;
        }

        public void setSizeKb(Long sizeKb) {
            this.sizeKb = sizeKb;
        }

        public LocalDateTime getModifiedAt() {
            return modifiedAt;
        }

        public void setModifiedAt(LocalDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
        }
    }
}
