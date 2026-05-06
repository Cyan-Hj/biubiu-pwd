package com.biubiu.service;

import com.biubiu.entity.*;
import com.biubiu.repository.FinancialRecordRepository;
import com.biubiu.repository.OperationLogRepository;
import com.biubiu.repository.OrderSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletedOrderBackupService {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    private final OrderSessionRepository orderSessionRepository;
    private final OperationLogRepository operationLogRepository;
    private final FinancialRecordRepository financialRecordRepository;

    private static final String BACKUP_DIR = "deleted_orders_backup";
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public void backupOrder(Order order) {
        try {
            DeletedOrderBackup backup = buildBackup(order);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backup);

            Path backupDir = Paths.get(uploadPath).resolve(BACKUP_DIR);
            Files.createDirectories(backupDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = order.getOrderNo() + "_" + timestamp + ".json";
            Path filePath = backupDir.resolve(filename);

            Files.writeString(filePath, json);
            log.info("已备份删除订单数据到: {}", filePath);
        } catch (IOException e) {
            log.error("备份订单数据失败, orderNo={}: {}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    private DeletedOrderBackup buildBackup(Order order) {
        DeletedOrderBackup backup = new DeletedOrderBackup();
        backup.setOrderNo(order.getOrderNo());
        backup.setBossInfo(order.getBossInfo());
        backup.setServiceContent(order.getServiceContent());
        backup.setServiceHours(order.getServiceHours());
        backup.setPricePerHour(order.getPricePerHour());
        backup.setTotalAmount(order.getTotalAmount());
        backup.setScheduledTime(order.getScheduledTime());
        backup.setRemark(order.getRemark());
        backup.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        backup.setOrderType(order.getOrderType());
        backup.setPlayerCount(order.getPlayerCount() != null ? order.getPlayerCount().name() : null);
        backup.setCustomerType(order.getCustomerType() != null ? order.getCustomerType().name() : null);
        backup.setOriginalAmount(order.getOriginalAmount());
        backup.setDiscountRate(order.getDiscountRate());
        backup.setUseBalance(order.getUseBalance());
        backup.setBalanceDeducted(order.getBalanceDeducted());
        backup.setActualHours(order.getActualHours());
        backup.setCancelReason(order.getCancelReason());
        backup.setCreatedAt(order.getCreatedAt());
        backup.setAssignedAt(order.getAssignedAt());
        backup.setStartedAt(order.getStartedAt());
        backup.setCompletedAt(order.getCompletedAt());
        backup.setCancelledAt(order.getCancelledAt());
        backup.setPausedAt(order.getPausedAt());
        backup.setResumedAt(order.getResumedAt());
        backup.setDeletedAt(LocalDateTime.now());

        if (order.getCurrentPlayer() != null) {
            backup.setCurrentPlayerId(order.getCurrentPlayer().getId());
            backup.setCurrentPlayerNickname(order.getCurrentPlayer().getNickname());
        }
        if (order.getCurrentPlayer2() != null) {
            backup.setCurrentPlayer2Id(order.getCurrentPlayer2().getId());
            backup.setCurrentPlayer2Nickname(order.getCurrentPlayer2().getNickname());
        }
        if (order.getCreatedBy() != null) {
            backup.setCreatedById(order.getCreatedBy().getId());
            backup.setCreatedByNickname(order.getCreatedBy().getNickname());
        }
        if (order.getAssignedBy() != null) {
            backup.setAssignedById(order.getAssignedBy().getId());
            backup.setAssignedByNickname(order.getAssignedBy().getNickname());
        }
        if (order.getBoss() != null) {
            backup.setBossId(order.getBoss().getId());
            backup.setBossName(order.getBoss().getName());
        }

        List<OrderSession> sessions = orderSessionRepository.findByOrderId(order.getId());
        backup.setSessions(sessions.stream().map(s -> {
            DeletedOrderBackup.SessionInfo si = new DeletedOrderBackup.SessionInfo();
            si.setPlayerId(s.getPlayer() != null ? s.getPlayer().getId() : null);
            si.setPlayerNickname(s.getPlayer() != null ? s.getPlayer().getNickname() : null);
            si.setStartedAt(s.getStartedAt());
            si.setEndedAt(s.getEndedAt());
            si.setActualHours(s.getActualHours());
            return si;
        }).collect(Collectors.toList()));

        List<OperationLog> logs = operationLogRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        backup.setOperationLogs(logs.stream().map(l -> {
            DeletedOrderBackup.OperationLogInfo oli = new DeletedOrderBackup.OperationLogInfo();
            oli.setOperatorNickname(l.getOperator() != null ? l.getOperator().getNickname() : null);
            oli.setOperationType(l.getOperationType());
            oli.setOldStatus(l.getOldStatus() != null ? l.getOldStatus().name() : null);
            oli.setNewStatus(l.getNewStatus() != null ? l.getNewStatus().name() : null);
            oli.setDetails(l.getDetails());
            oli.setCreatedAt(l.getCreatedAt());
            return oli;
        }).collect(Collectors.toList()));

        List<FinancialRecord> records = financialRecordRepository.findByOrderId(order.getId());
        backup.setFinancialRecords(records.stream().map(r -> {
            DeletedOrderBackup.FinancialRecordInfo fri = new DeletedOrderBackup.FinancialRecordInfo();
            fri.setPlayerId(r.getPlayer() != null ? r.getPlayer().getId() : null);
            fri.setPlayerNickname(r.getPlayer() != null ? r.getPlayer().getNickname() : null);
            fri.setType(r.getRecordType() != null ? r.getRecordType().name() : null);
            fri.setAmount(r.getAmount());
            fri.setDescription(r.getDescription());
            return fri;
        }).collect(Collectors.toList()));

        return backup;
    }

    public List<DeletedOrderBackup> listBackups() {
        try {
            Path backupDir = Paths.get(uploadPath).resolve(BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                return List.of();
            }
            return Files.list(backupDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .map(p -> {
                        try {
                            return objectMapper.readValue(p.toFile(), DeletedOrderBackup.class);
                        } catch (IOException e) {
                            log.warn("读取备份文件失败: {}", p, e);
                            return null;
                        }
                    })
                    .filter(b -> b != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出备份文件失败", e);
            return List.of();
        }
    }

    public DeletedOrderBackup getBackup(String orderNo) {
        try {
            Path backupDir = Paths.get(uploadPath).resolve(BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                return null;
            }
            return Files.list(backupDir)
                    .filter(p -> p.getFileName().toString().startsWith(orderNo + "_"))
                    .findFirst()
                    .map(p -> {
                        try {
                            return objectMapper.readValue(p.toFile(), DeletedOrderBackup.class);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    @Data
    public static class DeletedOrderBackup {
        private String orderNo;
        private String bossInfo;
        private String serviceContent;
        private BigDecimal serviceHours;
        private BigDecimal pricePerHour;
        private BigDecimal totalAmount;
        private LocalDateTime scheduledTime;
        private String remark;
        private String status;
        private String orderType;
        private String playerCount;
        private String customerType;
        private BigDecimal originalAmount;
        private BigDecimal discountRate;
        private Boolean useBalance;
        private BigDecimal balanceDeducted;
        private BigDecimal actualHours;
        private String cancelReason;

        private Long currentPlayerId;
        private String currentPlayerNickname;
        private Long currentPlayer2Id;
        private String currentPlayer2Nickname;
        private Long createdById;
        private String createdByNickname;
        private Long assignedById;
        private String assignedByNickname;
        private Long bossId;
        private String bossName;

        private LocalDateTime createdAt;
        private LocalDateTime assignedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime pausedAt;
        private LocalDateTime resumedAt;
        private LocalDateTime deletedAt;

        private List<SessionInfo> sessions;
        private List<OperationLogInfo> operationLogs;
        private List<FinancialRecordInfo> financialRecords;

        @Data
        public static class SessionInfo {
            private Long playerId;
            private String playerNickname;
            private LocalDateTime startedAt;
            private LocalDateTime endedAt;
            private BigDecimal actualHours;
        }

        @Data
        public static class OperationLogInfo {
            private String operatorNickname;
            private String operationType;
            private String oldStatus;
            private String newStatus;
            private String details;
            private LocalDateTime createdAt;
        }

        @Data
        public static class FinancialRecordInfo {
            private Long playerId;
            private String playerNickname;
            private String type;
            private BigDecimal amount;
            private String description;
        }
    }
}
