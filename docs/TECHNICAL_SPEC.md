# 三角洲陪玩管理系统 - 技术规格文档

> **版本**: 1.0.0  
> **更新日期**: 2024年  
> **项目名称**: biubiu-pwd

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈详情](#2-技术栈详情)
3. [系统架构](#3-系统架构)
4. [数据库设计](#4-数据库设计)
5. [API接口文档](#5-api接口文档)
6. [用户角色与权限](#6-用户角色与权限)
7. [业务流程说明](#7-业务流程说明)
8. [前端页面结构](#8-前端页面结构)
9. [部署说明](#9-部署说明)

---

## 1. 项目概述

### 1.1 项目简介

三角洲陪玩管理系统是一个完整的陪玩服务管理平台，支持：

- 用户注册与审核
- 订单创建、派单、接单、完成
- 支持单人和双人陪玩订单
- 财务统计与提现管理
- 系统配置管理

### 1.2 项目结构

```
biubiu_pwd/
├── biubiu-pwd-springboot/    # 后端项目
│   ├── src/main/java/com/biubiu/
│   │   ├── config/           # 配置类
│   │   ├── controller/       # 控制器
│   │   ├── dto/              # 数据传输对象
│   │   ├── entity/           # 实体类
│   │   ├── exception/        # 异常处理
│   │   ├── repository/       # 数据访问层
│   │   ├── security/         # 安全模块
│   │   └── service/          # 业务逻辑层
│   └── src/main/resources/
│       └── application.yml   # 配置文件
│
└── biubiu-pwd-vue3/          # 前端项目
    ├── src/
    │   ├── api/              # API接口
    │   ├── layouts/          # 布局组件
    │   ├── router/           # 路由配置
    │   ├── stores/           # 状态管理
    │   └── views/            # 页面组件
    └── package.json
```

---

## 2. 技术栈详情

### 2.1 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Boot** | 3.2.0 | 核心框架 |
| **JDK** | 17 | Java运行环境 |
| **Spring Data JPA** | - | ORM框架 (Hibernate) |
| **Spring Security** | - | 安全框架 |
| **JWT (jjwt)** | 0.12.3 | Token认证 |
| **MySQL** | - | 关系型数据库 |
| **Lombok** | - | 代码简化工具 |
| **MapStruct** | 1.5.5 | 对象映射工具 |
| **Maven** | - | 项目构建工具 |

### 2.2 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue** | 3.4.0 | 前端框架 |
| **Vite** | 5.0.0 | 构建工具 |
| **Element Plus** | 2.5.0 | UI组件库 |
| **Pinia** | 2.1.0 | 状态管理 |
| **Vue Router** | 4.2.0 | 路由管理 |
| **Axios** | 1.6.0 | HTTP客户端 |
| **ECharts** | 6.0.0 | 图表库 |
| **vue-echarts** | 8.0.1 | Vue ECharts封装 |
| **Day.js** | 1.11.0 | 日期处理 |
| **Sass** | 1.70.0 | CSS预处理 |

### 2.3 主要依赖说明

#### 后端核心依赖

```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

#### 前端核心依赖

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "element-plus": "^2.5.0",
    "axios": "^1.6.0",
    "echarts": "^6.0.0",
    "vue-echarts": "^8.0.1",
    "dayjs": "^1.11.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "@vitejs/plugin-vue": "^5.0.0",
    "sass": "^1.70.0"
  }
}
```

---

## 3. 系统架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │ Dashboard│ │ Orders  │ │ Finance │ │ Players │ ...       │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘           │
│       │           │           │           │                  │
│  ┌────┴───────────┴───────────┴───────────┴────┐           │
│  │              Axios HTTP Client               │           │
│  └──────────────────────┬──────────────────────┘           │
└─────────────────────────┼───────────────────────────────────┘
                          │ HTTP/JSON
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    后端 (Spring Boot)                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Spring Security + JWT                   │   │
│  └─────────────────────────┬───────────────────────────┘   │
│                            │                                │
│  ┌─────────┐ ┌─────────┐ ┌─┴───────┐ ┌─────────┐           │
│  │AuthCtrl │ │UserCtrl │ │OrderCtrl│ │FinanceCtrl│ ...     │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘           │
│       │           │           │           │                  │
│  ┌────┴───────────┴───────────┴───────────┴────┐           │
│  │              Service Layer                   │           │
│  └──────────────────────┬──────────────────────┘           │
│                         │                                   │
│  ┌──────────────────────┴──────────────────────┐           │
│  │         Spring Data JPA Repository          │           │
│  └──────────────────────┬──────────────────────┘           │
└─────────────────────────┼───────────────────────────────────┘
                          │ JDBC
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      MySQL Database                          │
│  users | orders | order_sessions | financial_records | ...  │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 认证流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │────▶│ /login   │────▶│  JWT     │────▶│  Token   │
│          │     │ Endpoint │     │ Generate │     │  Response│
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                                                        │
                                                        ▼
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  Access  │────▶│  JWT     │────▶│  User    │────▶│  API     │
│  API     │     │  Filter  │     │  Details │     │  Response│
└──────────┘     └──────────┘     └──────────┘     └──────────┘
```

---

## 4. 数据库设计

### 4.1 ER图概览

```
┌─────────────┐       ┌─────────────┐       ┌─────────────────┐
│    users    │       │   orders    │       │ order_sessions  │
├─────────────┤       ├─────────────┤       ├─────────────────┤
│ id (PK)     │◀──┐   │ id (PK)     │◀──────│ order_id (FK)   │
│ phone       │   │   │ order_no    │       │ player_id (FK)  │
│ password    │   │   │ boss_info   │       │ started_at      │
│ nickname    │   │   │ status      │       │ ended_at        │
│ role        │   │   │ player_id   │──┐    │ actual_hours    │
│ level       │   │   │ player2_id  │──┼───▶│                 │
│ status      │   │   │ total_amount│  │    └─────────────────┘
│ total_income│   │   └─────────────┘  │
│ balance     │   │                    │    ┌─────────────────┐
└─────────────┘   │                    │    │financial_records│
      ▲           │                    │    ├─────────────────┤
      │           │                    └────▶│ order_id (FK)   │
      │           │                         │ player_id (FK)  │
      │           └────────────────────────▶│ type            │
      │                                     │ amount          │
      │           ┌─────────────────┐       └─────────────────┘
      │           │withdrawal_req   │
      │           ├─────────────────┤       ┌─────────────────┐
      └──────────▶│ player_id (FK)  │       │ operation_logs  │
                  │ amount          │       ├─────────────────┤
                  │ status          │       │ order_id (FK)   │
                  └─────────────────┘       │ operator_id(FK) │
                                            │ operation_type  │
                                            └─────────────────┘
```

### 4.2 数据表详细设计

#### 4.2.1 users - 用户表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| phone | VARCHAR(11) | UNIQUE, NOT NULL | 手机号（登录账号） |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt加密） |
| nickname | VARCHAR(50) | NOT NULL | 昵称 |
| avatar | VARCHAR(255) | NULL | 头像URL |
| role | VARCHAR(20) | DEFAULT 'player' | 角色：player/customer_service/admin |
| level | VARCHAR(20) | DEFAULT '青铜' | 陪玩师等级 |
| price_per_hour | DECIMAL(10,2) | DEFAULT 50.00 | 时薪 |
| status | VARCHAR(20) | DEFAULT 'pending' | 状态：pending/active/disabled |
| total_income | DECIMAL(10,2) | DEFAULT 0.00 | 累计收入 |
| available_balance | DECIMAL(10,2) | DEFAULT 0.00 | 可提现余额 |
| created_at | DATETIME | AUTO | 创建时间 |
| updated_at | DATETIME | AUTO | 更新时间 |

**角色枚举 (Role)**:
- `player` - 陪玩师
- `customer_service` - 客服
- `admin` - 管理员

**状态枚举 (Status)**:
- `pending` - 待审核
- `active` - 正常
- `disabled` - 禁用

---

#### 4.2.2 orders - 订单表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_no | VARCHAR(20) | UNIQUE, NOT NULL | 订单编号 |
| boss_info | TEXT | NOT NULL | 老板信息 |
| service_content | TEXT | NOT NULL | 服务内容 |
| service_hours | DECIMAL(4,1) | NOT NULL | 服务时长 |
| price_per_hour | DECIMAL(10,2) | NOT NULL | 时薪 |
| total_amount | DECIMAL(10,2) | NOT NULL | 订单总金额 |
| scheduled_time | DATETIME | NULL | 预约时间 |
| remark | TEXT | NULL | 备注 |
| status | INT | NOT NULL, DEFAULT 0 | 订单状态 |
| current_player_id | BIGINT | FK | 当前陪玩师1 |
| current_player2_id | BIGINT | FK | 当前陪玩师2 |
| player_count | VARCHAR(20) | NOT NULL | 单人/双人 |
| created_by | BIGINT | FK, NOT NULL | 创建人 |
| assigned_by | BIGINT | FK | 派单人 |
| created_at | DATETIME | AUTO | 创建时间 |
| assigned_at | DATETIME | NULL | 派单时间 |
| started_at | DATETIME | NULL | 开始时间 |
| completed_at | DATETIME | NULL | 完成时间 |
| actual_hours | DECIMAL(4,1) | NULL | 实际时长 |
| cancel_reason | TEXT | NULL | 取消原因 |
| cancelled_at | DATETIME | NULL | 取消时间 |

**订单状态枚举 (Status)**:
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | PENDING_ASSIGN | 待分配 |
| 1 | PENDING_ACCEPT | 待接单（单人）/ 待接单1（双人） |
| 2 | PENDING_ACCEPT_2 | 待接单2（双人订单第二个） |
| 3 | IN_SERVICE | 服务中 |
| 4 | COMPLETED | 已完成 |
| 5 | CANCELLED | 已取消 |

**订单类型枚举 (PlayerCount)**:
- `SINGLE` - 单人订单
- `DOUBLE` - 双人订单

---

#### 4.2.3 order_sessions - 订单会话表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | BIGINT | FK, NOT NULL | 订单ID |
| player_id | BIGINT | FK, NOT NULL | 陪玩师ID |
| started_at | DATETIME | NOT NULL | 开始时间 |
| ended_at | DATETIME | NULL | 结束时间 |
| actual_hours | DECIMAL(4,1) | NULL | 实际服务时长 |

> 用于支持双人订单，每个陪玩师的接单和完成时间独立记录。

---

#### 4.2.4 financial_records - 财务记录表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | BIGINT | FK | 关联订单 |
| player_id | BIGINT | FK, NOT NULL | 陪玩师ID |
| type | VARCHAR(20) | NOT NULL | 类型：income/withdrawal |
| amount | DECIMAL(10,2) | NOT NULL | 金额 |
| description | TEXT | NULL | 描述 |
| created_at | DATETIME | AUTO | 创建时间 |

**类型枚举 (Type)**:
- `income` - 收入
- `withdrawal` - 提现

---

#### 4.2.5 withdrawal_requests - 提现申请表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| player_id | BIGINT | FK, NOT NULL | 申请人ID |
| amount | DECIMAL(10,2) | NOT NULL | 提现金额 |
| payment_method | VARCHAR(20) | NOT NULL | 支付方式 |
| account_info | TEXT | NOT NULL | 账号信息 |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'pending' | 状态 |
| reviewed_by | BIGINT | FK | 审核人ID |
| reviewed_at | DATETIME | NULL | 审核时间 |
| reject_reason | TEXT | NULL | 拒绝原因 |
| created_at | DATETIME | AUTO | 创建时间 |

**状态枚举 (Status)**:
- `pending` - 待审核
- `approved` - 已通过
- `rejected` - 已拒绝

---

#### 4.2.6 level_prices - 等级价格配置表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| level | VARCHAR(20) | UNIQUE, NOT NULL | 等级名称 |
| default_price | DECIMAL(10,2) | NOT NULL | 默认时薪 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 排序序号 |
| created_at | DATETIME | AUTO | 创建时间 |

---

#### 4.2.7 system_config - 系统配置表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| platform_fee_rate | DECIMAL(5,4) | DEFAULT 0.2000 | 平台抽成比例 |
| updated_at | DATETIME | AUTO | 更新时间 |

---

#### 4.2.8 system_options - 系统选项表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| type | VARCHAR(50) | NOT NULL | 选项类型 |
| value | VARCHAR(100) | NOT NULL | 选项值 |
| description | VARCHAR(200) | NULL | 描述 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 排序序号 |
| created_at | DATETIME | AUTO | 创建时间 |

**选项类型 (OptionType)**:
- `PRECAUTION` - 陪玩单注意事项
- `SERVICE_ITEM` - 护航单服务项目

---

#### 4.2.9 operation_logs - 操作日志表

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | BIGINT | FK | 订单ID |
| operator_id | BIGINT | FK | 操作人ID |
| operation_type | VARCHAR(20) | NOT NULL | 操作类型 |
| old_status | VARCHAR(20) | NULL | 原状态 |
| new_status | VARCHAR(20) | NOT NULL | 新状态 |
| details | TEXT | NULL | 详情 |
| created_at | DATETIME | AUTO | 创建时间 |

**操作类型**:
- `CREATE` - 创建订单
- `ASSIGN` - 派单
- `REASSIGN` - 改派
- `ACCEPT` - 接单
- `CANCEL` - 取消
- `COMPLETE` - 完成

---

## 5. API接口文档

### 5.1 通用说明

#### 基础URL
```
http://localhost:8080/api
```

#### 认证方式
使用 JWT Token 认证，在请求头中添加：
```
Authorization: Bearer <token>
```

#### 统一响应格式

**成功响应**:
```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... }
}
```

**失败响应**:
```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

#### 分页响应格式
```json
{
  "success": true,
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

---

### 5.2 认证模块 (`/api/auth`)

#### 5.2.1 用户登录

**接口**: `POST /api/auth/login`

**权限**: 公开

**请求体**:
```json
{
  "phone": "13800138000",
  "password": "123456"
}
```

**响应**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "id": 1,
    "phone": "13800138000",
    "nickname": "陪玩师A",
    "avatar": null,
    "role": "player",
    "level": "黄金",
    "pricePerHour": 80.00,
    "status": "active",
    "totalIncome": 1000.00,
    "availableBalance": 500.00,
    "createdAt": "2024-01-01T00:00:00",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**错误情况**:
- 用户不存在
- 密码错误
- 账号待审核
- 账号已禁用

---

#### 5.2.2 用户注册

**接口**: `POST /api/auth/register`

**权限**: 公开

**请求体**:
```json
{
  "phone": "13800138000",
  "password": "123456",
  "nickname": "新陪玩师"
}
```

**响应**:
```json
{
  "success": true,
  "message": "注册成功，请等待管理员审核",
  "data": null
}
```

**说明**: 注册后用户状态为 `pending`，需管理员审核通过后方可登录。

---

### 5.3 用户模块 (`/api/users`)

#### 5.3.1 获取当前用户信息

**接口**: `GET /api/users/me`

**权限**: 已登录用户

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "phone": "13800138000",
    "nickname": "陪玩师A",
    "avatar": null,
    "role": "player",
    "level": "黄金",
    "pricePerHour": 80.00,
    "status": "active",
    "totalIncome": 1000.00,
    "availableBalance": 500.00,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

---

#### 5.3.2 获取陪玩师列表

**接口**: `GET /api/users/players`

**权限**: admin, customer_service

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 状态筛选 |
| level | String | 否 | 等级筛选 |
| search | String | 否 | 搜索（昵称/手机号） |
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认10 |

**响应**:
```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "nickname": "陪玩师A",
        "phone": "13800138000",
        "level": "黄金",
        "pricePerHour": 80.00,
        "status": "active",
        "totalIncome": 1000.00,
        "availableBalance": 500.00,
        "activeOrdersCount": 1
      }
    ],
    "total": 50,
    "page": 1,
    "pageSize": 10
  }
}
```

---

#### 5.3.3 审核陪玩师

**接口**: `POST /api/users/{id}/approve`

**权限**: admin

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |

**请求体**:
```json
{
  "level": "黄金",
  "pricePerHour": 80.00
}
```

**响应**:
```json
{
  "success": true,
  "message": "审核通过",
  "data": null
}
```

---

#### 5.3.4 更新用户信息

**接口**: `PUT /api/users/{id}`

**权限**: admin

**请求体**:
```json
{
  "nickname": "新昵称",
  "level": "钻石",
  "pricePerHour": 100.00,
  "status": "active"
}
```

**响应**:
```json
{
  "success": true,
  "message": "更新成功",
  "data": null
}
```

---

#### 5.3.5 重置密码

**接口**: `POST /api/users/{id}/reset-password`

**权限**: admin

**请求体**:
```json
{
  "password": "newPassword123"
}
```

**响应**:
```json
{
  "success": true,
  "message": "密码重置成功",
  "data": null
}
```

---

#### 5.3.6 获取陪玩师等级列表

**接口**: `GET /api/users/players/levels`

**权限**: admin, customer_service

**响应**:
```json
{
  "success": true,
  "data": ["青铜", "白银", "黄金", "铂金", "钻石"]
}
```

---

### 5.4 订单模块 (`/api/orders`)

#### 5.4.1 获取订单列表

**接口**: `GET /api/orders`

**权限**: 已登录用户

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 订单状态（0-5） |
| today | Boolean | 否 | 仅今日订单 |
| startDate | String | 否 | 开始日期 (yyyy-MM-dd) |
| endDate | String | 否 | 结束日期 (yyyy-MM-dd) |
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认10 |
| sortBy | String | 否 | 排序字段，默认createdAt |
| sortOrder | String | 否 | 排序方向 asc/desc，默认desc |

**响应**:
```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "orderNo": "DD20240101000001",
        "bossInfo": "老板联系方式...",
        "serviceContent": "陪玩服务内容...",
        "serviceHours": 2.0,
        "pricePerHour": 80.00,
        "totalAmount": 160.00,
        "scheduledTime": "2024-01-01T14:00:00",
        "remark": "备注信息",
        "status": 3,
        "currentPlayerId": 1,
        "currentPlayerNickname": "陪玩师A",
        "currentPlayer2Id": null,
        "currentPlayer2Nickname": null,
        "createdAt": "2024-01-01T10:00:00",
        "startedAt": "2024-01-01T14:00:00",
        "completedAt": null,
        "actualHours": null,
        "createdByNickname": "客服A",
        "assignedByNickname": "客服A",
        "cancelReason": null,
        "cancelledAt": null,
        "playerCount": "SINGLE",
        "currentUserAccepted": true,
        "currentUserCompleted": false,
        "otherPlayerCompleted": false,
        "incomeAmount": null,
        "playerSessions": [
          {
            "playerId": 1,
            "playerNickname": "陪玩师A",
            "startedAt": "2024-01-01T14:00:00",
            "endedAt": null,
            "actualHours": null
          }
        ]
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

---

#### 5.4.2 创建订单

**接口**: `POST /api/orders`

**权限**: admin, customer_service

**请求体**:
```json
{
  "bossInfo": "老板联系方式...",
  "serviceContent": "陪玩服务内容...",
  "serviceHours": 2.0,
  "pricePerHour": 80.00,
  "totalAmount": 160.00,
  "scheduledTime": "2024-01-01T14:00:00",
  "remark": "备注信息",
  "playerCount": "single"
}
```

**响应**: 返回创建的订单详情

---

#### 5.4.3 获取订单详情

**接口**: `GET /api/orders/{id}`

**权限**: 已登录用户

**响应**: 返回订单详情（同列表项格式）

---

#### 5.4.4 派单/改派

**接口**: `POST /api/orders/{id}/assign`

**权限**: admin, customer_service

**请求体（单人订单）**:
```json
{
  "playerId": 1
}
```

**请求体（双人订单）**:
```json
{
  "playerId": 1,
  "playerId2": 2
}
```

**响应**:
```json
{
  "success": true,
  "message": "派送成功",
  "data": null
}
```

---

#### 5.4.5 取消订单

**接口**: `POST /api/orders/{id}/cancel`

**权限**: admin, customer_service

**请求体**:
```json
{
  "reason": "客户取消"
}
```

**响应**:
```json
{
  "success": true,
  "message": "订单取消成功",
  "data": null
}
```

---

#### 5.4.6 接单（陪玩师）

**接口**: `POST /api/orders/{id}/accept`

**权限**: player

**说明**: 
- 单人订单：接单后直接进入服务中状态
- 双人订单：两人都接单后才进入服务中状态

**响应**:
```json
{
  "success": true,
  "message": "接单成功",
  "data": null
}
```

---

#### 5.4.7 完成订单（陪玩师）

**接口**: `POST /api/orders/{id}/complete`

**权限**: player

**请求体**:
```json
{
  "actualHours": 2.0
}
```

**说明**:
- 双人订单需要两人都完成后，订单才变为已完成
- 完成后自动计算收入并记录财务

**响应**:
```json
{
  "success": true,
  "message": "订单完成",
  "data": null
}
```

---

#### 5.4.8 获取我的服务中订单

**接口**: `GET /api/orders/my-in-service`

**权限**: player

**响应**: 返回当前用户服务中的订单列表

---

#### 5.4.9 获取所有服务中订单

**接口**: `GET /api/orders/admin/in-service`

**权限**: admin, customer_service

**响应**: 返回所有服务中的订单列表

---

#### 5.4.10 批量删除订单

**接口**: `DELETE /api/orders/batch`

**权限**: admin

**请求体**:
```json
[1, 2, 3]
```

**响应**:
```json
{
  "success": true,
  "message": "批量删除成功",
  "data": null
}
```

---

### 5.5 财务模块 (`/api/finance`)

#### 5.5.1 获取财务统计

**接口**: `GET /api/finance/statistics`

**权限**: admin, customer_service

**响应**:
```json
{
  "success": true,
  "data": {
    "todayOrderAmount": 1000.00,
    "monthOrderAmount": 30000.00,
    "totalOrderAmount": 100000.00,
    "totalPlatformIncome": 20000.00,
    "totalPlayerIncome": 80000.00,
    "todayOrderCount": 5,
    "monthOrderCount": 150,
    "totalOrderCount": 500,
    "cancelledOrderCount": 20,
    "cancelledOrderAmount": 2000.00,
    "todayCancelledCount": 1,
    "todayCancelledAmount": 100.00,
    "monthCancelledCount": 10,
    "monthCancelledAmount": 1000.00,
    "playerRanking": [
      {
        "playerId": 1,
        "nickname": "陪玩师A",
        "totalIncome": 5000.00
      }
    ],
    "incomeDistribution": {
      "陪玩师收入": 24000.00,
      "平台抽成": 6000.00,
      "取消订单": 1000.00
    },
    "recentIncomes": [
      {
        "date": "2024-01-01",
        "amount": 1000.00,
        "orderCount": 5
      }
    ],
    "totalPlayerCount": 50,
    "pendingPlayerCount": 5,
    "activePlayerCount": 40,
    "pendingWithdrawals": 3,
    "pendingOrders": 10
  }
}
```

---

#### 5.5.2 获取我的收入

**接口**: `GET /api/finance/income`

**权限**: player

**响应**:
```json
{
  "success": true,
  "data": {
    "totalIncome": 5000.00,
    "availableBalance": 2000.00,
    "todayIncome": 200.00
  }
}
```

---

#### 5.5.3 获取收入记录

**接口**: `GET /api/finance/income/records`

**权限**: 已登录用户

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认10 |

**响应**:
```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": 1,
        "type": "income",
        "amount": 160.00,
        "orderNo": "DD20240101000001",
        "serviceContent": "陪玩服务...",
        "description": "订单收入 (单号: DD20240101000001)",
        "createdAt": "2024-01-01T16:00:00",
        "playerNickname": "陪玩师A"
      }
    ],
    "total": 50,
    "page": 1,
    "pageSize": 10
  }
}
```

---

#### 5.5.4 申请提现

**接口**: `POST /api/finance/withdraw`

**权限**: player

**请求体**:
```json
{
  "amount": 500.00,
  "paymentMethod": "支付宝",
  "accountInfo": "account@example.com",
  "realName": "张三"
}
```

**响应**:
```json
{
  "success": true,
  "message": "提现申请已提交",
  "data": null
}
```

---

#### 5.5.5 获取待处理提现

**接口**: `GET /api/finance/withdraw/pending`

**权限**: admin

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "playerNickname": "陪玩师A",
      "amount": 500.00,
      "paymentMethod": "支付宝",
      "accountInfo": "account@example.com",
      "realName": "张三",
      "status": "pending",
      "rejectReason": null,
      "createdAt": "2024-01-01T10:00:00"
    }
  ]
}
```

---

#### 5.5.6 审核提现

**接口**: `POST /api/finance/withdraw/{id}/review`

**权限**: admin

**请求体（通过）**:
```json
{
  "status": "approved"
}
```

**请求体（拒绝）**:
```json
{
  "status": "rejected",
  "rejectReason": "信息有误"
}
```

**响应**:
```json
{
  "success": true,
  "message": "处理成功",
  "data": null
}
```

---

### 5.6 系统模块 (`/api/system`)

#### 5.6.1 获取系统配置

**接口**: `GET /api/system/config`

**权限**: admin

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "platformFeeRate": 0.2000,
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

---

#### 5.6.2 更新系统配置

**接口**: `POST /api/system/config`

**权限**: admin

**请求体**:
```json
{
  "platformFeeRate": 0.2000
}
```

---

#### 5.6.3 获取等级价格列表

**接口**: `GET /api/system/levels`

**权限**: 已登录用户

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "level": "青铜",
      "defaultPrice": 50.00,
      "sortOrder": 0,
      "createdAt": "2024-01-01T00:00:00"
    },
    {
      "id": 2,
      "level": "白银",
      "defaultPrice": 60.00,
      "sortOrder": 1,
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

#### 5.6.4 更新等级价格

**接口**: `POST /api/system/levels`

**权限**: admin

**请求体**:
```json
{
  "id": 1,
  "defaultPrice": 55.00
}
```

---

#### 5.6.5 创建等级

**接口**: `POST /api/system/levels/create`

**权限**: admin

**请求体**:
```json
{
  "level": "王者",
  "defaultPrice": 150.00
}
```

---

#### 5.6.6 删除等级

**接口**: `DELETE /api/system/levels/{id}`

**权限**: admin

---

#### 5.6.7 排序等级

**接口**: `POST /api/system/levels/sort`

**权限**: admin

**请求体**:
```json
[
  { "id": 1 },
  { "id": 2 },
  { "id": 3 }
]
```

---

#### 5.6.8 获取系统选项

**接口**: `GET /api/system/options`

**权限**: 已登录用户

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 选项类型：PRECAUTION/SERVICE_ITEM |

**响应**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "PRECAUTION",
      "value": "请准时上线",
      "description": "准时提醒",
      "sortOrder": 0,
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

#### 5.6.9 创建系统选项

**接口**: `POST /api/system/options`

**权限**: admin

**请求体**:
```json
{
  "type": "PRECAUTION",
  "value": "请准时上线",
  "description": "准时提醒"
}
```

---

#### 5.6.10 更新系统选项

**接口**: `PUT /api/system/options/{id}`

**权限**: admin

---

#### 5.6.11 删除系统选项

**接口**: `DELETE /api/system/options/{id}`

**权限**: admin

---

#### 5.6.12 排序系统选项

**接口**: `POST /api/system/options/sort`

**权限**: admin

---

### 5.7 健康检查模块 (`/api`)

#### 5.7.1 服务健康检查

**接口**: `GET /api/health`

**权限**: 公开

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "OK",
    "message": "三角洲陪玩管理系统服务正常运行"
  }
}
```

