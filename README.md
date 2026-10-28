# 笔记工坊

笔记工坊是一个读书笔记社区平台。用户可以录入书籍信息、撰写读书笔记、上传书页照片，并将笔记设为私有或公开。公开笔记可供其他用户浏览、点赞和评论，也可以关注作者并查看其公开笔记。

系统内置 AI 阅读助手，用于辅助而非替代用户思考：可一键生成笔记总结、适宜人群推荐和评分，并由用户手动选择是否保存；同时提供多轮 AI 客服，回答平台使用相关问题。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.5、MyBatis-Plus
- 数据库：MySQL 8，本地开发可使用 H2 内存数据库
- 鉴权：JWT、Spring Security Crypto（BCrypt）
- AI：Spring AI，兼容 OpenAI 格式的模型服务
- 前端：原生 HTML、CSS、JavaScript

## 功能说明

### 用户模块

- 注册、登录与退出
- 修改个人资料、上传头像
- 使用 JWT 进行登录鉴权，密码使用 BCrypt 加密存储

### 读书笔记模块

- 新建、编辑、删除笔记
- 为笔记绑定书名，并上传多张配图
- 笔记支持私有或公开两种可见性
- 公开笔记需经管理员审核通过后方可对外展示

### 互动模块

- 对公开笔记点赞或取消点赞，查看个人点赞记录
- 关注或取消关注作者，查看粉丝数量与热门作者排名
- 对公开笔记发表评论，支持多级回复

### 管理员模块

- 查看用户列表，启用或禁用账号
- 审核公开笔记，下架违规笔记
- 删除违规评论
- 统计平台 AI 调用次数及成功、失败数量

### AI 模块

- 一键生成笔记总结、适宜人群推荐、笔记评分
- 用户确认后再保存 AI 辅助结果，避免自动修改笔记
- 多轮 AI 客服，支持新建会话和切换历史会话
- 未配置 AI 密钥时返回模拟结果，便于本地演示

## 环境要求

- JDK 17
- Maven 3.9+

如仅在本地体验，无需安装 MySQL。项目默认使用 H2 内存数据库，服务重启后数据会重置。

## 快速启动

在项目根目录执行：

```bash
mvn spring-boot:run
```

也可以复制 `.env.example` 为 `.env`，填入自己的模型配置后运行：

```bash
./scripts/run-dev.sh
```

待日志中出现 `Started NoteWorkshopApplication` 后，通过浏览器访问：

```text
http://localhost:8080
```

接口文档可通过 Swagger UI 查看：

```text
http://localhost:8080/swagger-ui.html
```

首次启动会自动创建管理员账号：

```text
用户名：admin
密码：admin123
```

普通用户可在页面上直接注册。

## 使用 MySQL

执行建表脚本：

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

配置环境变量并启动：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://localhost:3306/note?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export DB_USERNAME=root
export DB_PASSWORD=数据库密码
mvn spring-boot:run
```

以上变量也可写入 `src/main/resources/application-prod.yml`。

## 配置 AI 模型

项目通过 OpenAI 兼容接口调用模型，可使用 OpenAI、DeepSeek、Qwen 等服务：

```bash
export AI_BASE_URL='https://api.openai.com'
export AI_API_KEY='模型密钥'
export AI_MODEL='gpt-4o-mini'
```

密钥仅通过环境变量注入，请勿写入代码库。

如果使用图片识别功能，且视觉模型与聊天模型不同，可额外配置：

```bash
export AI_VISION_MODEL='gpt-4o-mini'
```

## 项目结构

```text
src/main/java/com/note
├── common        统一返回、异常、分页、当前登录用户
├── config        密码、MyBatis-Plus、Web、拦截器配置
├── util          JWT 工具、文件存储
├── interceptor   登录鉴权拦截器
├── user          用户与登录注册
├── note          笔记与图片
├── interaction   点赞、关注、评论
├── admin         管理员后台与 AI 审计
└── ai            AI 辅助、客服
```

前端页面位于 `src/main/resources/static`，采用原生 HTML、CSS 和 JavaScript 实现。

## 主要接口

接口统一返回 `{ code, message, data }`，`code` 为 `200` 表示成功。

### 用户与鉴权

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 token |
| GET | `/api/user/me` | 查看当前用户 |
| PUT | `/api/user/me` | 修改昵称、头像 |
| POST | `/api/user/avatar` | 上传头像 |
| PUT | `/api/user/password` | 修改密码 |
| GET | `/api/users/{id}` | 查看公开用户信息 |

### 笔记与文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/notes` | 新建笔记 |
| GET | `/api/notes/my` | 我的笔记（分页、搜索） |
| GET | `/api/notes` | 公开笔记（分页、搜索） |
| GET | `/api/notes/rank` | 笔记点赞数排名 |
| GET | `/api/notes/{id}` | 笔记详情 |
| PUT | `/api/notes/{id}` | 编辑笔记 |
| DELETE | `/api/notes/{id}` | 删除笔记 |
| POST | `/api/notes/{id}/images` | 上传笔记配图 |
| DELETE | `/api/notes/images/{imageId}` | 删除指定配图 |
| DELETE | `/api/notes/{id}/images` | 删除笔记全部配图 |
| GET | `/api/users/{userId}/notes` | 查看用户公开笔记 |
| POST | `/api/files` | 上传文件 |
| GET | `/api/images/{filename}` | 访问图片或视频 |

