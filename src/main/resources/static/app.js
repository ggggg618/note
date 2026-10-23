const $ = (sel) => document.querySelector(sel);
const API = "/api";
const token = () => localStorage.getItem("token") || "";
const headers = (json) => {
    const h = { "Content-Type": "application/json" };
    if (token()) h.Authorization = "Bearer " + token();
    return h;
};
const authHeaders = () => {
    const h = {};
    if (token()) h.Authorization = "Bearer " + token();
    return h;
};

function notify(message) {
    const toast = document.createElement("div");
    toast.className = "toast";
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

function askText(message) {
    return new Promise((resolve) => {
        const overlay = document.createElement("div");
        overlay.className = "dialog-overlay";
        const box = document.createElement("div");
        box.className = "dialog-box";
        const label = document.createElement("div");
        label.className = "dialog-label";
        label.textContent = message;
        const input = document.createElement("input");
        input.type = "text";
        const ok = document.createElement("button");
        ok.type = "button";
        ok.textContent = "确定";
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.textContent = "取消";
        const row = document.createElement("div");
        row.className = "dialog-actions";
        row.append(cancel, ok);
        box.append(label, input, row);
        overlay.appendChild(box);
        document.body.appendChild(overlay);

        const close = (value) => {
            overlay.remove();
            resolve(value);
        };
        ok.addEventListener("click", () => close(input.value.trim()));
        cancel.addEventListener("click", () => close(null));
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") close(input.value.trim());
        });
        input.focus();
    });
}

function confirmAction(message) {
    return new Promise((resolve) => {
        const overlay = document.createElement("div");
        overlay.className = "dialog-overlay";
        const box = document.createElement("div");
        box.className = "dialog-box";
        const label = document.createElement("div");
        label.className = "dialog-label";
        label.textContent = message;
        const ok = document.createElement("button");
        ok.type = "button";
        ok.textContent = "确定";
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.textContent = "取消";
        const row = document.createElement("div");
        row.className = "dialog-actions";
        row.append(cancel, ok);
        box.append(label, row);
        overlay.appendChild(box);
        document.body.appendChild(overlay);
        const close = (value) => {
            overlay.remove();
            resolve(value);
        };
        ok.addEventListener("click", () => close(true));
        cancel.addEventListener("click", () => close(false));
    });
}

async function ensureCurrentUser() {
    if (!token()) {
        currentUserId = null;
        return;
    }
    if (currentUserId != null) return;
    try {
        const me = await request("/user/me");
        currentUserId = me.id;
    } catch (e) {
        currentUserId = null;
    }
}

async function loadLikedNotes() {
    if (!token()) {
        likedNoteIds = new Set();
        return;
    }
    try {
        const ids = [];
        let page = 1;
        while (true) {
            const data = await request(`/likes/me?page=${page}&size=100`);
            ids.push(...(data.records || []).map((n) => n.id));
            if (!data.records || data.records.length < 100) break;
            page++;
        }
        likedNoteIds = new Set(ids);
    } catch (e) {
        likedNoteIds = new Set();
    }
}

async function loadFollowingIds() {
    if (!token()) {
        followingIds = new Set();
        return;
    }
    try {
        const data = await request("/follows/me");
        followingIds = new Set(Array.isArray(data) ? data : []);
    } catch (e) {
        followingIds = new Set();
    }
}

async function request(path, options = {}) {
    options = options || {};
    options.headers = { ...authHeaders(), ...(options.headers || {}) };
    const res = await fetch(API + path, options);
    if (res.status === 401) {
        clearSession();
        showAuth();
        throw new Error("登录已过期");
    }
    const body = await res.json();
    if (!res.ok || body.code !== 200) throw new Error(body.message || "请求失败");
    return body.data;
}

function showView(name) {
    document.querySelectorAll(".view").forEach((v) => v.classList.add("hidden"));
    $("#" + name + "-view").classList.remove("hidden");
    if (pageState[name] != null) pageState[name] = 1;
    if (name === "notes") loadMyNotes();
    if (name === "public") loadPublicNotes();
    if (name === "feeds") loadFeeds();
    if (name === "likes") loadMyLikes();
    if (name === "authors") loadHotAuthors();
    if (name === "rank") loadRankedNotes();
    if (name === "chat") loadChat();
    if (name === "profile") loadProfile();
    if (name === "admin") {
        pageState.adminUsers = 1;
        pageState.adminNotes = 1;
        loadAdmin();
    }
}

function showAuth() {
    document.querySelectorAll(".view").forEach((v) => v.classList.add("hidden"));
    $("#auth-view").classList.remove("hidden");
}

document.querySelectorAll("[data-view]").forEach((btn) => {
    btn.addEventListener("click", () => showView(btn.dataset.view));
});

