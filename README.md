# 笔记工坊

一个给在校学生用的“读书笔记社区”。你可以在这里写下自己的读书笔记，上传书页照片，选择只给自己看还是公开给大家看。其他同学可以浏览、点赞、关注你，也可以评论你的笔记。

系统还带了一个 AI 助手：它不会替你写笔记，而是根据你写的笔记，帮你做总结、推荐适合人群、打分；还有一个 AI 客服可以回答“怎么公开笔记”“审核规则是什么”这类使用问题。

这是软件组三面项目，代码里按后端分层做了用户、笔记、互动、管理员后台和 AI 五个模块。

## 这个项目能做什么

**用户**
- 注册、登录、退出
- 修改昵称、上传头像
- 登录状态用 JWT 校验，密码用 BCrypt 加密，不存明文

**读书笔记**
- 新建、编辑、删除笔记
- 给笔记绑定书名，上传多张图片
- 笔记可以设为“私有”或“公开”
- 公开笔记先进入管理员审核，通过后才会对外展示

**互动**
- 给公开笔记点赞、取消点赞，看自己点过赞的笔记
- 关注作者、取关，看作者的粉丝数和热门作者排名
- 给公开笔记评论，支持多级回复

**管理员后台**
- 查看用户列表，禁用或启用账号
- 审核公开笔记，下架违规笔记
- 删除违规评论
- 查看 AI 调用统计（成功次数、失败次数）

**AI 功能**
- 笔记总结、适宜人群推荐、笔记评分
- 多轮 AI 客服，支持新建会话、切换历史会话
- 没配置 AI 密钥时，会返回一条模拟结果，方便本地演示

## 运行前需要装什么

要跑起来只需要两样：

- JDK 17
- Maven 3.9+

如果你暂时只想在本地体验，**不需要安装 MySQL**。项目默认用 H2 内存数据库，重启后数据会清空。

## 三步跑起来

在项目根目录执行：

```bash
mvn spring-boot:run
```

看到类似 `Started NoteWorkshopApplication` 的日志后，打开浏览器访问：

```text
http://localhost:8080
```

默认管理员账号：

```text
用户名：admin
密码：admin123
```

管理员账号会在程序启动时自动创建，普通用户可以直接在页面上注册。

## 想连真实 MySQL

先执行建表脚本：

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

然后设置环境变量并启动：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://localhost:3306/note?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
mvn spring-boot:run
```

`DB_PASSWORD` 换成你自己的密码。这些变量也可以直接写进 `src/main/resources/application-prod.yml`。

## AI 功能怎么接真实模型

项目通过 OpenAI 兼容接口调用模型，所以 OpenAI、DeepSeek、Qwen 这类服务都能接。设置三个环境变量即可：

```bash
export AI_BASE_URL='https://api.openai.com/v1'
export AI_API_KEY='你的密钥'
export AI_MODEL='gpt-4o-mini'
```

密钥只通过环境变量注入，不要写进代码或提交到仓库。没有配置密钥时，AI 会返回模拟结果，不影响其他功能演示。

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
└── ai            AI 总结、推荐、评分、客服
```

前端页面在 `src/main/resources/static`，是原生 HTML、CSS、JavaScript 写的单页应用。

## 主要接口

接口统一返回 `{ code, message, data }`，`code` 为 `200` 表示成功。

```text
POST   /api/auth/register            注册
POST   /api/auth/login               登录
GET    /api/user/me                  查看当前用户
POST   /api/notes                    新建笔记
GET    /api/notes/my                 我的笔记（分页）
GET    /api/notes                    公开笔记（分页，可匿名访问）
POST   /api/likes/{noteId}           点赞
POST   /api/follows/{userId}         关注
POST   /api/comments                 发表评论
GET    /api/admin/users              管理员查看用户
POST   /api/ai/summary               生成笔记总结
POST   /api/ai/conversations/{id}/messages   AI 客服对话
```

除注册、登录、公开笔记浏览和评论浏览外，其他接口都需要在请求头里带 `Authorization: Bearer <token>`。登录成功后会返回 token。

## 说明

- 本地默认用 H2 内存数据库，重启后数据会重置；真实数据请连 MySQL。
- AI 没配密钥时返回的是模拟内容，不是真实模型结果。
- 上传的图片默认保存在项目的 `uploads` 目录，数据库里只存访问地址。
