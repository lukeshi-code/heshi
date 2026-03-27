package org.example.config;

import org.example.model.Role;
import org.example.model.UserAccount;
import org.example.repository.UserAccountRepository;
import org.example.service.HomeModuleConfigService;
import org.example.service.PagePermissionService;
import org.example.service.SitePageConfigService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PagePermissionService pagePermissionService;
    private final SitePageConfigService sitePageConfigService;
    private final HomeModuleConfigService homeModuleConfigService;

    public DataInitializer(UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder,
                           PagePermissionService pagePermissionService,
                           SitePageConfigService sitePageConfigService,
                           HomeModuleConfigService homeModuleConfigService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.pagePermissionService = pagePermissionService;
        this.sitePageConfigService = sitePageConfigService;
        this.homeModuleConfigService = homeModuleConfigService;
    }

    @Override
    public void run(String... args) {
        if (pagePermissionService != null) {
            pagePermissionService.ensureDefaults();
        }
        if (sitePageConfigService != null) {
            sitePageConfigService.ensureDefaults();
        }
        if (homeModuleConfigService != null) {
            homeModuleConfigService.ensureDefaults();
        }
        if (userAccountRepository.existsByUsername("admin")) {
            return;
        }
        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setDisplayName("Administrator");
        admin.setEmail("admin@heshicapital.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRoles(new HashSet<Role>(Arrays.asList(Role.ROLE_ADMIN, Role.ROLE_EDITOR, Role.ROLE_USER)));
        userAccountRepository.save(admin);
    }
}