let registerMode = false;
let currentConversationId = null;
let detailNoteId = null;
let currentUserId = null;
let currentAuthorId = null;
let replyToCommentId = null;
let editingNoteId = null;
let likedNoteIds = new Set();
let followingIds = new Set();
const PAGE_SIZE = 10;
const pageState = { notes: 1, public: 1, feeds: 1, likes: 1, authors: 1, rank: 1, adminUsers: 1, adminNotes: 1 };
$("#toggle-auth").addEventListener("click", () => {
    registerMode = !registerMode;
    renderAuthMode();
});

function renderAuthMode() {
    $("#auth-title").textContent = registerMode ? "注册" : "登录";
    $("#nickname-field").classList.toggle("hidden", !registerMode);
    $("#toggle-auth").textContent = registerMode ? "已有账号？去登录" : "没有账号？去注册";
}

$("#auth-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = { username: $("#username").value, password: $("#password").value };
    if (registerMode) payload.nickname = $("#nickname").value;
    const path = registerMode ? "/auth/register" : "/auth/login";
    const res = await fetch(API + path, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
    const body = await res.json();
    if (body.code !== 200) return notify(body.message);
    if (registerMode) {
        const loginRes = await fetch(API + "/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: payload.username, password: payload.password })
        });
        const loginBody = await loginRes.json();
        registerMode = false;
        renderAuthMode();
        if (loginBody.code === 200) {
            localStorage.setItem("token", loginBody.data.token);
            localStorage.setItem("role", loginBody.data.user.role);
            currentUserId = loginBody.data.user.id;
            afterAuth();
            showView("notes");
        } else {
            notify("注册成功，请登录");
            showAuth();
        }
        return;
    }
    localStorage.setItem("token", body.data.token);
    localStorage.setItem("role", body.data.user.role);
    currentUserId = body.data.user.id;
    afterAuth();
    showView("notes");
});

function afterAuth() {
    const logged = !!token();
    $("#auth-view").classList.toggle("hidden", logged);
    $("#logout").classList.toggle("hidden", !logged);
    const isAdmin = localStorage.getItem("role") === "ADMIN";
    document.querySelectorAll(".admin-only").forEach((b) => b.classList.toggle("hidden", !(logged && isAdmin)));
    document.querySelectorAll("[data-view]:not(.admin-only)").forEach((b) => {
        if (logged) {
            b.classList.remove("hidden");
        } else {
            b.classList.toggle("hidden", b.dataset.view !== "public");
        }
    });
    if (logged) {
        loadLikedNotes();
        loadFollowingIds();
    }
}

function logout() {
    clearSession();
    showAuth();
}

function clearSession() {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    currentUserId = null;
    likedNoteIds = new Set();
    afterAuth();
}
$("#logout").addEventListener("click", logout);

function noteCard(note) {
    const el = document.createElement("div");
    el.className = "note-card";
    const likeCount = note.likeCount == null ? 0 : note.likeCount;
    const isPrivate = note.visibility === 0;
    el.innerHTML = `<h3>${escapeHtml(note.title)}</h3><p>${escapeHtml(note.bookName || "")}</p><p>${escapeHtml((note.content || "").slice(0, 80))}</p>${isPrivate ? "" : `<div class="note-meta">赞 ${likeCount}</div>`}`;
    el.style.cursor = "pointer";
    el.addEventListener("click", () => openNoteDetail(note.id));

    const actions = document.createElement("div");
    actions.className = "note-actions";
    const isOwn = currentUserId != null && note.userId != null && note.userId === currentUserId;
    let displayedLikeCount = likeCount;

    if (!isPrivate) {
        const likeBtn = actionButton(likeLabel(likedNoteIds.has(note.id)), async () => {
            try {
                const wasLiked = likedNoteIds.has(note.id);
                if (wasLiked) {
                    await request(`/likes/${note.id}`, { method: "DELETE" });
                    likedNoteIds.delete(note.id);
                    displayedLikeCount = Math.max(0, displayedLikeCount - 1);
                    notify("已取消点赞");
                } else {
                    await request(`/likes/${note.id}`, { method: "POST" });
                    likedNoteIds.add(note.id);
                    displayedLikeCount += 1;
                    notify("点赞成功");
                }
                likeBtn.textContent = likeLabel(likedNoteIds.has(note.id));
                el.querySelector(".note-meta").textContent = `赞 ${displayedLikeCount}`;
            } catch (e) { notify(e.message); }
        });
        actions.appendChild(likeBtn);
        actions.appendChild(actionButton("评论", async () => {
            const content = await askText("输入评论内容");
            if (!content) return;
            try {
                await request("/comments", { method: "POST", headers: headers(), body: JSON.stringify({ noteId: note.id, content }) });
                notify("评论成功");
            } catch (e) { notify(e.message); }
        }));
    }
    if (!isOwn && note.userId != null) {
        actions.appendChild(actionButton("作者主页", () => openAuthorPage(note.userId)));
        actions.appendChild(makeFollowButton(note.userId));
    }
    if (isOwn) {
        actions.appendChild(actionButton("编辑", () => openEditor(note)));
        actions.appendChild(actionButton("删除", async () => {
            if (!(await confirmAction("确定删除这篇笔记吗？"))) return;
            try {
                await request(`/notes/${note.id}`, { method: "DELETE" });
                notify("已删除");
                showView("notes");
            } catch (e) { notify(e.message); }
        }));
        actions.appendChild(actionButton("AI 总结", () => runAiSummary(note.id, false)));
        actions.appendChild(actionButton("AI 推荐", () => runAiAction("/ai/recommend", note.id, "RECOMMEND")));
        actions.appendChild(actionButton("AI 评分", () => runAiAction("/ai/score", note.id, "SCORE")));
    }
    el.appendChild(actions);
    return el;
}

