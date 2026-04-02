package com.biubiu.config;

import com.biubiu.entity.*;
import com.biubiu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LevelPriceRepository levelPriceRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
        initCustomerServiceUser();
        initLevelPrices();
        initSystemConfig();
    }

    private void initAdminUser() {
        Optional<User> existing = userRepository.findByPhone("18191102965");
        if (existing.isEmpty()) {
            User admin = new User();
            admin.setPhone("18191102965");
            admin.setPassword(passwordEncoder.encode("rr031108"));
            admin.setNickname("系统管理员");
            admin.setRole(User.Role.ADMIN);
            admin.setStatus(User.Status.active);
            userRepository.save(admin);
            System.out.println("初始化管理员账号: 18191102965 / rr031108");
        }
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
}