---

## 6. 用户角色与权限

### 6.1 角色定义

| 角色 | 标识 | 说明 |
|------|------|------|
| 管理员 | `admin` | 系统最高权限，可管理所有功能 |
| 客服 | `customer_service` | 可管理订单、查看陪玩师信息 |
| 陪玩师 | `player` | 可接单、完成订单、申请提现 |

### 6.2 权限矩阵

| 功能模块 | 操作 | admin | customer_service | player |
|----------|------|:-----:|:----------------:|:------:|
| **认证** | 登录 | ✅ | ✅ | ✅ |
| | 注册 | ✅ | ✅ | ✅ |
| **用户** | 查看当前用户 | ✅ | ✅ | ✅ |
| | 查看陪玩师列表 | ✅ | ✅ | ❌ |
| | 审核陪玩师 | ✅ | ❌ | ❌ |
| | 更新用户信息 | ✅ | ❌ | ❌ |
| | 重置密码 | ✅ | ❌ | ❌ |
| **订单** | 查看订单列表 | ✅ | ✅ | ✅（仅自己的） |
| | 创建订单 | ✅ | ✅ | ❌ |
| | 派单/改派 | ✅ | ✅ | ❌ |
| | 取消订单 | ✅ | ✅ | ❌ |
| | 接单 | ❌ | ❌ | ✅ |
| | 完成订单 | ❌ | ❌ | ✅ |
| | 批量删除 | ✅ | ❌ | ❌ |
| **财务** | 查看统计 | ✅ | ✅ | ❌ |
| | 查看我的收入 | ❌ | ❌ | ✅ |
| | 查看收入记录 | ✅（全部） | ❌ | ✅（自己的） |
| | 申请提现 | ❌ | ❌ | ✅ |
| | 审核提现 | ✅ | ❌ | ❌ |
| **系统** | 系统配置 | ✅ | ❌ | ❌ |
| | 等级管理 | ✅ | ❌ | ❌ |
| | 选项管理 | ✅ | ❌ | ❌ |