function actionButton(text, onClick) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.textContent = text;
    btn.addEventListener("click", (e) => e.stopPropagation());
    btn.addEventListener("click", onClick);
    return btn;
}

function likeLabel(liked) {
    return liked ? "♥ 已赞" : "♡ 点赞";
}

function makeFollowButton(targetId, onChanged) {
    const following = followingIds.has(targetId);
    const btn = actionButton(following ? "已关注" : "关注", async () => {
        try {
            if (followingIds.has(targetId)) {
                await request(`/follows/${targetId}`, { method: "DELETE" });
                followingIds.delete(targetId);
                notify("已取消关注");
            } else {
                await request(`/follows/${targetId}`, { method: "POST" });
                followingIds.add(targetId);
                notify("已关注");
            }
            btn.textContent = followingIds.has(targetId) ? "已关注" : "关注";
            if (onChanged) onChanged();
        } catch (e) { notify(e.message); }
    });
    return btn;
}

async function runAiSummary(noteId, reloadDetail) {
    try {
        const data = await request("/ai/summary", { method: "POST", headers: headers(), body: JSON.stringify({ noteId }) });
        await request("/ai/result/save", { method: "POST", headers: headers(), body: JSON.stringify({ noteId, type: "SUMMARY", content: data.content }) });
        notify("AI 总结已保存");
        if (reloadDetail) loadNoteDetail();
    } catch (e) { notify(e.message); }
}

async function runAiAction(path, noteId, type) {
    try {
        const data = await request(path, { method: "POST", headers: headers(), body: JSON.stringify({ noteId }) });
        await request("/ai/result/save", { method: "POST", headers: headers(), body: JSON.stringify({ noteId, type, content: data.content }) });
        notify(`${type === "RECOMMEND" ? "AI 推荐" : "AI 评分"}已保存`);
    } catch (e) { notify(e.message); }
}

function showDetail() {
    document.querySelectorAll(".view").forEach((v) => v.classList.add("hidden"));
    const view = $("#detail-view");
    if (view) view.classList.remove("hidden");
}

function openNoteDetail(id) {
    detailNoteId = id;
    showDetail();
    loadNoteDetail();
}

async function loadNoteDetail() {
    try {
        await ensureCurrentUser();
        const note = await request(`/notes/${detailNoteId}`);
        const isPrivate = note.visibility === 0;
        const author = await request(`/users/${note.userId}`);
        $("#detail-title").textContent = note.title;
        $("#detail-meta").innerHTML = `<button class="link-btn" id="detail-author">${escapeHtml(author.nickname || author.username)}</button> · ${formatDate(note.createdAt)}${isPrivate ? "" : ` · 点赞 ${note.likeCount}`}`;
        $("#detail-author").addEventListener("click", () => openAuthorPage(note.userId));
        $("#detail-book").textContent = "书名：" + (note.bookName || "");
        $("#detail-content").textContent = note.content || "";

        const imgWrap = $("#detail-images");
        imgWrap.innerHTML = "";
        (note.images || []).forEach((src) => {
            const img = document.createElement("img");
            img.src = src;
            img.alt = "配图";
            imgWrap.appendChild(img);
        });

        const aiParts = [];
        if (note.aiSummary) aiParts.push(`<p><b>AI 总结：</b>${escapeHtml(note.aiSummary)}</p>`);
        if (note.aiRecommend) aiParts.push(`<p><b>AI 推荐：</b>${escapeHtml(note.aiRecommend)}</p>`);
        if (note.aiScore) aiParts.push(`<p><b>AI 评分：</b>${escapeHtml(note.aiScore)}</p>`);
        $("#detail-ai").innerHTML = aiParts.length ? aiParts.join("") : "暂无 AI 结果";
        document.querySelectorAll("#detail-view h3, #detail-comments, #detail-view .comment-form")
            .forEach((el) => el.classList.toggle("hidden", isPrivate));
        renderDetailActions(note);
        await loadComments(note.id);
    } catch (e) {
        notify(e.message);
        showView("public");
    }
}

