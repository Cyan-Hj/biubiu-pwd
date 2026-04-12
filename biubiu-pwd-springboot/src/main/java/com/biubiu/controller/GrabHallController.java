package com.biubiu.controller;

import com.biubiu.dto.*;
import com.biubiu.entity.SystemConfig;
import com.biubiu.entity.User;
import com.biubiu.repository.SystemConfigRepository;
import com.biubiu.repository.UserRepository;
import com.biubiu.service.GrabOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/grab-hall")
@RequiredArgsConstructor
public class GrabHallController {

    private final GrabOrderService grabOrderService;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;

    private void checkGrabEnabled() {
        SystemConfig config = systemConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null || !Boolean.TRUE.equals(config.getGrabEnabled())) {
            throw new RuntimeException("抢单大厅已关闭");
        }
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('PLAYER', 'ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Page<GrabHallOrderResponse>> getGrabHallOrders(
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String playerCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkGrabEnabled();
        User currentUser = getCurrentUser();
        Page<GrabHallOrderResponse> result = grabOrderService.getGrabHallOrders(orderType, playerCount, page, size, currentUser);
        return ApiResponse.success(result);
    }

    @PostMapping("/orders/{orderId}/grab")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<GrabResult> grabOrder(@PathVariable Long orderId, @RequestBody GrabRequest request) {
        checkGrabEnabled();
        User currentUser = getCurrentUser();
        GrabResult result = grabOrderService.grabOrder(orderId, request, currentUser);
        return ApiResponse.success("操作成功", result);
    }

    @PostMapping("/orders/{orderId}/join-team")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<GrabResult> joinTeam(@PathVariable Long orderId) {
        checkGrabEnabled();
        User currentUser = getCurrentUser();
        GrabResult result = grabOrderService.joinTeam(orderId, currentUser);
        return ApiResponse.success("加入成功", result);
    }

    @PostMapping("/orders/{orderId}/cancel-grab")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> cancelGrab(@PathVariable Long orderId) {
        checkGrabEnabled();
        User currentUser = getCurrentUser();
        grabOrderService.cancelGrab(orderId, currentUser);
        return ApiResponse.success("已取消", null);
    }

    @GetMapping("/my-status")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<MyGrabStatusResponse> getMyGrabStatus() {
        checkGrabEnabled();
        User currentUser = getCurrentUser();
        MyGrabStatusResponse result = grabOrderService.getMyGrabStatus(currentUser);
        return ApiResponse.success(result);
    }

    @GetMapping("/search-players")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<Map<String, Object>>> searchPlayers(@RequestParam String keyword) {
        User currentUser = getCurrentUser();
        Page<User> page = userRepository.findPlayers(
                User.Status.active, null, keyword, PageRequest.of(0, 10));
        List<Map<String, Object>> result = page.getContent().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("nickname", u.getNickname());
                    map.put("phone", u.getPhone());
                    map.put("level", u.getLevel());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
