package com.biubiu.controller;

import com.biubiu.dto.*;
import com.biubiu.entity.User;
import com.biubiu.repository.UserRepository;
import com.biubiu.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
        );

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getStatus() == User.Status.pending) {
            throw new RuntimeException("账号待审核，请联系管理员");
        }

        if (user.getStatus() == User.Status.disabled) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());

        String token = jwtService.generateToken(claims, (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .level(user.getLevel())
                .pricePerHour(user.getPricePerHour())
                .status(user.getStatus())
                .totalIncome(user.getTotalIncome())
                .availableBalance(user.getAvailableBalance())
                .createdAt(user.getCreatedAt())
                .token(token)
                .build();

        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("该手机号已注册");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.player);
        user.setStatus(User.Status.pending);

        userRepository.save(user);

        return ApiResponse.success("注册成功，请等待管理员审核", null);
    }
}
