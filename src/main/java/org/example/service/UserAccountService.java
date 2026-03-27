package org.example.service;

import org.example.model.Role;
import org.example.model.UserAccount;
import org.example.repository.UserAccountRepository;
import org.example.web.RegisterForm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAccountService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(RegisterForm form) {
        if (userAccountRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userAccountRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        UserAccount user = new UserAccount();
        user.setUsername(form.getUsername());
        user.setDisplayName(form.getDisplayName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        return userAccountRepository.save(user);
    }

    public UserAccount getByUsername(String username) {
        return userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public List<UserAccount> findAllUsers() {
        return userAccountRepository.findAll();
    }

    public void updateRoles(Long userId, Set<Role> roles) {
        UserAccount user = userAccountRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setRoles(roles);
        userAccountRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = getByUsername(username);
        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(Role::name)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
        return new User(user.getUsername(), user.getPassword(), authorities);
    }
}
