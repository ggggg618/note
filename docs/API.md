# 接口文档

基础地址：`http://localhost:8080`

除标注“公开”的接口外，其余接口均需携带登录返回的 token：

```text
Authorization: Bearer <token>
```

统一返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

`code` 为 `200` 表示成功，其他值表示失败，`message` 为提示信息。

## 用户与鉴权

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 注册 | 公开 |
| POST | `/api/auth/login` | 登录，返回 token | 公开 |
| GET | `/api/user/me` | 查看当前用户 | 登录 |
| PUT | `/api/user/me` | 修改昵称、头像 | 登录 |
| POST | `/api/user/avatar` | 上传头像（`file`） | 登录 |
| GET | `/api/users/{id}` | 查看公开用户信息 | 公开 |

## 笔记

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/notes` | 新建笔记 | 登录 |
| GET | `/api/notes/my` | 我的笔记（分页） | 登录 |
| GET | `/api/notes` | 公开笔记（分页、关键字搜索） | 公开 |
| GET | `/api/notes/rank` | 笔记点赞数排名（分页） | 公开 |
| GET | `/api/notes/{id}` | 笔记详情 | 作者/管理员或公开 |
| PUT | `/api/notes/{id}` | 编辑笔记 | 作者 |
| DELETE | `/api/notes/{id}` | 删除笔记 | 作者/管理员 |
| POST | `/api/notes/{id}/images` | 上传笔记配图（`files`） | 作者/管理员 |
| GET | `/api/users/{userId}/notes` | 查看某用户公开笔记（分页） | 公开 |

## 互动

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/likes/{noteId}` | 点赞 | 登录 |
| DELETE | `/api/likes/{noteId}` | 取消点赞 | 登录 |
| GET | `/api/likes/me` | 我点赞过的笔记（分页） | 登录 |
| POST | `/api/follows/{userId}` | 关注 | 登录 |
| DELETE | `/api/follows/{userId}` | 取消关注 | 登录 |
| GET | `/api/feeds` | 关注的人的公开笔记流（分页） | 登录 |
| GET | `/api/follows/{userId}/followers` | 粉丝列表（分页） | 登录 |
| GET | `/api/follows/{userId}/following` | 关注列表（分页） | 登录 |
| GET | `/api/follows/rank` | 热门作者排名（分页） | 登录 |
| POST | `/api/comments` | 发表评论 | 登录 |
| GET | `/api/comments/{noteId}` | 评论列表 | 公开 |
| DELETE | `/api/comments/{id}` | 删除评论 | 作者/管理员 |

## 管理员

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/admin/users` | 用户列表（分页、搜索） | 管理员 |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用用户 | 管理员 |
| GET | `/api/admin/notes` | 笔记列表（可按审核状态筛选） | 管理员 |
| PUT | `/api/admin/notes/{id}/audit` | 审核笔记（1通过 2驳回） | 管理员 |
| PUT | `/api/admin/notes/{id}/offline` | 下架笔记 | 管理员 |
| DELETE | `/api/admin/comments/{id}` | 删除违规评论 | 管理员 |
| GET | `/api/admin/ai-stats` | AI 调用统计 | 管理员 |

## AI

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/ai/summary` | 笔记总结 | 登录且可查看该笔记 |
| POST | `/api/ai/recommend` | 适宜人群推荐 | 登录且可查看该笔记 |
| POST | `/api/ai/score` | 笔记评分 | 登录且可查看该笔记 |
| POST | `/api/ai/result/save` | 保存 AI 结果 | 作者 |
| GET | `/api/ai/result/{noteId}` | 查看已保存的 AI 结果 | 作者 |
| POST | `/api/ai/conversations` | 新建会话 | 登录 |
| GET | `/api/ai/conversations` | 我的会话列表 | 登录 |
| POST | `/api/ai/conversations/{id}/messages` | 发送消息 | 会话归属用户 |
| GET | `/api/ai/conversations/{id}/messages` | 消息列表 | 会话归属用户 |
| DELETE | `/api/ai/conversations/{id}` | 删除会话 | 会话归属用户 |

AI 辅助结果采用“一篇笔记只保存一种结果”的规则。前端会将总结、适宜人群推荐和评分合并为一个 `MIXED` 结果，用户确认后再保存；再次保存时会覆盖该笔记之前保存的 AI 结果。

## 请求示例

注册：

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"tom","password":"123456","nickname":"Tom"}'
```

登录：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"tom","password":"123456"}'
```

新建笔记：

```bash
curl -X POST http://localhost:8080/api/notes \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"title":"标题","content":"正文","bookName":"书名","visibility":1}'
```
