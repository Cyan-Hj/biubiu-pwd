package com.biubiu.config;

import com.biubiu.entity.*;
import com.biubiu.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LevelPriceRepository levelPriceRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    public void run(String... args) {
        initAdminUser();
        initCustomerServiceUser();
        initLevelPrices();
        initSystemConfig();
        migrateGrabConfigMinutesToSeconds();
        recoverGrabStatus();
    }

    private void initAdminUser() {
        // 如果数据库中已存在任何管理员账号，则不再创建
        if (userRepository.existsByRole(User.Role.ADMIN)) {
            return;
        }

        // 只有在没有管理员时才创建默认管理员
        User admin = new User();
        admin.setPhone("13800000000");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("系统管理员");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.Status.active);
        userRepository.save(admin);
        System.out.println("初始化管理员账号: 13800000000 / admin123");
    }

    private void initCustomerServiceUser() {
        Optional<User> existing = userRepository.findByPhone("13800000001");
        if (existing.isEmpty()) {
            User service = new User();
            service.setPhone("13800000001");
            service.setPassword(passwordEncoder.encode("service123"));
            service.setNickname("客服01");
            service.setRole(User.Role.CUSTOMER_SERVICE);
            service.setStatus(User.Status.active);
            userRepository.save(service);
            System.out.println("初始化客服账号: 13800000001 / service123");
        }
    }

    private void initLevelPrices() {
        // 只有当数据库中没有任何等级配置时，才初始化默认等级
        long count = levelPriceRepository.count();
        if (count > 0) {
            return; // 已有等级配置，跳过初始化
        }

        String[] levels = {"青铜", "白银", "黄金", "王牌"};
        BigDecimal[] prices = {BigDecimal.valueOf(40), BigDecimal.valueOf(50), BigDecimal.valueOf(60), BigDecimal.valueOf(80)};

        for (int i = 0; i < levels.length; i++) {
            final String level = levels[i];
            final BigDecimal price = prices[i];
            LevelPrice lp = new LevelPrice();
            lp.setLevel(level);
            lp.setDefaultPrice(price);
            lp.setSortOrder(i);
            levelPriceRepository.save(lp);
        }
        System.out.println("初始化默认等级配置: 青铜、白银、黄金、王牌");
    }

    private void initSystemConfig() {
        Optional<SystemConfig> existing = systemConfigRepository.findFirstByOrderByIdAsc();
        if (existing.isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setPlatformFeeRate(BigDecimal.valueOf(0.2));
            systemConfigRepository.save(config);
        }
    }

    @Transactional
    public void migrateGrabConfigMinutesToSeconds() {
        try {
            var nativeQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_config' " +
                "AND COLUMN_NAME = 'grab_team_lock_minutes'");
            long count = ((Number) nativeQuery.getSingleResult()).longValue();
            if (count > 0) {
                entityManager.createNativeQuery(
                    "UPDATE system_config SET " +
                    "grab_team_lock_seconds = grab_team_lock_minutes * 60, " +
                    "grab_cooldown_seconds = grab_cooldown_minutes * 60, " +
                    "grab_priority_wait_seconds = grab_priority_wait_minutes * 60 " +
                    "WHERE grab_team_lock_seconds = 60 AND grab_cooldown_seconds = 60 AND grab_priority_wait_seconds = 300"
                ).executeUpdate();
                entityManager.createNativeQuery(
                    "ALTER TABLE system_config " +
                    "DROP COLUMN grab_team_lock_minutes, " +
                    "DROP COLUMN grab_cooldown_minutes, " +
                    "DROP COLUMN grab_priority_wait_minutes"
                ).executeUpdate();
                System.out.println("迁移抢单配置：分钟→秒 完成");
            }
        } catch (Exception e) {
            System.out.println("迁移抢单配置跳过: " + e.getMessage());
        }
    }

    @Transactional
    public void recoverGrabStatus() {
        List<Order> expiredLocks = orderRepository.findByGrabStatusAndGrabLockUntilBefore(
                Order.GrabStatus.LOCKED, LocalDateTime.now());
        for (Order order : expiredLocks) {
            order.setGrabStatus(Order.GrabStatus.OPEN);
            order.setGrabLeader(null);
            order.setGrabPartner(null);
            order.setGrabLockUntil(null);
            orderRepository.save(order);
        }
        if (!expiredLocks.isEmpty()) {
            System.out.println("恢复抢单状态：释放 " + expiredLocks.size() + " 个过期锁定订单");
        }
    }
}