---

## 7. 业务流程说明

### 7.1 用户注册与审核流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 用户注册  │────▶│ 状态:    │────▶│ 管理员   │────▶│ 状态:    │
│ (pending)│     │ pending  │     │ 审核     │     │ active   │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                                          │
                                          ▼
                                  ┌──────────┐
                                  │ 设置等级 │
                                  │ 和时薪   │
                                  └──────────┘
```

**流程说明**:
1. 用户通过注册接口提交手机号、密码、昵称
2. 系统创建用户，状态为 `pending`（待审核）
3. 管理员在陪玩师管理页面审核
4. 审核通过时设置等级和时薪，状态变为 `active`
5. 用户可以正常登录使用

---

### 7.2 订单创建与派单流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 创建订单  │────▶│ 状态:    │────▶│ 派单     │────▶│ 状态:    │
│          │     │ PENDING_ │     │ 选择陪玩 │     │ PENDING_ │
│          │     │ ASSIGN   │     │ 师       │     │ ACCEPT   │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
```

**流程说明**:
1. 客服/管理员创建订单，填写老板信息、服务内容、时长、金额等
2. 订单创建后状态为 `PENDING_ASSIGN`（待分配）
3. 选择陪玩师进行派单
   - 单人订单：选择1名陪玩师
   - 双人订单：选择2名陪玩师
