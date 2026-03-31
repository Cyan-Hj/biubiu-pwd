# 技术文档生成计划

## 项目概述
**项目名称**: 三角洲陪玩管理系统 (biubiu-pwd)
**项目描述**: 一个完整的陪玩管理系统，包含后端 Spring Boot 和前端 Vue3 两部分

---

## 计划生成的文档内容

### 1. 技术栈文档

#### 后端技术栈
- **框架**: Spring Boot 3.2.0
- **Java版本**: JDK 17
- **数据库**: MySQL
- **ORM**: Spring Data JPA (Hibernate)
- **安全框架**: Spring Security + JWT (jjwt 0.12.3)
- **工具库**: Lombok, MapStruct
- **构建工具**: Maven

#### 前端技术栈
- **框架**: Vue 3.4.0
- **构建工具**: Vite 5.0.0
- **UI框架**: Element Plus 2.5.0
- **状态管理**: Pinia 2.1.0
- **路由**: Vue Router 4.2.0
- **HTTP客户端**: Axios 1.6.0
- **图表库**: ECharts 6.0.0 + vue-echarts
- **日期处理**: Day.js 1.11.0
- **CSS预处理**: Sass 1.70.0

### 2. 数据库设计文档

将包含以下实体表的设计：
- `users` - 用户表（管理员、客服、陪玩师）
- `orders` - 订单表
- `order_sessions` - 订单会话表（支持双人订单）
- `financial_records` - 财务记录表
- `withdrawal_requests` - 提现申请表
- `level_prices` - 等级价格配置表
- `system_config` - 系统配置表
- `system_options` - 系统选项表
- `operation_logs` - 操作日志表

### 3. API接口文档

#### 认证模块 (`/api/auth`)
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

#### 用户模块 (`/api/users`)
- `GET /api/users/me` - 获取当前用户信息
- `GET /api/users/players` - 获取陪玩师列表
- `POST /api/users/{id}/approve` - 审核陪玩师
- `PUT /api/users/{id}` - 更新用户信息
- `POST /api/users/{id}/reset-password` - 重置密码
- `GET /api/users/players/levels` - 获取陪玩师等级列表

#### 订单模块 (`/api/orders`)
- `GET /api/orders` - 获取订单列表
- `POST /api/orders` - 创建订单
- `GET /api/orders/{id}` - 获取订单详情
- `POST /api/orders/{id}/assign` - 派单/改派
- `POST /api/orders/{id}/cancel` - 取消订单
- `POST /api/orders/{id}/accept` - 接单（陪玩师）
- `POST /api/orders/{id}/complete` - 完成订单（陪玩师）
- `GET /api/orders/my-in-service` - 获取我的服务中订单
- `GET /api/orders/admin/in-service` - 获取所有服务中订单
- `DELETE /api/orders/batch` - 批量删除订单

#### 财务模块 (`/api/finance`)
- `GET /api/finance/statistics` - 获取财务统计
- `GET /api/finance/income` - 获取我的收入
- `GET /api/finance/income/records` - 获取收入记录
- `POST /api/finance/withdraw` - 申请提现
- `GET /api/finance/withdraw/pending` - 获取待处理提现
- `POST /api/finance/withdraw/{id}/review` - 审核提现

#### 系统模块 (`/api/system`)
- `GET /api/system/config` - 获取系统配置
- `POST /api/system/config` - 更新系统配置
- `GET /api/system/levels` - 获取等级价格列表
- `POST /api/system/levels` - 更新等级价格
- `POST /api/system/levels/create` - 创建等级
- `DELETE /api/system/levels/{id}` - 删除等级
- `POST /api/system/levels/sort` - 排序等级
- `GET /api/system/options` - 获取系统选项
- `POST /api/system/options` - 创建系统选项
- `PUT /api/system/options/{id}` - 更新系统选项
- `DELETE /api/system/options/{id}` - 删除系统选项
- `POST /api/system/options/sort` - 排序系统选项

#### 健康检查模块 (`/api`)
- `GET /api/health` - 服务健康检查

### 4. 用户角色与权限文档

三种角色：
- `admin` - 管理员（全部权限）
- `customer_service` - 客服（订单管理、陪玩师查看）
- `player` - 陪玩师（接单、完成订单、提现）

### 5. 业务流程文档

- 用户注册与审核流程
- 订单创建与派单流程
- 单人/双人订单接单流程
- 订单完成与结算流程
- 提现申请与审核流程

---

## 文档输出位置

将在项目根目录创建 `docs/` 文件夹，包含：
- `docs/TECHNICAL_SPEC.md` - 完整技术规格文档

---

## 执行步骤

1. 创建 `docs/` 目录
2. 生成 `TECHNICAL_SPEC.md` 技术规格文档，包含：
   - 项目概述
   - 技术栈详情
   - 数据库设计
   - API接口文档
   - 用户角色权限
   - 业务流程说明
   - 部署说明

---

**请确认此计划，我将开始生成详细的技术文档。**
