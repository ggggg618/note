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
    if (body.code !== 200) return alert(body.message);
    if (registerMode) {
        alert("注册成功，请登录");
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
    return el;
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

function escapeHtml(value) {
    return String(value || "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

afterAuth();
if (token()) showView("notes");
