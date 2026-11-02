package com.velsis.monitoring.security.service;

import com.velsis.monitoring.security.persistence.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var user = users.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));
        return User.withUsername(user.getUsuario())
                .password(user.getSenha())
                .roles("USER")
                .build();
    }
}