4. 派单后状态变为 `PENDING_ACCEPT`（待接单）

---

### 7.3 单人订单接单流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 待接单   │────▶│ 陪玩师   │────▶│ 状态:    │────▶│ 服务中   │
│ PENDING_ │     │ 接单     │     │ IN_     │     │          │
│ ACCEPT   │     │          │     │ SERVICE  │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
```

**流程说明**:
1. 陪玩师收到派单通知
2. 陪玩师点击接单
3. 系统检查该陪玩师是否有其他服务中的订单
4. 无冲突则接单成功，订单进入 `IN_SERVICE` 状态
5. 创建服务会话记录（order_sessions）

---

### 7.4 双人订单接单流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 待接单   │────▶│ 陪玩师1  │────▶│ 状态:    │────▶│ 陪玩师2  │
│ PENDING_ │     │ 接单     │     │ PENDING_ │     │ 接单     │
│ ACCEPT   │     │          │     │ ACCEPT_2 │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                                                          │
                                                          ▼
                                                  ┌──────────┐
                                                  │ 状态:    │
                                                  │ IN_     │
                                                  │ SERVICE  │
                                                  └──────────┘
```

**流程说明**:
1. 双人订单派单时选择2名陪玩师
2. 第一位陪玩师接单后，状态变为 `PENDING_ACCEPT_2`
3. 第二位陪玩师接单后，状态变为 `IN_SERVICE`
4. 两位陪玩师独立接单，互不影响

