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
    if (name === "notes") loadMyNotes();
    if (name === "public") loadPublicNotes();
    if (name === "feeds") loadFeeds();
    if (name === "chat") loadChat();
    if (name === "admin") loadAdmin();
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
        notify("注册成功，请登录");
        registerMode = false;
        renderAuthMode();
        $("#username").value = "";
        $("#password").value = "";
        $("#nickname").value = "";
        showAuth();
        return;
    }
    localStorage.setItem("token", body.data.token);
    localStorage.setItem("role", body.data.user.role);
    afterAuth();
    showView("notes");
});

function afterAuth() {
    const logged = !!token();
    $("#auth-view").classList.toggle("hidden", logged);
    $("#logout").classList.toggle("hidden", !logged);
    $(".admin-only").classList.toggle("hidden", localStorage.getItem("role") !== "ADMIN");
    document.querySelectorAll("[data-view]").forEach((b) => b.classList.remove("hidden"));
}

function logout() {
    clearSession();
    showAuth();
}

function clearSession() {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    afterAuth();
}
$("#logout").addEventListener("click", logout);

function noteCard(note) {
    const el = document.createElement("div");
    el.className = "note-card";
    el.innerHTML = `<h3>${escapeHtml(note.title)}</h3><p>${escapeHtml(note.bookName || "")}</p><p>${escapeHtml((note.content || "").slice(0, 80))}</p>`;
    el.style.cursor = "pointer";
    el.addEventListener("click", () => openNoteDetail(note.id));

    const actions = document.createElement("div");
    actions.className = "note-actions";
    actions.appendChild(actionButton("点赞", async () => {
        try {
            await request(`/likes/${note.id}`, { method: "POST" });
            notify("点赞成功");
        } catch (e) { notify(e.message); }
    }));
    actions.appendChild(actionButton("评论", async () => {
        const content = await askText("输入评论内容");
        if (!content) return;
        try {
            await request("/comments", { method: "POST", headers: headers(), body: JSON.stringify({ noteId: note.id, content }) });
            notify("评论成功");
        } catch (e) { notify(e.message); }
    }));
    actions.appendChild(actionButton("关注作者", async () => {
        try {
            await request(`/follows/${note.userId}`, { method: "POST" });
            notify("关注成功");
        } catch (e) { notify(e.message); }
    }));
    actions.appendChild(actionButton("AI 总结", async () => {
        try {
            const data = await request("/ai/summary", { method: "POST", headers: headers(), body: JSON.stringify({ noteId: note.id }) });
            notify(data.content);
        } catch (e) { notify(e.message); }
    }));
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

function showDetail() {
    document.querySelectorAll(".view").forEach((v) => v.classList.add("hidden"));
    $("#detail-view").classList.remove("hidden");
}

function openNoteDetail(id) {
    detailNoteId = id;
    showDetail();
    loadNoteDetail();
}

async function loadNoteDetail() {
    try {
        const note = await request(`/notes/${detailNoteId}`);
        const author = await request(`/users/${note.userId}`);
        $("#detail-title").textContent = note.title;
        $("#detail-meta").textContent = `${author.nickname || author.username} · ${formatDate(note.createdAt)} · 点赞 ${note.likeCount}`;
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

        $("#detail-ai").textContent = note.aiResult ? "AI 总结：" + note.aiResult : "暂无 AI 总结";
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
    actions.appendChild(actionButton("点赞", async () => {
        try {
            await request(`/likes/${note.id}`, { method: "POST" });
            notify("点赞成功");
            loadNoteDetail();
        } catch (e) { notify(e.message); }
    }));
    actions.appendChild(actionButton("关注作者", async () => {
        try {
            await request(`/follows/${note.userId}`, { method: "POST" });
            notify("关注成功");
        } catch (e) { notify(e.message); }
    }));
    actions.appendChild(actionButton("AI 总结", async () => {
        try {
            const data = await request("/ai/summary", { method: "POST", headers: headers(), body: JSON.stringify({ noteId: note.id }) });
            notify(data.content);
        } catch (e) { notify(e.message); }
    }));
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
    const body = document.createElement("div");
    body.className = "comment-body";
    body.textContent = comment.content || "";
    div.append(head, body);
    parent.appendChild(div);
    (comment.children || []).forEach((child) => {
        const wrap = document.createElement("div");
        wrap.className = "comment-children";
        renderComment(child, authors, wrap);
        div.appendChild(wrap);
    });
}

function formatDate(value) {
    if (!value) return "";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString("zh-CN", { hour12: false });
}

async function loadMyNotes() {
    const data = await request("/notes/my?page=1&size=50");
    $("#notes-list").innerHTML = "";
    (data.records || []).forEach((n) => $("#notes-list").appendChild(noteCard(n)));
}

async function loadPublicNotes() {
    const data = await request("/notes?page=1&size=50");
    $("#public-list").innerHTML = "";
    (data.records || []).forEach((n) => $("#public-list").appendChild(noteCard(n)));
}

async function loadFeeds() {
    const data = await request("/feeds?page=1&size=50");
    $("#feeds-list").innerHTML = "";
    (data.records || []).forEach((n) => $("#feeds-list").appendChild(noteCard(n)));
}

async function loadChat() {
    const data = await request("/ai/conversations");
    const conversations = Array.isArray(data) ? data : (data.records || []);
    $("#chat-list").innerHTML = "";
    conversations.forEach((conversation) => {
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
        $("#chat-list").appendChild(btn);
    });
}

async function loadMessages() {
    if (!currentConversationId) {
        $("#chat-messages").innerHTML = "";
        return;
    }
    const data = await request(`/ai/conversations/${currentConversationId}/messages`);
    const messages = Array.isArray(data) ? data : (data.records || []);
    $("#chat-messages").innerHTML = "";
    messages.forEach((message) => {
        const role = String(message.role || "assistant").toLowerCase();
        const content = message.content || "";
        const item = document.createElement("div");
        item.className = `chat-message ${role === "user" ? "user" : "assistant"}`;
        item.innerHTML = `<div class="chat-role">${escapeHtml(message.role || "assistant")}</div><div class="chat-content">${escapeHtml(content)}</div>`;
        $("#chat-messages").appendChild(item);
    });
    $("#chat-messages").scrollTop = $("#chat-messages").scrollHeight;
}

$("#new-note").addEventListener("click", () => {
    $("#note-form").reset();
    $("#editor-title").textContent = "新建笔记";
    showView("editor");
});
$("#cancel-editor").addEventListener("click", () => showView("notes"));
$("#note-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
        title: $("#note-title").value,
        content: $("#note-content").value,
        bookName: $("#note-book").value,
        visibility: Number($("#note-visibility").value)
    };
    const data = await request("/notes", { method: "POST", headers: headers(), body: JSON.stringify(payload) });
    const files = $("#note-images").files;
    if (files.length) {
        const fd = new FormData();
        for (const f of files) fd.append("files", f);
        await fetch(API + "/notes/" + data.id + "/images", { method: "POST", headers: authHeaders(), body: fd });
    }
    showView("notes");
});