function renderDetailActions(note) {
    const actions = $("#detail-actions");
    actions.innerHTML = "";
    const isOwn = currentUserId != null && note.userId != null && note.userId === currentUserId;
    const isPrivate = note.visibility === 0;
    if (!isPrivate) {
        const likeBtn = actionButton(likeLabel(likedNoteIds.has(note.id)), async () => {
            try {
                const wasLiked = likedNoteIds.has(note.id);
                if (wasLiked) {
                    await request(`/likes/${note.id}`, { method: "DELETE" });
                    likedNoteIds.delete(note.id);
                    notify("已取消点赞");
                } else {
                    await request(`/likes/${note.id}`, { method: "POST" });
                    likedNoteIds.add(note.id);
                    notify("点赞成功");
                }
                likeBtn.textContent = likeLabel(likedNoteIds.has(note.id));
                loadNoteDetail();
            } catch (e) { notify(e.message); }
        });
        actions.appendChild(likeBtn);
    }
    if (!isOwn && note.userId != null) {
        actions.appendChild(makeFollowButton(note.userId));
    }
    if (isOwn) {
        actions.appendChild(actionButton("编辑", () => openEditor(note)));
        actions.appendChild(actionButton("删除", async () => {
            if (!(await confirmAction("确定删除这篇笔记吗？"))) return;
            try {
                await request(`/notes/${note.id}`, { method: "DELETE" });
                notify("已删除");
                showView("notes");
            } catch (e) { notify(e.message); }
        }));
        actions.appendChild(actionButton("AI 总结", () => runAiSummary(note.id, true)));
        actions.appendChild(actionButton("AI 推荐", () => runAiAction("/ai/recommend", note.id, "RECOMMEND")));
        actions.appendChild(actionButton("AI 评分", () => runAiAction("/ai/score", note.id, "SCORE")));
    }
}

async function loadComments(noteId) {
    const container = $("#detail-comments");
    container.innerHTML = "";
    const comments = await request(`/comments/${noteId}`);
    if (!Array.isArray(comments) || comments.length === 0) {
        container.textContent = "暂无评论";
        return;
    }
    const ids = new Set();
    comments.forEach((c) => {
        ids.add(c.userId);
        (c.children || []).forEach((child) => ids.add(child.userId));
    });
    const authors = {};
    await Promise.all([...ids].map(async (id) => {
        try { authors[id] = await request(`/users/${id}`); } catch (e) {}
    }));
    comments.forEach((c) => renderComment(c, authors, container));
}

function renderComment(comment, authors, parent) {
    const div = document.createElement("div");
    div.className = "comment";
    const author = authors[comment.userId] || {};
    const head = document.createElement("div");
    head.className = "comment-head";
    head.textContent = (author.nickname || author.username || ("用户" + comment.userId)) + " · " + formatDate(comment.createdAt);
    const reply = document.createElement("button");
    reply.type = "button";
    reply.className = "comment-reply";
    reply.textContent = "回复";
    reply.addEventListener("click", () => startReply(comment.id, author.nickname || author.username));
    head.appendChild(reply);
    if (currentUserId === comment.userId || localStorage.getItem("role") === "ADMIN") {
        const del = document.createElement("button");
        del.type = "button";
        del.className = "comment-reply";
        del.textContent = "删除";
        del.addEventListener("click", async () => {
            if (!(await confirmAction("确定删除这条评论吗？"))) return;
            try {
                await request(`/comments/${comment.id}`, { method: "DELETE" });
                notify("已删除");
                await loadComments(detailNoteId);
            } catch (e) { notify(e.message); }
        });
        head.appendChild(del);
    }
    const body = document.createElement("div");
    body.className = "comment-body";
    body.textContent = comment.content || "";
    div.append(head, body);
    renderCommentMedia(comment.media, div);
    const children = comment.children || [];
    if (children.length > 0) {
        const childrenWrap = document.createElement("div");
        childrenWrap.className = "comment-children";
        children.forEach((child) => renderComment(child, authors, childrenWrap));
        const foldBtn = document.createElement("button");
        foldBtn.type = "button";
        foldBtn.className = "comment-fold";
        foldBtn.textContent = "收起回复";
        foldBtn.addEventListener("click", () => {
            const hidden = childrenWrap.classList.toggle("hidden");
            foldBtn.textContent = hidden ? "展开回复" : "收起回复";
        });
        div.append(foldBtn, childrenWrap);
    }
    parent.appendChild(div);
}