---

### 7.5 订单完成与结算流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 服务中   │────▶│ 陪玩师   │────▶│ 计算收入 │────▶│ 状态:    │
│ IN_     │     │ 完成服务 │     │ 分配     │     │ COMPLETED│
│ SERVICE  │     │          │     │          │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                        │
                        ▼
                ┌──────────┐
                │ 双人订单 │
                │ 等待两人 │
                │ 都完成   │
                └──────────┘
```

**收入计算规则**:

1. **单人订单**:
   - 陪玩师收入 = 订单总金额 × (1 - 平台抽成比例)
   - 默认平台抽成 20%

2. **双人订单**:
   - 总可分配收入 = 订单总金额 × (1 - 平台抽成比例)
   - 每人收入 = 总可分配收入 ÷ 2

**流程说明**:
1. 陪玩师完成服务后点击完成
2. 系统记录服务会话结束时间和实际时长
3. 双人订单需两人都完成后才进入完成状态
4. 完成时自动计算收入，更新陪玩师余额
5. 创建财务记录

---

### 7.6 提现申请与审核流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 申请提现 │────▶│ 冻结金额 │────▶│ 管理员   │────▶│ 审核结果 │
│          │     │          │     │ 审核     │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                                          │
                        ┌─────────────────┴─────────────────┐
                        ▼                                   ▼
                ┌──────────┐                       ┌──────────┐
                │ 通过     │                       │ 拒绝     │
                │ 状态:    │                       │ 退回金额 │
                │ approved │                       │ 状态:    │
                └──────────┘                       │ rejected │
                                                   └──────────┘
```

