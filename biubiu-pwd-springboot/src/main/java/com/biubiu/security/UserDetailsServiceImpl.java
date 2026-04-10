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

        // 检查账号是否被禁用
        boolean enabled = user.getEnabled() != null ? user.getEnabled() : true;
        boolean accountNonLocked = enabled;

        return new org.springframework.security.core.userdetails.User(
                user.getPhone(),
                user.getPassword(),
                user.getStatus() == User.Status.active && enabled,
                true,
                true,
                accountNonLocked,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase()))
        );
    }
}
