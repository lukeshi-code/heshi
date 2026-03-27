package org.example.service;

import org.example.model.LinkType;
import org.example.model.SiteMenuItem;
import org.example.model.SitePageConfig;
import org.example.repository.SiteMenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class SiteMenuService {
    private final SiteMenuItemRepository repository;
    private final SitePageConfigService sitePageConfigService;

    public SiteMenuService(SiteMenuItemRepository repository, SitePageConfigService sitePageConfigService) {
        this.repository = repository;
        this.sitePageConfigService = sitePageConfigService;
    }

    public void ensureDefaults() {
        if (!repository.findAll().isEmpty()) {
            return;
        }
        List<SitePageConfig> pages = sitePageConfigService.findAll();
        int order = 1;
        for (SitePageConfig page : pages) {
            if (!page.isNavVisible()) continue;
            SiteMenuItem item = new SiteMenuItem();
            item.setMenuName(page.getPageName());
            item.setLinkType(LinkType.INTERNAL);
            item.setInternalPath(page.getRoutePath());
            item.setVisible(page.isEnabled());
            item.setOpenInNewWindow(false);
            item.setSortOrder(order++);
            repository.save(item);
        }
    }

    public List<SiteMenuItem> findAll() {
        List<SiteMenuItem> rows = repository.findAllByOrderBySortOrderAscIdAsc();
        rows.sort((a, b) -> {
            int p = Long.compare(safeParent(a.getParentId()), safeParent(b.getParentId()));
            if (p != 0) return p;
            int s = Integer.compare(a.getSortOrder(), b.getSortOrder());
            if (s != 0) return s;
            return Long.compare(a.getId(), b.getId());
        });
        return rows;
    }

    public List<SiteMenuItem> visibleTopMenus() {
        List<SiteMenuItem> out = new ArrayList<SiteMenuItem>();
        for (SiteMenuItem row : findAll()) {
            if (row.isVisible() && row.getParentId() == null) {
                out.add(row);
            }
        }
        return out;
    }

    public SiteMenuItem create(String menuName) {
        List<SiteMenuItem> all = findAll();
        SiteMenuItem row = new SiteMenuItem();
        row.setMenuName(menuName == null || menuName.trim().isEmpty() ? "新菜单" : menuName.trim());
        row.setLinkType(LinkType.INTERNAL);
        row.setInternalPath("/");
        row.setVisible(true);
        row.setOpenInNewWindow(false);
        row.setSortOrder(all.size() + 1);
        return repository.save(row);
    }

    public void batchSave(List<Long> ids,
                          List<String> menuNames,
                          List<Long> parentIds,
                          List<String> linkTypes,
                          List<String> internalPaths,
                          List<String> externalUrls,
                          List<String> visibles,
                          List<String> openNewWindows,
                          List<Integer> sortOrders) {
        int total = minSize(Arrays.asList(ids, menuNames, linkTypes));
        for (int i = 0; i < total; i++) {
            Long id = ids.get(i);
            if (id == null) continue;
            SiteMenuItem row = repository.findById(id).orElse(null);
            if (row == null) continue;
            row.setMenuName(text(menuNames, i, row.getMenuName()));
            row.setParentId(parent(parentIds, i));
            row.setLinkType(parseLinkType(text(linkTypes, i, "INTERNAL")));
            row.setInternalPath(text(internalPaths, i, "/"));
            row.setExternalUrl(text(externalUrls, i, ""));
            row.setVisible(bool(visibles, i, row.isVisible()));
            row.setOpenInNewWindow(bool(openNewWindows, i, row.isOpenInNewWindow()));
            row.setSortOrder(number(sortOrders, i, row.getSortOrder()));
            repository.save(row);
        }
    }

    public void syncMenusFromPage(SitePageConfig page) {
        if (page == null) return;
        String routePath = normalizePath(page.getRoutePath());
        if (routePath == null || routePath.isEmpty()) return;

        List<SiteMenuItem> all = repository.findAllByOrderBySortOrderAscIdAsc();
        boolean matched = false;
        for (SiteMenuItem row : all) {
            if (row.getLinkType() != LinkType.INTERNAL) continue;
            if (!routePath.equals(normalizePath(row.getInternalPath()))) continue;
            matched = true;
            boolean changed = false;
            String pageName = safeText(page.getPageName(), row.getMenuName());
            if (!pageName.equals(row.getMenuName())) {
                row.setMenuName(pageName);
                changed = true;
            }
            if (row.getParentId() == null) {
                boolean visible = page.isEnabled() && page.isNavVisible();
                if (row.isVisible() != visible) {
                    row.setVisible(visible);
                    changed = true;
                }
                if (row.getSortOrder() != page.getNavOrder()) {
                    row.setSortOrder(page.getNavOrder());
                    changed = true;
                }
            }
            if (changed) repository.save(row);
        }

        if (!matched && page.isEnabled() && page.isNavVisible()) {
            SiteMenuItem row = new SiteMenuItem();
            row.setMenuName(safeText(page.getPageName(), "页面"));
            row.setLinkType(LinkType.INTERNAL);
            row.setInternalPath(routePath);
            row.setVisible(true);
            row.setOpenInNewWindow(false);
            row.setSortOrder(page.getNavOrder());
            repository.save(row);
        }
    }

    public void syncPagesFromMenus() {
        List<SitePageConfig> pages = sitePageConfigService.findAll();
        if (pages.isEmpty()) return;

        List<SiteMenuItem> menus = repository.findAllByOrderBySortOrderAscIdAsc();
        for (SiteMenuItem m : menus) {
            if (m.getParentId() != null) continue;
            if (m.getLinkType() != LinkType.INTERNAL) continue;
            String menuPath = normalizePath(m.getInternalPath());
            if (menuPath == null || menuPath.isEmpty()) continue;

            for (SitePageConfig p : pages) {
                String pagePath = normalizePath(p.getRoutePath());
                if (!menuPath.equals(pagePath)) continue;
                boolean changed = false;
                String menuName = safeText(m.getMenuName(), p.getPageName());
                if (!menuName.equals(p.getPageName())) {
                    p.setPageName(menuName);
                    changed = true;
                }
                if (p.getNavOrder() != m.getSortOrder()) {
                    p.setNavOrder(m.getSortOrder());
                    changed = true;
                }
                if (p.isNavVisible() != m.isVisible()) {
                    p.setNavVisible(m.isVisible());
                    changed = true;
                }
                if (changed) sitePageConfigService.save(p);
                break;
            }
        }
    }

    public List<LinkType> allLinkTypes() {
        return Arrays.asList(LinkType.values());
    }

    private long safeParent(Long p) {
        return p == null ? 0 : p;
    }

    private int minSize(List<?> lists) {
        int min = Integer.MAX_VALUE;
        for (Object obj : lists) {
            if (!(obj instanceof List)) continue;
            List<?> list = (List<?>) obj;
            min = Math.min(min, list.size());
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private String text(List<String> list, int i, String def) {
        if (list == null || i < 0 || i >= list.size()) return def;
        String v = list.get(i);
        if (v == null || v.trim().isEmpty()) return def;
        return v.trim();
    }

    private Long parent(List<Long> list, int i) {
        if (list == null || i < 0 || i >= list.size()) return null;
        Long v = list.get(i);
        if (v == null || v <= 0) return null;
        return v;
    }

    private boolean bool(List<String> list, int i, boolean def) {
        if (list == null || i < 0 || i >= list.size()) return def;
        String v = list.get(i);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "on".equalsIgnoreCase(v);
    }

    private int number(List<Integer> list, int i, int def) {
        if (list == null || i < 0 || i >= list.size() || list.get(i) == null) return def;
        return list.get(i);
    }

    private LinkType parseLinkType(String value) {
        try {
            return LinkType.valueOf(value);
        } catch (Exception ex) {
            return LinkType.INTERNAL;
        }
    }

    private String normalizePath(String path) {
        if (path == null) return null;
        String p = path.trim();
        if (p.isEmpty()) return null;
        if (!p.startsWith("/")) p = "/" + p;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private String safeText(String value, String def) {
        if (value == null) return def == null ? "" : def.trim();
        String t = value.trim();
        if (t.isEmpty()) return def == null ? "" : def.trim();
        return t;
    }
}