**流程说明**:
1. 陪玩师提交提现申请（金额、支付方式、账号、真实姓名）
2. 系统冻结对应金额（从可用余额扣除）
3. 管理员审核提现申请
4. 审核通过：状态变为 `approved`
5. 审核拒绝：状态变为 `rejected`，金额退回可用余额

---

## 8. 前端页面结构

### 8.1 路由配置

| 路径 | 页面 | 权限 | 说明 |
|------|------|------|------|
| `/login` | Login | 公开 | 登录页面 |
| `/dashboard` | Dashboard | 已登录 | 仪表盘（统计概览） |
| `/players` | Players | admin, customer_service | 陪玩师管理 |
| `/orders` | Orders | 已登录 | 订单管理 |
| `/finance` | Finance | 已登录 | 财务管理 |
| `/withdrawals` | Withdrawals | admin | 提现审核 |
| `/settings` | Settings | admin | 系统设置 |

### 8.2 页面功能说明

#### Dashboard（仪表盘）
- 今日/本月订单统计
- 收入统计与分布
- 陪玩师收入排行
- 最近7天收入趋势图
- 待处理事项提醒

#### Players（陪玩师管理）
- 陪玩师列表（支持筛选、搜索）
- 审核待审核陪玩师
- 编辑陪玩师信息
- 重置密码