function renderCommentMedia(media, container) {
    (media || []).forEach((url) => {
        const lower = String(url).toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".m4v")) {
            const video = document.createElement("video");
            video.src = url;
            video.controls = true;
            video.className = "comment-media";
            container.appendChild(video);
        } else {
            const img = document.createElement("img");
            img.src = url;
            img.alt = "评论图片";
            img.className = "comment-media";
            container.appendChild(img);
        }
    });
}

function startReply(commentId, nickname) {
    replyToCommentId = commentId;
    const hint = $("#detail-reply-hint");
    hint.classList.remove("hidden");
    hint.innerHTML = `回复 @${escapeHtml(nickname)} <button type="button" id="cancel-reply">取消</button>`;
    $("#cancel-reply").addEventListener("click", clearReply);
    $("#detail-comment-input").focus();
}

function clearReply() {
    replyToCommentId = null;
    const hint = $("#detail-reply-hint");
    hint.classList.add("hidden");
    hint.innerHTML = "";
}

function formatDate(value) {
    if (!value) return "";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString("zh-CN", { hour12: false });
}

function showAuthor() {
    document.querySelectorAll(".view").forEach((v) => v.classList.add("hidden"));
    $("#author-view").classList.remove("hidden");
}

function openAuthorPage(userId) {
    currentAuthorId = userId;
    showAuthor();
    loadAuthorPage();
}

async function loadAuthorPage() {
    try {
        await ensureCurrentUser();
        const author = await request(`/users/${currentAuthorId}`);
        const stats = await request(`/users/${currentAuthorId}/stats`);
        const notes = await request(`/users/${currentAuthorId}/notes?page=1&size=50`);

        $("#author-name").textContent = author.nickname || author.username;
        const avatar = $("#author-avatar");
        avatar.innerHTML = "";
        if (author.avatar) {
            const img = document.createElement("img");
            img.src = author.avatar;
            img.alt = "头像";
            avatar.appendChild(img);
        } else {
            avatar.textContent = (author.nickname || author.username).slice(0, 1);
        }
        $("#author-stats").innerHTML = `笔记 ${stats.noteCount} · 粉丝 ${stats.followerCount} · 关注 ${stats.followingCount}`;

        const followWrap = $("#author-follow");
        followWrap.innerHTML = "";
        if (currentUserId != null && currentUserId !== currentAuthorId) {
            followWrap.appendChild(makeFollowButton(currentAuthorId, loadAuthorPage));
        }

        const list = $("#author-notes");
        list.innerHTML = "";
        (notes.records || []).forEach((n) => list.appendChild(noteCard(n)));
    } catch (e) {
        notify(e.message);
        showView("public");
    }
}

async function loadMyNotes() {
    await ensureCurrentUser();
    await loadLikedNotes();
    await loadFollowingIds();
    await loadPaged("notes", "#notes-list", "#notes-more", "/notes/my", "你还没有写过笔记，点右上角“新建笔记”开始吧", (n) => $("#notes-list").appendChild(noteCard(n)));
}

async function loadPublicNotes() {
    await ensureCurrentUser();
    await loadLikedNotes();
    await loadFollowingIds();
    const keyword = $("#public-search")?.value.trim() || "";
    const path = keyword ? `/notes?keyword=${encodeURIComponent(keyword)}` : "/notes";
    await loadPaged("public", "#public-list", "#public-more", path, "这里还没有公开笔记", (n) => $("#public-list").appendChild(noteCard(n)));
}

async function loadFeeds() {
    await ensureCurrentUser();
    await loadLikedNotes();
    await loadFollowingIds();
    await loadPaged("feeds", "#feeds-list", "#feeds-more", "/feeds", "关注作者后，他们的公开笔记会显示在这里", (n) => $("#feeds-list").appendChild(noteCard(n)));
}

async function loadMyLikes() {
    await ensureCurrentUser();
    await loadLikedNotes();
    await loadFollowingIds();
    await loadPaged("likes", "#likes-list", "#likes-more", "/likes/me", "你还没有点赞过笔记", (n) => $("#likes-list").appendChild(noteCard(n)));
}

async function loadHotAuthors() {
    await ensureCurrentUser();
    await loadFollowingIds();
    await loadPaged("authors", "#authors-list", "#authors-more", "/follows/rank", "还没有热门作者", (a) => $("#authors-list").appendChild(authorCard(a)));
}

async function loadRankedNotes() {
    await ensureCurrentUser();
    await loadLikedNotes();
    await loadFollowingIds();
    await loadPaged("rank", "#rank-list", "#rank-more", "/notes/rank", "还没有热门笔记", (n) => $("#rank-list").appendChild(noteCard(n)));
}