### 点赞、关注与评论

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/likes/{noteId}` | 点赞 |
| DELETE | `/api/likes/{noteId}` | 取消点赞 |
| GET | `/api/likes/me` | 我点赞过的笔记 |
| GET | `/api/feeds` | 关注动态 |
| POST | `/api/follows/{userId}` | 关注用户 |
| DELETE | `/api/follows/{userId}` | 取消关注 |
| GET | `/api/follows/{userId}/followers` | 粉丝列表 |
| GET | `/api/follows/{userId}/following` | 关注列表 |
| GET | `/api/follows/rank` | 热门作者排名 |
| GET | `/api/follows/me` | 我的关注列表 |
| GET | `/api/users/{userId}/stats` | 用户统计信息 |
| POST | `/api/comments` | 发表评论 |
| GET | `/api/comments/{noteId}` | 评论列表 |
| DELETE | `/api/comments/{id}` | 删除评论 |

### 图书收藏

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/books` | 收藏图书 |
| GET | `/api/books` | 我的收藏图书列表 |
| DELETE | `/api/books/{id}` | 删除收藏图书 |
| POST | `/api/books/recognize` | 上传图片识别图书信息 |

### AI 辅助与客服

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ai/summary` | 生成笔记总结 |
| POST | `/api/ai/recommend` | 生成适宜人群推荐 |
| POST | `/api/ai/score` | 生成笔记评分 |
| POST | `/api/ai/result/save` | 保存 AI 辅助结果 |
| GET | `/api/ai/result/{noteId}` | 查看已保存的 AI 结果 |
| POST | `/api/ai/conversations` | 新建 AI 客服会话 |
| GET | `/api/ai/conversations` | 会话列表 |
| POST | `/api/ai/conversations/{id}/messages` | 发送客服消息 |
| GET | `/api/ai/conversations/{id}/messages` | 查看会话消息 |
| DELETE | `/api/ai/conversations/{id}` | 删除会话 |

### 管理员

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/status` | 启用或禁用用户 |
| GET | `/api/admin/notes` | 笔记列表 |
| PUT | `/api/admin/notes/{id}/audit` | 审核笔记 |
| PUT | `/api/admin/notes/{id}/offline` | 下架笔记 |
| DELETE | `/api/admin/comments/{id}` | 删除违规评论 |
| GET | `/api/admin/ai-stats` | AI 调用统计 |

除注册、登录、公开笔记浏览和评论浏览外，其余接口需在请求头中携带 `Authorization: Bearer <token>`。

## 说明

- 本地默认使用 H2 内存数据库，重启后数据会重置；如需持久化数据请使用 MySQL。
- 未配置 AI 密钥时返回的是模拟内容，并非真实模型结果。
- 上传的图片默认保存在项目的 `uploads` 目录，数据库中仅保存访问地址。
