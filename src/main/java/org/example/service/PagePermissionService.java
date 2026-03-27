package org.example.service;

import org.example.model.AccessLevel;
import org.example.model.PagePermission;
import org.example.model.Role;
import org.example.repository.PagePermissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PagePermissionService {
    private final PagePermissionRepository pagePermissionRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<PageRule> DEFAULT_RULES = Collections.unmodifiableList(Arrays.asList(
        new PageRule("/", "首页", AccessLevel.PUBLIC),
        new PageRule("/about", "关于我们", AccessLevel.PUBLIC),
        new PageRule("/solutions", "解决方案", AccessLevel.PUBLIC),
        new PageRule("/cases", "案例展示", AccessLevel.PUBLIC),
        new PageRule("/news", "新闻动态", AccessLevel.PUBLIC),
        new PageRule("/stock-sim", "模拟股票", AccessLevel.PUBLIC),
        new PageRule("/contact", "联系我们", AccessLevel.PUBLIC),
        new PageRule("/articles/*", "文章详情页", AccessLevel.PUBLIC),
        new PageRule("/articles/new", "发布文章页", AccessLevel.EDITOR)
    ));

    public PagePermissionService(PagePermissionRepository pagePermissionRepository) {
        this.pagePermissionRepository = pagePermissionRepository;
    }

    public void ensureDefaults() {
        for (PageRule rule : DEFAULT_RULES) {
            Optional<PagePermission> existing = pagePermissionRepository.findByPathPattern(rule.getPathPattern());
            if (!existing.isPresent()) {
                PagePermission permission = new PagePermission();
                permission.setPathPattern(rule.getPathPattern());
                permission.setPageName(rule.getPageName());
                permission.setRequiredLevel(rule.getRequiredLevel());
                pagePermissionRepository.save(permission);
                continue;
            }
            PagePermission row = existing.get();
            boolean changed = false;
            if (looksMojibake(row.getPageName())) {
                row.setPageName(rule.getPageName());
                changed = true;
            }
            if (changed) {
                pagePermissionRepository.save(row);
            }
        }
    }

    public List<PagePermission> findManagedPages() {
        ensureDefaults();
        List<PagePermission> rows = pagePermissionRepository.findAll();
        final Map<String, Integer> orderMap = new HashMap<String, Integer>();
        for (int i = 0; i < DEFAULT_RULES.size(); i++) {
            orderMap.put(DEFAULT_RULES.get(i).getPathPattern(), i);
        }
        List<PagePermission> managed = new ArrayList<PagePermission>();
        for (PagePermission row : rows) {
            if (orderMap.containsKey(row.getPathPattern())) {
                managed.add(row);
            }
        }
        managed.sort((a, b) -> {
            Integer i1 = orderMap.get(a.getPathPattern());
            Integer i2 = orderMap.get(b.getPathPattern());
            return Integer.compare(i1 == null ? Integer.MAX_VALUE : i1, i2 == null ? Integer.MAX_VALUE : i2);
        });
        return managed;
    }

    public void updatePermission(Long id, AccessLevel requiredLevel) {
        PagePermission permission = pagePermissionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Page permission not found"));
        permission.setRequiredLevel(requiredLevel);
        pagePermissionRepository.save(permission);
    }

    public void updateByPathPattern(String pathPattern, AccessLevel requiredLevel) {
        ensureDefaults();
        PagePermission permission = pagePermissionRepository.findByPathPattern(pathPattern)
            .orElseThrow(() -> new IllegalArgumentException("Page permission not found"));
        permission.setRequiredLevel(requiredLevel);
        pagePermissionRepository.save(permission);
    }

    public void upsertByPathPattern(String pathPattern, String pageName, AccessLevel requiredLevel) {
        ensureDefaults();
        String normalized = normalizePath(pathPattern);
        if (requiredLevel == null) {
            requiredLevel = AccessLevel.PUBLIC;
        }
        PagePermission row = pagePermissionRepository.findByPathPattern(normalized).orElseGet(() -> {
            PagePermission n = new PagePermission();
            n.setPathPattern(normalized);
            return n;
        });
        String finalName = (pageName == null || pageName.trim().isEmpty()) ? normalized : pageName.trim();
        row.setPageName(finalName);
        row.setRequiredLevel(requiredLevel);
        pagePermissionRepository.save(row);
    }

    public Map<String, AccessLevel> levelByPath() {
        ensureDefaults();
        Map<String, AccessLevel> map = new HashMap<String, AccessLevel>();
        for (PagePermission row : pagePermissionRepository.findAll()) {
            map.put(row.getPathPattern(), row.getRequiredLevel());
        }
        return map;
    }

    public AccessLevel resolveRequiredLevel(String path) {
        ensureDefaults();
        String normalized = normalizePath(path);
        Map<String, AccessLevel> rules = new LinkedHashMap<String, AccessLevel>();
        for (PagePermission row : pagePermissionRepository.findAll()) {
            rules.put(row.getPathPattern(), row.getRequiredLevel());
        }
        AccessLevel matchedLevel = null;
        int matchedPatternLength = -1;
        for (Map.Entry<String, AccessLevel> entry : rules.entrySet()) {
            if (pathMatcher.match(entry.getKey(), normalized)) {
                int length = entry.getKey().length();
                if (length > matchedPatternLength) {
                    matchedPatternLength = length;
                    matchedLevel = entry.getValue();
                }
            }
        }
        return matchedLevel;
    }

    public boolean canAccess(String path, Authentication authentication) {
        AccessLevel requiredLevel = resolveRequiredLevel(path);
        if (requiredLevel == null || requiredLevel == AccessLevel.PUBLIC) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }
        boolean isUser = hasRole(authentication, Role.ROLE_USER.name())
            || hasRole(authentication, Role.ROLE_EDITOR.name())
            || hasRole(authentication, Role.ROLE_ADMIN.name());
        boolean isEditor = hasRole(authentication, Role.ROLE_EDITOR.name())
            || hasRole(authentication, Role.ROLE_ADMIN.name());
        boolean isAdmin = hasRole(authentication, Role.ROLE_ADMIN.name());
        switch (requiredLevel) {
            case USER:
                return isUser;
            case EDITOR:
                return isEditor;
            case ADMIN:
                return isAdmin;
            default:
                return true;
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        String value = path.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public List<AccessLevel> allLevels() {
        return Arrays.asList(AccessLevel.values());
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

    private static class PageRule {
        private final String pathPattern;
        private final String pageName;
        private final AccessLevel requiredLevel;

        private PageRule(String pathPattern, String pageName, AccessLevel requiredLevel) {
            this.pathPattern = pathPattern;
            this.pageName = pageName;
            this.requiredLevel = requiredLevel;
        }

        public String getPathPattern() {
            return pathPattern;
        }

        public String getPageName() {
            return pageName;
        }

        public AccessLevel getRequiredLevel() {
            return requiredLevel;
        }
    }
}