function renderEmpty(container, message) {
    const div = document.createElement("div");
    div.className = "empty";
    div.textContent = message;
    container.appendChild(div);
}

async function loadPaged(name, listSel, moreSel, path, emptyMsg, renderFn) {
    const page = pageState[name];
    const sep = path.includes("?") ? "&" : "?";
    const data = await request(`${path}${sep}page=${page}&size=${PAGE_SIZE}`);
    const list = $(listSel);
    if (page === 1) list.innerHTML = "";
    const records = data.records || [];
    if (records.length === 0) {
        if (page === 1) renderEmpty(list, emptyMsg);
        $(moreSel).classList.add("hidden");
        return;
    }
    records.forEach(renderFn);
    const hasMore = records.length === PAGE_SIZE && (data.total == null || page * PAGE_SIZE < data.total);
    $(moreSel).classList.toggle("hidden", !hasMore);
}

function authorCard(author) {
    const el = document.createElement("div");
    el.className = "note-card author-card";
    el.innerHTML = `<h3>${escapeHtml(author.nickname || author.username)}</h3><p>笔记 ${author.noteCount} · 粉丝 ${author.followerCount}</p>`;
    const actions = document.createElement("div");
    actions.className = "note-actions";
    actions.appendChild(actionButton("查看主页", () => openAuthorPage(author.id)));
    if (currentUserId != null && currentUserId !== author.id) {
        actions.appendChild(makeFollowButton(author.id));
    }
    el.appendChild(actions);
    return el;
}

async function loadChat() {
    const data = await request("/ai/conversations");
    const conversations = Array.isArray(data) ? data : (data.records || []);
    $("#chat-list").innerHTML = "";
    if (conversations.length === 0) {
        renderEmpty($("#chat-list"), "还没有会话");
    }
    conversations.forEach((conversation) => {
        const wrap = document.createElement("div");
        wrap.className = "conversation-item-wrap";
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "conversation-item";
        btn.textContent = conversation.title || "新会话";
        btn.addEventListener("click", () => {
            currentConversationId = conversation.id;
            document.querySelectorAll(".conversation-item").forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            loadMessages();
        });
        if (conversation.id === currentConversationId) btn.classList.add("active");
        const del = document.createElement("button");
        del.type = "button";
        del.className = "conversation-delete";
        del.textContent = "删除";
        del.addEventListener("click", async () => {
            if (!(await confirmAction("确定删除这个会话吗？"))) return;
            try {
                await request(`/ai/conversations/${conversation.id}`, { method: "DELETE" });
                if (currentConversationId === conversation.id) {
                    currentConversationId = null;
                    $("#chat-messages").innerHTML = "";
                }
                notify("已删除");
                await loadChat();
            } catch (e) { notify(e.message); }
        });
        wrap.append(btn, del);
        $("#chat-list").appendChild(wrap);
    });
    if (!currentConversationId && conversations.length > 0) {
        currentConversationId = conversations[0].id;
        document.querySelector(".conversation-item")?.classList.add("active");
        loadMessages();
    }
}

async function loadMessages() {
    if (!currentConversationId) {
        $("#chat-messages").innerHTML = "";
        return;
    }
    const data = await request(`/ai/conversations/${currentConversationId}/messages`);
    const messages = Array.isArray(data) ? data : (data.records || []);
    $("#chat-messages").innerHTML = "";
    if (messages.length === 0) {
        renderEmpty($("#chat-messages"), "输入问题开始咨询");
    }
    messages.forEach((message) => {
        const role = String(message.role || "assistant").toLowerCase();
        const content = message.content || "";
        const item = document.createElement("div");
        item.className = `chat-message ${role === "user" ? "user" : "assistant"}`;
        const roleLabel = role === "user" ? "用户" : "助手";
        item.innerHTML = `<div class="chat-role">${roleLabel}</div><div class="chat-content">${escapeHtml(content)}</div>`;
        $("#chat-messages").appendChild(item);
    });
    $("#chat-messages").scrollTop = $("#chat-messages").scrollHeight;
}

function openEditor(note) {
    editingNoteId = note ? note.id : null;
    $("#note-form").reset();
    $("#editor-title").textContent = note ? "编辑笔记" : "新建笔记";
    $("#note-title").value = note ? (note.title || "") : "";
    $("#note-book").value = note ? (note.bookName || "") : "";
    $("#note-content").value = note ? (note.content || "") : "";
    $("#note-visibility").value = note && note.visibility != null ? note.visibility : 0;
    $("#note-images").value = "";
    renderEditorImages(note ? (note.images || []) : []);
    showView("editor");
}

