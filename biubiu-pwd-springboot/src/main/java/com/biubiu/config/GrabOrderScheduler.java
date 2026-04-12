package com.biubiu.config;

import com.biubiu.entity.*;
import com.biubiu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrabOrderScheduler {

    private final OrderRepository orderRepository;
    private final GrabWaitingPlayerRepository waitingPlayerRepository;
    private final GrabCooldownRepository cooldownRepository;
    private final GrabPriorityWaitRepository priorityWaitRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final OrderSessionRepository orderSessionRepository;

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkLockTimeout() {
        List<Order> lockedOrders = orderRepository.findByGrabStatusAndGrabLockUntilBefore(
                Order.GrabStatus.LOCKED, LocalDateTime.now());

        for (Order order : lockedOrders) {
            User leader = order.getGrabLeader();
            if (leader != null) {
                SystemConfig config = systemConfigRepository.findFirstByOrderByIdAsc().orElse(null);
                int cooldownSeconds = config != null ? config.getGrabCooldownSeconds() : 60;

                GrabCooldown cooldown = new GrabCooldown();
                cooldown.setOrder(order);
                cooldown.setPlayer(leader);
                cooldown.setCooldownUntil(LocalDateTime.now().plusSeconds(cooldownSeconds));
                cooldownRepository.save(cooldown);
            }

            order.setGrabStatus(Order.GrabStatus.OPEN);
            order.setGrabLeader(null);
            order.setGrabPartner(null);
            order.setGrabLockUntil(null);
            orderRepository.save(order);
            log.info("固排锁超时，释放订单: {}", order.getOrderNo());
        }
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkPriorityWaitTimeout() {
        List<GrabPriorityWait> expiredWaits = priorityWaitRepository.findByWaitUntilBefore(LocalDateTime.now());
        expiredWaits.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        for (GrabPriorityWait wait : expiredWaits) {
            Order order = orderRepository.findByIdForUpdate(wait.getOrder().getId()).orElse(null);
            if (order == null || order.getGrabStatus() != Order.GrabStatus.OPEN) {
                priorityWaitRepository.delete(wait);
                continue;
            }

            order.setCurrentPlayer(wait.getPlayer());
            order.setStatus(Order.Status.PENDING_ACCEPT);
            order.setGrabStatus(Order.GrabStatus.ASSIGNED);
            order.setInGrabHall(false);
            order.setAssignedAt(LocalDateTime.now());
            orderRepository.save(order);

            priorityWaitRepository.deleteByOrderId(order.getId());
            waitingPlayerRepository.deleteByOrderId(order.getId());
            cooldownRepository.deleteByOrderId(order.getId());

            log.info("优先等待到期，自动分配订单: {} 给玩家: {}", order.getOrderNo(), wait.getPlayer().getNickname());
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanExpiredCooldowns() {
        cooldownRepository.deleteExpiredCooldowns(LocalDateTime.now());
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanInactiveWaiters() {
        List<GrabWaitingPlayer> allWaiters = waitingPlayerRepository.findAll();
        for (GrabWaitingPlayer wp : allWaiters) {
            if (wp.getPlayer().getStatus() == User.Status.disabled) {
                waitingPlayerRepository.delete(wp);
                List<GrabWaitingPlayer> remaining = waitingPlayerRepository.findByOrderId(wp.getOrder().getId());
                if (remaining.isEmpty()) {
                    Order order = orderRepository.findById(wp.getOrder().getId()).orElse(null);
                    if (order != null && order.getGrabStatus() == Order.GrabStatus.WAITING) {
                        order.setGrabStatus(Order.GrabStatus.OPEN);
                        orderRepository.save(order);
                    }
                }
            }
        }

        List<GrabPriorityWait> disabledPriorityWaits = priorityWaitRepository.findByPlayerStatus(User.Status.disabled);
        for (GrabPriorityWait pw : disabledPriorityWaits) {
            priorityWaitRepository.delete(pw);
        }
    }
}
