# 三角洲陪玩管理系统 - Vue3 前端

## 技术栈

- Vue 3
- Vite
- Pinia (状态管理)
- Vue Router
- Element Plus (UI组件库)
- Axios (HTTP请求)
- Dayjs (日期处理)

## 项目结构

```
biubiu-pwd-vue3/
├── src/
│   ├── api/           # API接口
│   ├── assets/        # 静态资源
│   ├── components/    # 公共组件
│   ├── layouts/       # 布局组件
│   ├── router/        # 路由配置
│   ├── stores/        # Pinia状态管理
│   ├── views/         # 页面视图
│   ├── App.vue        # 根组件
│   └── main.js        # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 安装依赖

```bash
cd biubiu-pwd-vue3
npm install
```

## 开发运行

```bash
npm run dev
```

前端服务将运行在 http://localhost:5173

## 生产构建

```bash
npm run build
```

## 功能模块

- 登录/注册
- 首页仪表盘
- 陪玩师管理 (管理员/客服)
- 订单管理
- 财务管理
- 提现审核 (管理员)
- 系统设置 (管理员)

## 角色说明

1. **管理员 (admin)**: 拥有所有权限
2. **客服 (customer_service)**: 可以创建订单、派送订单、查看陪玩师
3. **陪玩师 (player)**: 可以接单、查看自己的订单和收入
