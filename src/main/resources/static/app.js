const apiBase = "/serverManege";

const state = {
    services: [],
    selectedServiceId: "",
    detail: null,
    globalConfigs: [],
    currentView: "services",
    tailAbort: null
};

const $ = (selector) => document.querySelector(selector);

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    loadAll();
});

function bindEvents() {
    document.querySelectorAll(".nav-item").forEach((item) => {
        item.addEventListener("click", () => switchView(item.dataset.view));
    });
    $("#refreshBtn").addEventListener("click", loadAll);
    $("#newServiceBtn").addEventListener("click", () => {
        resetServiceForm();
        switchView("services");
    });
    $("#serviceForm").addEventListener("submit", saveService);
    $("#resetServiceBtn").addEventListener("click", resetServiceForm);
    $("#deleteServiceBtn").addEventListener("click", deleteService);
    $("#startBtn").addEventListener("click", () => runtimeAction("start"));
    $("#stopBtn").addEventListener("click", () => runtimeAction("stop"));
    $("#restartBtn").addEventListener("click", () => runtimeAction("restart"));
    $("#configForm").addEventListener("submit", saveConfig);
    $("#resetConfigBtn").addEventListener("click", resetConfigForm);
    $("#packageForm").addEventListener("submit", uploadPackage);
    $("#globalForm").addEventListener("submit", saveGlobalConfigs);
    $("#loadLogBtn").addEventListener("click", loadSelectedLog);
    $("#logSelect").addEventListener("change", updateDownloadLink);
}

async function loadAll() {
    try {
        const [services, globalConfigs] = await Promise.all([
            request("/services"),
            request("/global-configs")
        ]);
        state.services = services || [];
        state.globalConfigs = globalConfigs || [];
        if (!state.selectedServiceId && state.services.length) {
            state.selectedServiceId = state.services[0].id;
        }
        renderServices();
        renderGlobalConfigs();
        await loadSelectedDetail();
        showToast("已刷新");
    } catch (error) {
        showToast(error.message || "刷新失败");
    }
}

async function loadSelectedDetail() {
    if (!state.selectedServiceId) {
        state.detail = null;
        renderDetail();
        return;
    }
    state.detail = await request(`/services/${state.selectedServiceId}`);
    renderDetail();
}

function renderServices() {
    $("#serviceCount").textContent = `${state.services.length} 项`;
    $("#totalServices").textContent = state.services.length;
    $("#runningServices").textContent = state.services.filter((item) => item.status === "RUNNING").length;
    const list = $("#serviceList");
    if (!state.services.length) {
        list.innerHTML = `<div class="empty">暂无服务</div>`;
        $("#activeServiceName").textContent = "-";
        return;
    }
    list.innerHTML = state.services.map((service) => {
        const active = service.id === state.selectedServiceId ? " active" : "";
        const status = service.status || "STOPPED";
        return `
            <article class="service-card${active}" data-id="${escapeHtml(service.id)}">
                <header>
                    <strong>${escapeHtml(service.serviceName || service.serverName || "未命名服务")}</strong>
                    <span class="status-badge ${status === "RUNNING" ? "" : "muted"}">${escapeHtml(status)}</span>
                </header>
                <div class="service-meta">
                    <span>端口 ${escapeHtml(service.port || service.serverPort || "-")}</span>
                    <span>内存 ${escapeHtml(service.memoryLimit || "-")}</span>
                    <span>数据库 ${escapeHtml(service.databaseName || "-")}</span>
                </div>
            </article>
        `;
    }).join("");
    list.querySelectorAll(".service-card").forEach((card) => {
        card.addEventListener("click", async () => {
            state.selectedServiceId = card.dataset.id;
            renderServices();
            await loadSelectedDetail();
        });
    });
}

function renderDetail() {
    const detail = state.detail;
    const service = detail ? detail.service : null;
    $("#activeServiceName").textContent = service ? (service.serviceName || "未命名服务") : "-";
    fillServiceForm(service);
    renderRuntime(detail ? detail.runtimeInfo : null);
    renderConfigs(detail ? detail.configItems || [] : []);
    renderPackages(detail ? detail.packages || [] : []);
    renderLogs();
}

function fillServiceForm(service) {
    $("#serviceId").value = service ? service.id || "" : "";
    $("#serviceName").value = service ? service.serviceName || "" : "";
    $("#port").value = service ? service.port || "" : "";
    $("#memoryLimit").value = service ? service.memoryLimit || "" : "";
    $("#databaseName").value = service ? service.databaseName || "" : "";
    $("#description").value = service ? service.description || "" : "";
}

function renderRuntime(runtime) {
    const status = runtime ? runtime.status || "STOPPED" : "STOPPED";
    $("#runtimeBadge").textContent = status;
    $("#runtimeBadge").className = `status-badge ${status === "RUNNING" ? "" : "muted"}`;
    const command = runtime && runtime.command && runtime.command.length ? runtime.command.join(" ") : "选择服务并启动后显示命令";
    $("#commandBox").textContent = command;
    $("#commandStatus").textContent = runtime && runtime.logFile ? runtime.logFile : "命令待生成";
}