async function loadAdmin() {
    const stats = await request("/admin/ai-stats");
    $("#ai-total").textContent = stats.totalCalls;
    $("#ai-success").textContent = stats.successCalls;
    $("#ai-failed").textContent = stats.failedCalls;
    const users = await request("/admin/users?page=1&size=50");
    $("#admin-users").innerHTML = "";
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
    const notes = await request("/admin/notes?page=1&size=50");
    $("#admin-notes").innerHTML = "";
    (notes.records || []).forEach((n) => {
        const row = document.createElement("div");
        row.className = "row";
        row.innerHTML = `<span>${escapeHtml(n.title)} · 审核:${n.auditStatus}</span>`;
        const btn = document.createElement("button");
        btn.textContent = n.auditStatus === 1 ? "下架" : "通过";
        btn.onclick = async () => {
            if (n.auditStatus === 1) await request(`/admin/notes/${n.id}/offline`, { method: "PUT", headers: headers() });
            else await request(`/admin/notes/${n.id}/audit`, { method: "PUT", headers: headers(), body: JSON.stringify({ auditStatus: 1 }) });
            loadAdmin();
        };
        row.appendChild(btn);
        $("#admin-notes").appendChild(row);
    });
}

$("#new-chat").addEventListener("click", async () => {
    try {
        await request("/ai/conversations", { method: "POST" });
        currentConversationId = null;
        $("#chat-messages").innerHTML = "";
        await loadChat();
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
$("#detail-comment-send").addEventListener("click", async () => {
    const content = $("#detail-comment-input").value.trim();
    if (!content) {
        notify("请输入评论内容");
        return;
    }
    try {
        await request("/comments", {
            method: "POST",
            headers: headers(),
            body: JSON.stringify({ noteId: detailNoteId, content })
        });
        $("#detail-comment-input").value = "";
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
