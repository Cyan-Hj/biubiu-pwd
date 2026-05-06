package com.biubiu.controller;

import com.biubiu.dto.ApiResponse;
import com.biubiu.dto.LevelUpgradeApplyRequest;
import com.biubiu.dto.LevelUpgradeResponse;
import com.biubiu.dto.LevelUpgradeReviewRequest;
import com.biubiu.entity.LevelUpgradeApplication;
import com.biubiu.entity.LevelPrice;
import com.biubiu.entity.User;
import com.biubiu.repository.LevelPriceRepository;
import com.biubiu.repository.LevelUpgradeApplicationRepository;
import com.biubiu.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/level-upgrade")
@RequiredArgsConstructor
public class LevelUpgradeController {

    private final LevelUpgradeApplicationRepository applicationRepository;
    private final LevelPriceRepository levelPriceRepository;
    private final UserRepository userRepository;

    @PostMapping("/apply")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> apply(@Valid @RequestBody LevelUpgradeApplyRequest request) {
        User currentUser = getCurrentUser();

        if (applicationRepository.existsByPlayerIdAndStatus(currentUser.getId(), LevelUpgradeApplication.ApplicationStatus.PENDING)) {
            throw new RuntimeException("您已有待审核的升级申请，请等待审批");
        }

        if (request.getRequestedLevel().equals(currentUser.getLevel())) {
            throw new RuntimeException("申请等级与当前等级相同");
        }

        LevelUpgradeApplication app = LevelUpgradeApplication.builder()
                .player(currentUser)
                .currentLevel(currentUser.getLevel())
                .requestedLevel(request.getRequestedLevel())
                .reason(request.getReason())
                .status(LevelUpgradeApplication.ApplicationStatus.PENDING)
                .build();

        applicationRepository.save(app);
        return ApiResponse.success("申请已提交", null);
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<LevelUpgradeResponse>> getMyApplications() {
        User currentUser = getCurrentUser();
        List<LevelUpgradeApplication> apps = applicationRepository.findByPlayerIdOrderByCreatedAtDesc(currentUser.getId());
        return ApiResponse.success(apps.stream().map(this::convertToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LevelUpgradeResponse>> getPendingApplications() {
        List<LevelUpgradeApplication> apps = applicationRepository.findPendingApplications();
        return ApiResponse.success(apps.stream().map(this::convertToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LevelUpgradeResponse>> getAllApplications() {
        List<LevelUpgradeApplication> apps = applicationRepository.findAllByOrderByCreatedAtDesc();
        return ApiResponse.success(apps.stream().map(this::convertToResponse).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestBody(required = false) LevelUpgradeReviewRequest request) {
        User currentUser = getCurrentUser();
        LevelUpgradeApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (app.getStatus() != LevelUpgradeApplication.ApplicationStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }

        User player = app.getPlayer();
        player.setLevel(app.getRequestedLevel());

        levelPriceRepository.findByLevel(app.getRequestedLevel()).ifPresent(lp -> {
            player.setPricePerHour(lp.getDefaultPrice());
        });

        userRepository.save(player);

        app.setStatus(LevelUpgradeApplication.ApplicationStatus.APPROVED);
        app.setReviewer(currentUser);
        app.setReviewedAt(java.time.LocalDateTime.now());
        if (request != null && request.getReviewComment() != null) {
            app.setReviewComment(request.getReviewComment());
        }
        applicationRepository.save(app);

        return ApiResponse.success("已批准", null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody(required = false) LevelUpgradeReviewRequest request) {
        User currentUser = getCurrentUser();
        LevelUpgradeApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (app.getStatus() != LevelUpgradeApplication.ApplicationStatus.PENDING) {
            throw new RuntimeException("该申请已处理");
        }

        app.setStatus(LevelUpgradeApplication.ApplicationStatus.REJECTED);
        app.setReviewer(currentUser);
        app.setReviewedAt(java.time.LocalDateTime.now());
        if (request != null && request.getReviewComment() != null) {
            app.setReviewComment(request.getReviewComment());
        }
        applicationRepository.save(app);

        return ApiResponse.success("已拒绝", null);
    }

    @PostMapping("/batch-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> batchApprove(@RequestBody List<Long> ids) {
        User currentUser = getCurrentUser();

        for (Long id : ids) {
            try {
                LevelUpgradeApplication app = applicationRepository.findById(id).orElse(null);
                if (app == null || app.getStatus() != LevelUpgradeApplication.ApplicationStatus.PENDING) continue;

                User player = app.getPlayer();
                player.setLevel(app.getRequestedLevel());
                levelPriceRepository.findByLevel(app.getRequestedLevel()).ifPresent(lp -> {
                    player.setPricePerHour(lp.getDefaultPrice());
                });
                userRepository.save(player);

                app.setStatus(LevelUpgradeApplication.ApplicationStatus.APPROVED);
                app.setReviewer(currentUser);
                app.setReviewedAt(java.time.LocalDateTime.now());
                applicationRepository.save(app);
            } catch (Exception ignored) {
            }
        }

        return ApiResponse.success("批量审批完成", null);
    }

    private LevelUpgradeResponse convertToResponse(LevelUpgradeApplication app) {
        java.math.BigDecimal requestedPrice = null;
        try {
            requestedPrice = levelPriceRepository.findByLevel(app.getRequestedLevel())
                    .map(LevelPrice::getDefaultPrice).orElse(null);
        } catch (Exception ignored) {
        }

        return LevelUpgradeResponse.builder()
                .id(app.getId())
                .playerId(app.getPlayer().getId())
                .playerNickname(app.getPlayer().getNickname())
                .playerNo(app.getPlayer().getPlayerNo())
                .currentLevel(app.getCurrentLevel())
                .requestedLevel(app.getRequestedLevel())
                .requestedLevelPrice(requestedPrice)
                .status(app.getStatus().name())
                .reason(app.getReason())
                .reviewerNickname(app.getReviewer() != null ? app.getReviewer().getNickname() : null)
                .reviewComment(app.getReviewComment())
                .createdAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
