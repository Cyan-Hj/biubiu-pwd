package com.biubiu.controller;

import com.biubiu.dto.*;
import com.biubiu.entity.Order;
import com.biubiu.entity.User;
import com.biubiu.repository.OrderRepository;
import com.biubiu.repository.UserRepository;
import com.biubiu.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        User user = getCurrentUserEntity();

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
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/players")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<PageResponse<PlayerResponse>> getPlayers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        User.Status statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = User.Status.valueOf(status);
        }

        String levelParam = null;
        if (level != null && !level.isEmpty()) {
            levelParam = level;
        }

        Page<User> playerPage = userRepository.findPlayers(
                statusEnum,
                levelParam,
                search,
                PageRequest.of(page - 1, pageSize)
        );

        List<PlayerResponse> list = playerPage.getContent().stream().map(user -> {
            long activeOrders = orderRepository.countByCurrentPlayerIdAndStatus(user.getId(), Order.Status.IN_SERVICE);
            return PlayerResponse.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .phone(user.getPhone())
                    .level(user.getLevel())
                    .pricePerHour(user.getPricePerHour())
                    .status(user.getStatus())
                    .totalIncome(user.getTotalIncome())
                    .availableBalance(user.getAvailableBalance())
                    .activeOrdersCount(activeOrders)
                    .build();
        }).collect(Collectors.toList());

        PageResponse<PlayerResponse> response = PageResponse.<PlayerResponse>builder()
                .list(list)
                .total(playerPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approvePlayer(@PathVariable Long id, @Valid @RequestBody ApprovePlayerRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setStatus(User.Status.active);
        user.setLevel(request.getLevel());
        user.setPricePerHour(request.getPricePerHour());

        userRepository.save(user);

        return ApiResponse.success("审核通过", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updatePlayer(@PathVariable Long id, @Valid @RequestBody UpdatePlayerRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getLevel() != null) {
            user.setLevel(request.getLevel());
        }
        if (request.getPricePerHour() != null) {
            user.setPricePerHour(request.getPricePerHour());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userRepository.save(user);

        return ApiResponse.success("更新成功", null);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return ApiResponse.success("密码重置成功", null);
    }

    @GetMapping("/players/levels")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<List<String>> getPlayerLevels() {
        List<User> players = userRepository.findActivePlayers();
        List<String> levels = players.stream()
                .map(User::getLevel)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ApiResponse.success(levels);
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
