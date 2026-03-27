package org.example.config;

import org.example.service.PagePermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class PageAccessInterceptor implements HandlerInterceptor {
    private final PagePermissionService pagePermissionService;

    public PageAccessInterceptor(PagePermissionService pagePermissionService) {
        this.pagePermissionService = pagePermissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/css/") || path.startsWith("/uploads/") || path.startsWith("/h2-console")) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (pagePermissionService.canAccess(path, authentication)) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            response.sendRedirect("/login");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        return false;
    }
}
