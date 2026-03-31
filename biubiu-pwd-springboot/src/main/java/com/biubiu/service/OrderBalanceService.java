package com.biubiu.service;

import com.biubiu.entity.Boss;
import com.biubiu.entity.BossRechargeRecord;
import com.biubiu.entity.Order;
import com.biubiu.repository.BossRechargeRecordRepository;
import com.biubiu.repository.BossRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderBalanceService {

    private final BossRepository bossRepository;
    private final BossRechargeRecordRepository rechargeRecordRepository;

    /**
     * 扣减余额 - 余额不足时返回差额，不抛异常
     * @return 差额（如果余额充足返回0，余额不足返回需要补的差价）
     */
    @Transactional
    public BigDecimal deductBalance(Order order, Boss boss, BigDecimal amount) {
        BigDecimal balance = boss.getBalance();
        BigDecimal deductAmount;
        BigDecimal shortfall = BigDecimal.ZERO;

        if (balance.compareTo(amount) >= 0) {
            // 余额充足，全额扣减
            deductAmount = amount;
        } else {
            // 余额不足，扣减全部余额，记录差额
            deductAmount = balance;
            shortfall = amount.subtract(balance);
        }

        if (deductAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 扣减余额
            boss.setBalance(balance.subtract(deductAmount));
            boss.setTotalConsumption(boss.getTotalConsumption().add(deductAmount));
            bossRepository.save(boss);

            // 记录扣减
            BossRechargeRecord record = new BossRechargeRecord();
            record.setBossId(boss.getId());
            record.setAmount(deductAmount.negate());
            record.setType(BossRechargeRecord.Type.DEDUCT);
            record.setOrderNo(order.getOrderNo());
            record.setRemark("订单消费" + (shortfall.compareTo(BigDecimal.ZERO) > 0 ? "，余额不足需补差价¥" + shortfall : ""));
            record.setOperatorId(order.getCreatedBy().getId());
            rechargeRecordRepository.save(record);
        }

        // 更新订单
        order.setBalanceDeducted(deductAmount);

        return shortfall;
    }
}
