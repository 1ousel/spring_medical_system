# 医养结合云端后台管理系统 - 前端

本目录是系统前端工程，基于 Vue 3 + Vite 实现，提供后台管理页面、账号管理、老人档案、健康预警、评估报告、设备管理、重点人群、个人中心及算法演示页面。

## 技术栈

- **框架**：Vue 3
- **构建工具**：Vite
- **路由**：Vue Router
- **状态管理**：Pinia
- **HTTP 请求**：Axios
- **UI 组件库**：Element Plus
- **图表**：ECharts / vue-echarts

## 目录结构

```text
smsweb
├── src
│   ├── api              # 接口封装
│   ├── assets           # 静态资源
│   ├── router           # 路由配置
│   ├── store            # Pinia 状态
│   ├── utils            # request/auth 等工具
│   ├── views            # 页面组件
│   ├── App.vue
│   ├── main.js
│   └── style.css
├── package.json
└── vite.config.js
```

## 环境要求

- Node.js 18+
- npm
- 后端服务已启动：`http://localhost:8088`

## 安装依赖

```bash
npm install
```

## 启动开发环境

```bash
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

## 构建生产包

```bash
npm run build
```

## 本地预览生产包

```bash
npm run preview
```

## 接口代理

前端所有接口统一以 `/api` 开头，开发环境由 Vite 代理到后端：

```js
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8088',
      changeOrigin: true
    }
  }
}
```

## 登录说明

系统登录接口：

```text
POST /api/auth/login
```

登录成功后，前端会保存：

- **Token**：`localStorage.sms_token`
- **用户信息**：`localStorage.sms_user`

默认账号由后端初始化，默认密码为：

```text
123456
```

如登录异常或旧 Token 导致请求失败，可在浏览器控制台执行：

```js
localStorage.clear()
location.reload()
```

## 已接入真实接口的模块

- **登录认证**：`/api/auth/login`
- **首页统计**：`/api/dashboard/stats`
- **老人档案**：`/api/elder`
- **健康预警**：`/api/health-alert`
- **评估报告**：`/api/assessment`
- **设备管理**：`/api/device`
- **用户账号**：`/api/user`
- **医生账号**：`/api/user/list?role=DOCTOR`
- **个人中心**：用户查询、资料修改、密码修改
- **重点人群**：基于老人健康状态统计

## 说明

`Statistics.vue`、`BigData.vue` 当前属于图表展示页，后端暂无专门的统计分析接口，因此部分图表仍为展示数据。如需完全真实化，需要在后端新增对应统计接口。