function renderEditorImages(images) {
    const box = $("#editor-images");
    box.innerHTML = "";
    images.forEach((url) => {
        const wrap = document.createElement("div");
        wrap.className = "editor-image";
        const img = document.createElement("img");
        img.src = url;
        img.alt = "配图";
        const del = document.createElement("button");
        del.type = "button";
        del.textContent = "删除";
        del.addEventListener("click", async () => {
            try {
                await request(`/notes/${editingNoteId}/images?url=${encodeURIComponent(url)}`, { method: "DELETE" });
                wrap.remove();
                notify("已删除");
            } catch (e) { notify(e.message); }
        });
        wrap.append(img, del);
        box.appendChild(wrap);
    });
}

$("#new-note").addEventListener("click", () => openEditor(null));
$("#cancel-editor").addEventListener("click", () => showView("notes"));
$("#note-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        title: $("#note-title").value,
        content: $("#note-content").value,
        bookName: $("#note-book").value,
        visibility: Number($("#note-visibility").value)
    };
    const path = editingNoteId ? `/notes/${editingNoteId}` : "/notes";
    const data = await request(path, { method: editingNoteId ? "PUT" : "POST", headers: headers(), body: JSON.stringify(payload) });
    const files = $("#note-images").files;
    if (files.length) {
        const fd = new FormData();
        for (const f of files) fd.append("files", f);
        await fetch(API + "/notes/" + data.id + "/images", { method: "POST", headers: authHeaders(), body: fd });
    }
    editingNoteId = null;
    showView("notes");
});

async function loadAdmin() {
    const stats = await request("/admin/ai-stats");
    $("#ai-total").textContent = stats.totalCalls;
    $("#ai-success").textContent = stats.successCalls;
    $("#ai-failed").textContent = stats.failedCalls;

    const userPage = pageState.adminUsers;
    const users = await request(`/admin/users?page=${userPage}&size=${PAGE_SIZE}`);
    if (userPage === 1) $("#admin-users").innerHTML = "";
    (users.records || []).forEach((u) => {
        const row = document.createElement("div");
        row.className = "row";
        row.innerHTML = `<span>${escapeHtml(u.username)} · ${escapeHtml(u.nickname || "")}</span>`;
        const btn = document.createElement("button");
        btn.textContent = u.status === 1 ? "禁用" : "启用";
        btn.onclick = async () => {
            await request(`/admin/users/${u.id}/status`, { method: "PUT", headers: headers(), body: JSON.stringify({ status: u.status === 1 ? 0 : 1 }) });
            loadAdmin();
        };
        row.appendChild(btn);
        $("#admin-users").appendChild(row);
    });
    $("#admin-users-more").classList.toggle("hidden", (users.records || []).length < PAGE_SIZE);

    const notePage = pageState.adminNotes;
    const notes = await request(`/admin/notes?page=${notePage}&size=${PAGE_SIZE}`);
    if (notePage === 1) $("#admin-notes").innerHTML = "";
    (notes.records || []).forEach((n) => {
        const row = document.createElement("div");
        row.className = "row";
        row.innerHTML = `<span>${escapeHtml(n.title)} · ${auditLabel(n.auditStatus)}</span>`;
        const actions = document.createElement("span");
        actions.className = "row-actions";
        const view = document.createElement("button");
        view.textContent = "查看正文";
        view.onclick = () => {
            const existed = row.querySelector(".admin-note-content");
            if (existed) {
                existed.remove();
                view.textContent = "查看正文";
            } else {
                const content = document.createElement("div");
                content.className = "admin-note-content";
                content.textContent = n.content || "（无正文）";
                row.appendChild(content);
                view.textContent = "收起正文";
            }
        };
        actions.appendChild(view);
        if (n.auditStatus !== 1) {
            const pass = document.createElement("button");
            pass.textContent = "通过";
            pass.onclick = async () => {
                await request(`/admin/notes/${n.id}/audit`, { method: "PUT", headers: headers(), body: JSON.stringify({ auditStatus: 1 }) });
                loadAdmin();
            };
            actions.appendChild(pass);
            const reject = document.createElement("button");
            reject.textContent = "拒绝";
            reject.onclick = async () => {
                await request(`/admin/notes/${n.id}/audit`, { method: "PUT", headers: headers(), body: JSON.stringify({ auditStatus: 2 }) });
                loadAdmin();
            };
            actions.appendChild(reject);
        } else {
            const offline = document.createElement("button");
            offline.textContent = "下架";
            offline.onclick = async () => {
                await request(`/admin/notes/${n.id}/offline`, { method: "PUT", headers: headers() });
                loadAdmin();
            };
            actions.appendChild(offline);
        }
        row.appendChild(actions);
        $("#admin-notes").appendChild(row);
    });
    $("#admin-notes-more").classList.toggle("hidden", (notes.records || []).length < PAGE_SIZE);
}

