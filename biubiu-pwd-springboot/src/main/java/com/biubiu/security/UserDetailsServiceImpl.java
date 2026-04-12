package com.biubiu.security;

import com.biubiu.entity.User;
import com.biubiu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("手机号或密码错误"));

        boolean accountEnabled;
        if (user.getRole() == User.Role.ADMIN) {
            accountEnabled = true;
        } else {
            boolean isEnabled = user.getEnabled() != null ? user.getEnabled() : true;
            accountEnabled = isEnabled && user.getStatus() == User.Status.active;
        }

        return new org.springframework.security.core.userdetails.User(
                user.getPhone(),
                user.getPassword(),
                accountEnabled,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase()))
        );
    }
}