#### Orders（订单管理）
- 订单列表（支持多条件筛选）
- 创建订单
- 派单/改派
- 取消订单
- 陪玩师：接单、完成订单

#### Finance（财务管理）
- 管理员/客服：财务统计
- 陪玩师：我的收入、收入记录、申请提现

#### Withdrawals（提现审核）
- 待处理提现列表
- 审核通过/拒绝

#### Settings（系统设置）
- 平台抽成比例配置
- 等级价格管理
- 注意事项/服务项目管理

---

## 9. 部署说明

### 9.1 环境要求

**后端**:
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

**前端**:
- Node.js 18+
- npm 9+

### 9.2 后端部署

#### 1. 数据库配置

创建数据库：
```sql
CREATE DATABASE biubiu_pwd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. 修改配置文件

编辑 `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/biubiu_pwd?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password

jwt:
  secret: your-secret-key
  expiration: 86400000
```

#### 3. 构建运行

```bash
# 进入后端目录
cd biubiu-pwd-springboot

# 构建
mvn clean package -DskipTests

# 运行
java -jar target/biubiu-pwd-backend-1.0.0.jar
```

### 9.3 前端部署

#### 1. 安装依赖

```bash
# 进入前端目录
cd biubiu-pwd-vue3

# 安装依赖
npm install
```

#### 2. 开发环境运行

```bash
npm run dev
```

#### 3. 生产环境构建

```bash
npm run build
```

构建产物在 `dist/` 目录，可部署到任意静态服务器。

### 9.4 默认管理员账号

系统初始化时会创建默认管理员账号：
- 手机号：`admin`
- 密码：`admin123`

> ⚠️ 请在生产环境中及时修改默认密码！

---

## 附录

### A. 订单状态流转图

```
                    ┌─────────────────────────────────────┐
                    │                                     │
                    ▼                                     │
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ 创建订单 │────▶│ 待分配   │────▶│ 待接单   │────▶│ 服务中   │
│          │     │ PENDING_ │     │ PENDING_ │     │ IN_     │
│          │     │ ASSIGN   │     │ ACCEPT   │     │ SERVICE  │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
                      │                │                │
                      │                │                │
                      ▼                ▼                ▼
                 ┌──────────┐    ┌──────────┐    ┌──────────┐
                 │ 已取消   │◀───│ 已取消   │◀───│ 已完成   │
                 │ CANCELLED│    │ CANCELLED│    │ COMPLETED│
                 └──────────┘    └──────────┘    └──────────┘
```

### B. 错误码说明

| 错误信息 | 说明 |
|----------|------|
| 用户不存在 | 登录时手机号未注册 |
| 密码错误 | 登录密码不正确 |
| 账号待审核 | 用户状态为pending |
| 账号已被禁用 | 用户状态为disabled |
| 该手机号已注册 | 注册时手机号重复 |
| 订单不存在 | 订单ID无效 |
| 订单状态不正确 | 操作与当前状态不匹配 |
| 无权操作此订单 | 非订单相关陪玩师 |
| 提现金额超过可提现余额 | 余额不足 |
| 陪玩师状态不正常 | 陪玩师非active状态 |

---

**文档版本**: 1.0.0  
**最后更新**: 2024年
