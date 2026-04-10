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
                .playerNo(user.getPlayerNo())
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
                    .playerNo(user.getPlayerNo())
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

        if (user.getPlayerNo() == null || user.getPlayerNo().isEmpty()) {
            Integer maxNo = userRepository.findMaxPlayerNo();
            int nextNo = (maxNo != null ? maxNo : 0) + 1;
            user.setPlayerNo(String.format("P-%04d", nextNo));
        }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deletePlayer(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否有进行中的订单
        long activeOrders = orderRepository.countByCurrentPlayerIdAndStatus(user.getId(), Order.Status.IN_SERVICE);
        if (activeOrders > 0) {
            throw new RuntimeException("该陪玩师有进行中的订单，无法删除");
        }

        userRepository.delete(user);
        return ApiResponse.success("删除成功", null);
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

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile() {
        User user = getCurrentUserEntity();
        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .playerNo(user.getPlayerNo())
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

    @PutMapping("/nickname")
    public ApiResponse<Void> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        User user = getCurrentUserEntity();
        user.setNickname(request.getNickname());
        userRepository.save(user);
        return ApiResponse.success("昵称修改成功", null);
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.success("密码修改成功", null);
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // ==================== 客服账号管理接口 ====================

    @GetMapping("/customer-service")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CustomerServiceResponse>> getCustomerServiceList() {
        List<User> csList = userRepository.findByRoleOrderByCreatedAtDesc(User.Role.CUSTOMER_SERVICE);
        List<CustomerServiceResponse> response = csList.stream()
                .map(user -> CustomerServiceResponse.builder()
                        .id(user.getId())
                        .phone(user.getPhone())
                        .nickname(user.getNickname())
                        .enabled(user.getEnabled())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @PostMapping("/customer-service")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> createCustomerService(@Valid @RequestBody CreateCustomerServiceRequest request) {
        // 检查手机号是否已存在
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }

        User cs = new User();
        cs.setPhone(request.getPhone());
        cs.setNickname(request.getNickname());
        cs.setPassword(passwordEncoder.encode(request.getPassword()));
        cs.setRole(User.Role.CUSTOMER_SERVICE);
        cs.setStatus(User.Status.active);
        cs.setEnabled(true);

        userRepository.save(cs);
        return ApiResponse.success("客服账号创建成功", null);
    }

    @PutMapping("/customer-service/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateCustomerService(@PathVariable Long id, @Valid @RequestBody UpdateCustomerServiceRequest request) {
        User cs = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("客服账号不存在"));

        if (cs.getRole() != User.Role.CUSTOMER_SERVICE) {
            throw new RuntimeException("该用户不是客服账号");
        }

        if (request.getEnabled() != null) {
            cs.setEnabled(request.getEnabled());
        }

        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            cs.setNickname(request.getNickname());
        }

        userRepository.save(cs);
        return ApiResponse.success("更新成功", null);
    }

    @PostMapping("/customer-service/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetCustomerServicePassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        User cs = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("客服账号不存在"));

        if (cs.getRole() != User.Role.CUSTOMER_SERVICE) {
            throw new RuntimeException("该用户不是客服账号");
        }

        cs.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(cs);
        return ApiResponse.success("密码重置成功", null);
    }

    @DeleteMapping("/customer-service/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCustomerService(@PathVariable Long id) {
        User cs = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("客服账号不存在"));

        if (cs.getRole() != User.Role.CUSTOMER_SERVICE) {
            throw new RuntimeException("该用户不是客服账号");
        }

        userRepository.delete(cs);
        return ApiResponse.success("客服账号已删除", null);
    }
}
