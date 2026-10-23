#!/usr/bin/env python3
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080/api"


def call(method, path, data=None, token=None):
    headers = {}
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def login(username, password="123456"):
    result = call("POST", "/auth/login", {"username": username, "password": password})
    return result["data"]["token"] if result.get("code") == 200 else None


def register(username, nickname):
    call("POST", "/auth/register", {"username": username, "password": "123456", "nickname": nickname})


def main():
    users = {"alice": "爱丽丝", "bob": "小明", "carol": "小文", "dave": "小凯"}
    for username, nickname in users.items():
        register(username, nickname)

    admin = login("admin", "admin123")
    tokens = {username: login(username) for username in users}

    def note(user, title, book, content, visibility):
        result = call("POST", "/notes", {
            "title": title, "bookName": book, "content": content, "visibility": visibility
        }, tokens[user])
        return result["data"]["id"]

    notes = {
        "alice_jvm": note("alice", "JVM 内存模型笔记", "深入理解 Java 虚拟机",
                          "记录堆、栈、方法区，以及 Minor GC 和 Full GC 的区别。", 1),
        "alice_spring": note("alice", "SpringBoot 起步笔记", "Spring Boot 实战",
                             "整理了自动配置、starter、yaml 配置和 Controller-Service-Mapper 分层。", 1),
        "alice_private": note("alice", "私下复习草稿", "计算机网络", "还没整理完的私有笔记。", 0),
        "bob_ai": note("bob", "Spring AI 学习笔记", "Spring AI 实战",
                       "学习 ChatClient、Prompt、SystemMessage 和模型配置。", 1),
        "bob_db": note("bob", "MySQL 索引笔记", "高性能 MySQL",
                       "聚簇索引、覆盖索引和最左前缀原则。", 1),
        "carol_read": note("carol", "如何高效阅读", "如何阅读一本书",
                           "主动阅读、提出问题、做输出式笔记。", 1),
        "dave_pending": note("dave", "待审核的公开笔记", "算法",
                             "这是留给管理员审核演示的笔记。", 1),
    }

    for key in ["alice_jvm", "alice_spring", "bob_ai", "bob_db", "carol_read"]:
        call("PUT", f"/admin/notes/{notes[key]}/audit", {"auditStatus": 1}, admin)

    admin_users = call("GET", "/admin/users?page=1&size=20", None, admin)["data"]["records"]
    user_ids = {u["username"]: u["id"] for u in admin_users}

    for user, key in [("bob", "alice_jvm"), ("carol", "alice_jvm"), ("dave", "alice_spring"),
                      ("alice", "bob_ai"), ("carol", "bob_ai"), ("alice", "bob_db"), ("bob", "carol_read")]:
        call("POST", f"/likes/{notes[key]}", None, tokens[user])

    for follower, followee in [("alice", "bob"), ("alice", "carol"), ("bob", "alice"),
                               ("carol", "alice"), ("dave", "bob")]:
        call("POST", f"/follows/{user_ids[followee]}", None, tokens[follower])

    for user, key, content in [
        ("bob", "alice_jvm", "讲得很清楚，GC 那块我再消化一下。"),
        ("carol", "alice_jvm", "同感，尤其是 Minor GC 和 Full GC。"),
        ("alice", "bob_ai", "ChatClient 的用法很实用，谢谢分享。"),
        ("dave", "carol_read", "输出式笔记这个方法很好用。"),
    ]:
        call("POST", "/comments", {"noteId": notes[key], "content": content}, tokens[user])

    call("POST", "/ai/result/save", {
        "noteId": notes["alice_jvm"], "type": "SUMMARY",
        "content": "这篇笔记围绕 JVM 内存区域和 GC 机制做了系统梳理，适合作为复习提纲。"
    }, tokens["alice"])

    print("演示数据灌入完成")


if __name__ == "__main__":
    main()
