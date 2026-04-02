# 三角洲陪玩管理系统 - SpringBoot 后端

## 技术栈

- Spring Boot 3.2
- Spring Security (JWT认证)
- Spring Data JPA
- MySQL 8.0
- Maven
- Lombok

## 项目结构

```
biubiu-pwd-springboot/
├── src/main/java/com/biubiu/
│   ├── config/        # 配置类
│   ├── controller/    # 控制器
│   ├── dto/           # 数据传输对象
│   ├── entity/        # 实体类
│   ├── exception/     # 异常处理
│   ├── repository/    # 数据访问层
│   └── security/      # 安全相关
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

## 数据库配置

修改 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/biubiu_pwd?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## 数据库表结构

系统会自动创建以下表：

1. **users** - 用户表
2. **orders** - 订单表
3. **order_sessions** - 订单服务时段表
4. **financial_records** - 财务流水表
5. **withdrawal_requests** - 提现申请表
6. **system_config** - 系统配置表
7. **level_prices** - 等级价格配置表

## 运行项目

### 方式1: 使用Maven

```bash
cd biubiu-pwd-springboot
mvn spring-boot:run
```

### 方式2: 打包后运行

```bash
cd biubiu-pwd-springboot
mvn clean package
java -jar target/biubiu-pwd-backend-1.0.0.jar
```

后端服务将运行在 http://localhost:8080

## API接口

### 认证接口
- POST `/api/auth/login` - 登录
- POST `/api/auth/register` - 注册

### 用户接口
- GET `/api/users/me` - 获取当前用户信息
- GET `/api/users/players` - 获取陪玩师列表
- POST `/api/users/{id}/approve` - 审核陪玩师
- PUT `/api/users/{id}` - 更新陪玩师
- POST `/api/users/{id}/reset-password` - 重置密码

### 订单接口
- GET `/api/orders` - 获取订单列表
- POST `/api/orders` - 创建订单
- POST `/api/orders/{id}/assign` - 派送订单
- POST `/api/orders/{id}/accept` - 接单
- POST `/api/orders/{id}/complete` - 完成订单

### 财务接口
- GET `/api/finance/income` - 获取收入概览
- GET `/api/finance/income/records` - 获取收入明细
- POST `/api/finance/withdraw` - 申请提现
- GET `/api/finance/withdraw/pending` - 获取待审核提现
- POST `/api/finance/withdraw/{id}/review` - 审核提现

## 默认账号

系统启动时会自动创建以下账号：

1. **管理员**: 13800000000 / admin123
2. **客服**: 13800000001 / service123

## 跨域配置

后端已配置允许前端跨域访问，默认允许 http://localhost:5173
