-- 删除未完成订单的SQL脚本
-- 未完成订单指：状态不是 4(已完成) 和 5(已取消) 的订单
-- 执行顺序：先删除子表数据，再删除主表数据

-- 设置数据库
USE biubiu_pwd;

-- 开始事务
START TRANSACTION;

-- 1. 先删除 order_sessions 表中关联的未完成订单的会话记录
DELETE os FROM order_sessions os
INNER JOIN orders o ON os.order_id = o.id
WHERE o.status NOT IN (4, 5);

-- 2. 删除 financial_records 表中关联的未完成订单的财务记录
DELETE fr FROM financial_records fr
INNER JOIN orders o ON fr.order_id = o.id
WHERE o.status NOT IN (4, 5);

-- 3. 删除 operation_logs 表中关联的未完成订单的操作日志
DELETE ol FROM operation_logs ol
INNER JOIN orders o ON ol.order_id = o.id
WHERE o.status NOT IN (4, 5);

-- 4. 最后删除 orders 表中的未完成订单
DELETE FROM orders
WHERE status NOT IN (4, 5);

-- 提交事务
COMMIT;

-- 显示删除后的订单统计
SELECT 
    CASE status
        WHEN 0 THEN '待分配'
        WHEN 1 THEN '待接单'
        WHEN 2 THEN '待接单2'
        WHEN 3 THEN '服务中'
        WHEN 4 THEN '已完成'
        WHEN 5 THEN '已取消'
        ELSE '未知'
    END as status_name,
    status,
    COUNT(*) as count
FROM orders
GROUP BY status;