function renderConfigs(configs) {
    $("#configCount").textContent = `${configs.length} 项`;
    const table = $("#configTable");
    if (!configs.length) {
        table.innerHTML = `<tr><td colspan="4">暂无配置项</td></tr>`;
        return;
    }
    table.innerHTML = configs.map((item) => `
        <tr>
            <td>${escapeHtml(item.name)}</td>
            <td>${escapeHtml(item.parameterPath)}</td>
            <td>${escapeHtml(item.value || "")}</td>
            <td>
                <button data-edit-config="${escapeHtml(item.id)}">编辑</button>
                <button class="danger-button" data-delete-config="${escapeHtml(item.id)}">删除</button>
            </td>
        </tr>
    `).join("");
    table.querySelectorAll("[data-edit-config]").forEach((button) => {
        button.addEventListener("click", () => editConfig(button.dataset.editConfig));
    });
    table.querySelectorAll("[data-delete-config]").forEach((button) => {
        button.addEventListener("click", () => deleteConfig(button.dataset.deleteConfig));
    });
}

function renderPackages(packages) {
    $("#packageCount").textContent = `${packages.length} 项`;
    $("#totalPackages").textContent = packages.length;
    const table = $("#packageTable");
    if (!packages.length) {
        table.innerHTML = `<tr><td colspan="6">暂无 Jar 包</td></tr>`;
        return;
    }
    table.innerHTML = packages.map((item) => `
        <tr>
            <td>${escapeHtml(item.fileName)}</td>
            <td>${escapeHtml(item.version)}</td>
            <td>${formatSize(item.fileSize)}</td>
            <td class="hash">${escapeHtml(item.sha256 || "")}</td>
            <td>${item.currentUsed ? `<span class="status-badge">当前使用</span>` : `<span class="status-badge muted">备用</span>`}</td>
            <td>
                <button data-use-package="${escapeHtml(item.id)}">使用</button>
                <button class="danger-button" data-delete-package="${escapeHtml(item.id)}">删除</button>
            </td>
        </tr>
    `).join("");
    table.querySelectorAll("[data-use-package]").forEach((button) => {
        button.addEventListener("click", () => usePackage(button.dataset.usePackage));
    });
    table.querySelectorAll("[data-delete-package]").forEach((button) => {
        button.addEventListener("click", () => deletePackage(button.dataset.deletePackage));
    });
}

async function renderLogs() {
    const select = $("#logSelect");
    if (!state.selectedServiceId) {
        select.innerHTML = "";
        $("#logBox").textContent = "暂无日志";
        updateDownloadLink();
        return;
    }
    try {
        const logs = await request(`/services/${state.selectedServiceId}/logs`);
        select.innerHTML = logs.length
            ? logs.map((name) => `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`).join("")
            : `<option value="">暂无日志</option>`;
        updateDownloadLink();
    } catch (error) {
        select.innerHTML = `<option value="">暂无日志</option>`;
        updateDownloadLink();
    }
}

function renderGlobalConfigs() {
    const db = state.globalConfigs.find((item) => item.configKey === "database.address");
    const middleware = state.globalConfigs.find((item) => item.configKey === "middleware.address");
    $("#databaseAddress").value = db ? db.configValue || "" : "";
    $("#middlewareAddress").value = middleware ? middleware.configValue || "" : "";
}

async function saveService(event) {
    event.preventDefault();
    const id = $("#serviceId").value;
    const body = {
        serviceName: $("#serviceName").value.trim(),
        port: Number($("#port").value),
        memoryLimit: $("#memoryLimit").value.trim(),
        databaseName: $("#databaseName").value.trim(),
        description: $("#description").value.trim()
    };
    const saved = id
        ? await request(`/services/${id}`, { method: "PUT", body })
        : await request("/services", { method: "POST", body });
    state.selectedServiceId = saved.id;
    await loadAll();
    showToast("服务已保存");
}

async function deleteService() {
    if (!state.selectedServiceId || !confirm("确认删除当前服务？")) return;
    await request(`/services/${state.selectedServiceId}`, { method: "DELETE" });
    state.selectedServiceId = "";
    await loadAll();
    showToast("服务已删除");
}

function resetServiceForm() {
    state.selectedServiceId = "";
    state.detail = null;
    renderServices();
    renderDetail();
}

async function runtimeAction(action) {
    if (!state.selectedServiceId) {
        showToast("请先选择服务");
        return;
    }
    const runtime = await request(`/services/${state.selectedServiceId}/${action}`, { method: "POST" });
    if (state.detail) {
        state.detail.runtimeInfo = runtime;
    }
    renderRuntime(runtime);
    await loadAll();
    showToast(action === "start" ? "启动命令已执行" : action === "stop" ? "服务已关闭" : "服务已重启");
}

