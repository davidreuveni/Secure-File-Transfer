package com.davidr.secureft.services;

import com.davidr.secureft.datamodels.User;
import com.davidr.secureft.datamodels.UserRole;
import com.davidr.secureft.repositories.UserRepo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    public CustomUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        UserRole role = user.getRole() == null ? UserRole.ROLE_USER : user.getRole();

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getHashedPassword())
                .authorities(List.of(new SimpleGrantedAuthority(role.name())))
                .build();
    }
}