function auditLabel(status) {
    if (status === 1) return "已通过";
    if (status === 2) return "已拒绝";
    return "待审核";
}

async function loadProfile() {
    await ensureCurrentUser();
    const me = await request("/user/me");
    $("#profile-nickname").value = me.nickname || "";
    renderProfileAvatar(me.avatar, me.nickname || me.username);
}

function renderProfileAvatar(avatar, name) {
    const box = $("#profile-avatar");
    box.innerHTML = "";
    if (avatar) {
        const img = document.createElement("img");
        img.src = avatar;
        img.alt = "头像";
        box.appendChild(img);
    } else {
        box.textContent = (name || "?").slice(0, 1);
    }
}

$("#profile-save").addEventListener("click", async () => {
    try {
        await request("/user/me", { method: "PUT", headers: headers(), body: JSON.stringify({ nickname: $("#profile-nickname").value }) });
        notify("昵称已保存");
    } catch (e) { notify(e.message); }
});

$("#profile-avatar-file").addEventListener("change", async () => {
    const file = $("#profile-avatar-file").files[0];
    if (!file) return;
    const fd = new FormData();
    fd.append("file", file);
    try {
        const res = await fetch(API + "/user/avatar", { method: "POST", headers: authHeaders(), body: fd });
        const body = await res.json();
        if (body.code !== 200) throw new Error(body.message || "上传失败");
        notify("头像已更新");
        loadProfile();
    } catch (e) { notify(e.message); }
});

$("#profile-password-save").addEventListener("click", async () => {
    try {
        await request("/user/password", {
            method: "PUT",
            headers: headers(),
            body: JSON.stringify({
                oldPassword: $("#profile-old-password").value,
                newPassword: $("#profile-new-password").value
            })
        });
        notify("密码已修改");
        $("#profile-old-password").value = "";
        $("#profile-new-password").value = "";
    } catch (e) { notify(e.message); }
});

$("#public-search-btn").addEventListener("click", () => {
    pageState.public = 1;
    loadPublicNotes();
});

function bindMore(name, moreSel, fn) {
    $(moreSel).addEventListener("click", () => {
        pageState[name]++;
        fn();
    });
}
bindMore("notes", "#notes-more", loadMyNotes);
bindMore("public", "#public-more", loadPublicNotes);
bindMore("rank", "#rank-more", loadRankedNotes);
bindMore("feeds", "#feeds-more", loadFeeds);
bindMore("likes", "#likes-more", loadMyLikes);
bindMore("authors", "#authors-more", loadHotAuthors);
$("#admin-users-more").addEventListener("click", () => {
    pageState.adminUsers++;
    loadAdmin();
});
$("#admin-notes-more").addEventListener("click", () => {
    pageState.adminNotes++;
    loadAdmin();
});

$("#new-chat").addEventListener("click", async () => {
    try {
        const conversation = await request("/ai/conversations", { method: "POST" });
        currentConversationId = conversation.id;
        await loadChat();
        await loadMessages();
    } catch (e) {
        notify(e.message);
    }
});

$("#chat-send").addEventListener("click", async () => {
    if (!currentConversationId) {
        notify("请先选择会话");
        return;
    }
    const content = $("#chat-input").value.trim();
    if (!content) return;
    try {
        await request(`/ai/conversations/${currentConversationId}/messages`, {
            method: "POST",
            headers: headers(),
            body: JSON.stringify({ content })
        });
        $("#chat-input").value = "";
        await loadMessages();
    } catch (e) {
        notify(e.message);
    }
});

$("#back-detail").addEventListener("click", () => showView("public"));
$("#back-author").addEventListener("click", () => showView("public"));
$("#detail-comment-send").addEventListener("click", async () => {
    const content = $("#detail-comment-input").value.trim();
    const files = $("#detail-comment-files").files;
    if (!content && files.length === 0) {
        notify("请输入评论内容或选择照片/视频");
        return;
    }
    try {
        const media = [];
        for (const file of files) {
            const fd = new FormData();
            fd.append("file", file);
            const res = await fetch(API + "/files", { method: "POST", headers: authHeaders(), body: fd });
            const body = await res.json();
            if (body.code !== 200) throw new Error(body.message || "上传失败");
            media.push(body.data);
        }
        await request("/comments", {
            method: "POST",
            headers: headers(),
            body: JSON.stringify({ noteId: detailNoteId, parentId: replyToCommentId, content, media })
        });
        $("#detail-comment-input").value = "";
        $("#detail-comment-files").value = "";
        clearReply();
        await loadComments(detailNoteId);
    } catch (e) {
        notify(e.message);
    }
});

function escapeHtml(value) {
    return String(value || "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

afterAuth();
if (token()) showView("notes");