async function saveConfig(event) {
    event.preventDefault();
    if (!state.selectedServiceId) {
        showToast("请先选择服务");
        return;
    }
    const id = $("#configId").value;
    const body = {
        name: $("#configName").value.trim(),
        parameterPath: $("#parameterPath").value.trim(),
        value: $("#configValue").value
    };
    if (id) {
        await request(`/configs/${id}`, { method: "PUT", body });
    } else {
        await request(`/services/${state.selectedServiceId}/configs`, { method: "POST", body });
    }
    resetConfigForm();
    await loadSelectedDetail();
    showToast("配置项已保存");
}

function editConfig(id) {
    const item = state.detail.configItems.find((config) => config.id === id);
    if (!item) return;
    $("#configId").value = item.id;
    $("#configName").value = item.name || "";
    $("#parameterPath").value = item.parameterPath || "";
    $("#configValue").value = item.value || "";
}

async function deleteConfig(id) {
    if (!confirm("确认删除该配置项？")) return;
    await request(`/configs/${id}`, { method: "DELETE" });
    await loadSelectedDetail();
    showToast("配置项已删除");
}

function resetConfigForm() {
    $("#configId").value = "";
    $("#configName").value = "";
    $("#parameterPath").value = "";
    $("#configValue").value = "";
}

async function uploadPackage(event) {
    event.preventDefault();
    if (!state.selectedServiceId) {
        showToast("请先选择服务");
        return;
    }
    const formData = new FormData();
    formData.append("version", $("#packageVersion").value.trim());
    formData.append("file", $("#packageFile").files[0]);
    await fetch(`${apiBase}/services/${state.selectedServiceId}/packages`, {
        method: "POST",
        body: formData
    }).then(handleResponse);
    $("#packageForm").reset();
    await loadSelectedDetail();
    showToast("Jar 包已上传");
}

async function usePackage(id) {
    await request(`/services/${state.selectedServiceId}/packages/${id}/use`, { method: "POST" });
    await loadSelectedDetail();
    showToast("当前 Jar 包已更新");
}

async function deletePackage(id) {
    if (!confirm("确认删除该 Jar 包？")) return;
    await request(`/packages/${id}`, { method: "DELETE" });
    await loadSelectedDetail();
    showToast("Jar 包已删除");
}

async function saveGlobalConfigs(event) {
    event.preventDefault();
    await Promise.all([
        request("/global-configs", {
            method: "POST",
            body: {
                configKey: "database.address",
                configValue: $("#databaseAddress").value.trim(),
                description: "公共数据库地址"
            }
        }),
        request("/global-configs", {
            method: "POST",
            body: {
                configKey: "middleware.address",
                configValue: $("#middlewareAddress").value.trim(),
                description: "公共中间件地址"
            }
        })
    ]);
    state.globalConfigs = await request("/global-configs");
    showToast("公共配置已保存");
}

async function loadSelectedLog() {
    const fileName = $("#logSelect").value;
    if (!state.selectedServiceId || !fileName) {
        $("#logBox").textContent = "暂无日志";
        return;
    }
    if (state.tailAbort) {
        state.tailAbort.abort();
    }
    state.tailAbort = new AbortController();
    $("#logBox").textContent = "正在连接日志流...\n";
    try {
        const response = await fetch(`${apiBase}/services/${state.selectedServiceId}/logs/${encodeURIComponent(fileName)}/tail`, {
            signal: state.tailAbort.signal
        });
        if (!response.ok || !response.body) {
            throw new Error("日志读取失败");
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            $("#logBox").textContent += decoder.decode(value, { stream: true });
            $("#logBox").scrollTop = $("#logBox").scrollHeight;
        }
    } catch (error) {
        if (error.name !== "AbortError") {
            showToast(error.message || "日志读取失败");
        }
    }
}

function updateDownloadLink() {
    const fileName = $("#logSelect").value;
    const link = $("#downloadLogLink");
    if (!state.selectedServiceId || !fileName) {
        link.href = "#";
        return;
    }
    link.href = `${apiBase}/services/${state.selectedServiceId}/logs/${encodeURIComponent(fileName)}/download`;
}

function switchView(viewName) {
    state.currentView = viewName;
    document.querySelectorAll(".nav-item").forEach((item) => {
        item.classList.toggle("active", item.dataset.view === viewName);
    });
    document.querySelectorAll(".view").forEach((view) => {
        view.classList.toggle("active", view.id === `view-${viewName}`);
    });
    const titles = {
        services: "服务",
        packages: "包版本",
        configs: "配置项",
        logs: "日志",
        global: "公共配置"
    };
    $("#pageTitle").textContent = titles[viewName] || "服务";
}

async function request(path, options = {}) {
    const init = {
        method: options.method || "GET",
        headers: {}
    };
    if (options.body !== undefined) {
        init.headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(options.body);
    }
    return fetch(`${apiBase}${path}`, init).then(handleResponse);
}

async function handleResponse(response) {
    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json") ? await response.json() : await response.text();
    if (!response.ok) {
        throw new Error(data && data.message ? data.message : "请求失败");
    }
    return data;
}

function formatSize(size) {
    if (!size) return "-";
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function escapeHtml(value) {
    return String(value === null || value === undefined ? "" : value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showToast(message) {
    const toast = $("#toast");
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove("show"), 2200);
}
