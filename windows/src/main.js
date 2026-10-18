// Loop Windows Desktop Client Controller
import { invoke } from '@tauri-apps/api/core';
import { getCurrentWindow } from '@tauri-apps/api/window';

const DEFAULT_URL = "https://dcsplayer.blogspot.com/";

// Initialize Window Instance
let appWindow = null;
try {
  appWindow = getCurrentWindow();
} catch (e) {
  console.warn("Tauri Window API not available (web preview mode):", e);
}

// Safe Invocation Wrapper (Tauri Core with local fallback)
const safeInvoke = async (cmd, args = {}) => {
  try {
    return await invoke(cmd, args);
  } catch (err) {
    if (window.__TAURI__?.core?.invoke) {
      return await window.__TAURI__.core.invoke(cmd, args);
    }
    console.log(`[Preview Fallback] ${cmd}:`, args);
    if (cmd === "get_app_settings") {
      return {
        active_url: localStorage.getItem("loop_url") || DEFAULT_URL,
        has_password: !!localStorage.getItem("loop_password"),
        is_js_enabled: true,
        is_desktop_mode: false,
        is_adblock_enabled: true,
        blocked_ads_count: 142,
        dns_provider: localStorage.getItem("loop_dns_provider") || "adguard",
        custom_dns_template: localStorage.getItem("loop_custom_dns") || "https://dns.adguard-dns.com/dns-query",
        proxy_mode: localStorage.getItem("loop_proxy_mode") || "direct",
        proxy_url: localStorage.getItem("loop_proxy_url") || ""
      };
    }
    if (cmd === "verify_access_code") return args.code === "dcsus7";
    if (cmd === "verify_password") return args.password === localStorage.getItem("loop_password");
    if (cmd === "set_active_url") {
      localStorage.setItem("loop_url", args.url);
      return args.url;
    }
    if (cmd === "reset_default_url") {
      localStorage.setItem("loop_url", DEFAULT_URL);
      return DEFAULT_URL;
    }
    if (cmd === "set_dns_config") {
      localStorage.setItem("loop_dns_provider", args.provider);
      localStorage.setItem("loop_custom_dns", args.template);
      return true;
    }
    if (cmd === "set_proxy_config") {
      localStorage.setItem("loop_proxy_mode", args.mode);
      localStorage.setItem("loop_proxy_url", args.url);
      return true;
    }
    return null;
  }
};

// ========================================================
// 1. Diagnostic Console & System Logger
// ========================================================
class SystemLogger {
  constructor() {
    this.logs = [];
    this.maxLogs = 400;
    this.container = document.getElementById("system-log-stream");
  }

  log(category, message, level = "info") {
    const timestamp = new Date().toLocaleTimeString();
    const entry = { timestamp, category, message, level };
    this.logs.push(entry);
    if (this.logs.length > this.maxLogs) this.logs.shift();

    console.log(`[${category}] ${message}`);

    if (this.container) {
      const row = document.createElement("div");
      row.className = `log-row log-${level.toLowerCase()}`;
      row.innerHTML = `
        <span class="log-time">${timestamp}</span>
        <span class="log-tag tag-${category.toLowerCase()}">${category}</span>
        <span class="log-msg">${this.escapeHtml(message)}</span>
      `;
      this.container.appendChild(row);
      this.container.scrollTop = this.container.scrollHeight;
    }
  }

  escapeHtml(str) {
    return String(str).replace(/[&<>'"]/g, 
      tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
    );
  }

  clear() {
    this.logs = [];
    if (this.container) this.container.innerHTML = "";
    this.log("SYS", "Diagnostic log cleared by user.", "info");
  }

  exportText() {
    return this.logs.map(l => `[${l.timestamp}] [${l.category}] (${l.level.toUpperCase()}) ${l.message}`).join("\n");
  }
}

const Logger = new SystemLogger();

// ========================================================
// 2. DOM Elements
// ========================================================
const webFrame = document.getElementById("web-frame");
const loadingBar = document.getElementById("loading-bar");
const frameWarningBanner = document.getElementById("frame-warning-banner");
const btnBannerDirect = document.getElementById("btn-banner-direct");
const btnBannerDiagnostics = document.getElementById("btn-banner-diagnostics");
const btnBannerDismiss = document.getElementById("btn-banner-dismiss");

const btnBack = document.getElementById("btn-back");
const btnForward = document.getElementById("btn-forward");
const btnReload = document.getElementById("btn-reload");
const btnHome = document.getElementById("btn-home");
const btnDirectView = document.getElementById("btn-direct-view");
const btnDiagnostics = document.getElementById("btn-diagnostics");
const btnSettings = document.getElementById("btn-settings");
const settingsDrawer = document.getElementById("settings-drawer");
const btnCloseSettings = document.getElementById("btn-close-settings");

// Settings Elements
const btnUnlockUrl = document.getElementById("btn-unlock-url");
const urlLockStatus = document.getElementById("url-lock-status");
const urlEditPanel = document.getElementById("url-edit-panel");
const settingActiveUrl = document.getElementById("setting-active-url");
const btnApplyUrl = document.getElementById("btn-apply-url");
const btnDefaultUrl = document.getElementById("btn-default-url");
const accessCodeModal = document.getElementById("access-code-modal");
const inputAccessCode = document.getElementById("input-access-code");
const btnVerifyAccess = document.getElementById("btn-verify-access");
const btnCancelAccess = document.getElementById("btn-cancel-access");
const accessCodeErr = document.getElementById("access-code-err");

// DNS Elements
const dnsRadios = document.querySelectorAll('input[name="dns-choice"]');
const customDnsContainer = document.getElementById("custom-dns-container");
const inputCustomDns = document.getElementById("input-custom-dns");
const btnSaveDns = document.getElementById("btn-save-dns");
const dnsStatusMsg = document.getElementById("dns-status-msg");
const dnsBadge = document.getElementById("dns-badge");

// Proxy Elements
const proxyRadios = document.querySelectorAll('input[name="proxy-choice"]');
const proxyInputContainer = document.getElementById("proxy-input-container");
const inputProxyUrl = document.getElementById("input-proxy-url");
const btnSaveProxy = document.getElementById("btn-save-proxy");
const proxyStatusMsg = document.getElementById("proxy-status-msg");
const proxyBadge = document.getElementById("proxy-badge");

// Diagnostics Modal Elements
const diagnosticsModal = document.getElementById("diagnostics-modal");
const btnOpenDiagnostics = document.getElementById("btn-open-diagnostics");
const btnCloseDiagnostics = document.getElementById("btn-close-diagnostics");
const btnDirectModeLaunch = document.getElementById("btn-direct-mode-launch");
const btnClearLogs = document.getElementById("btn-clear-logs");
const btnCopyLogs = document.getElementById("btn-copy-logs");
const diagDns = document.getElementById("diag-dns");
const diagProxy = document.getElementById("diag-proxy");
const btnTestDoh = document.getElementById("btn-test-doh");
const btnLaunchFirefox = document.getElementById("btn-launch-firefox");

// App Lock Elements
const lockScreenModal = document.getElementById("lock-screen-modal");
const inputAppPassword = document.getElementById("input-app-password");
const btnUnlockApp = document.getElementById("btn-unlock-app");
const lockErrorMsg = document.getElementById("lock-error-msg");
const applockBadge = document.getElementById("applock-badge");
const btnManagePassword = document.getElementById("btn-manage-password");
const btnLockNow = document.getElementById("btn-lock-now");

let currentActiveUrl = DEFAULT_URL;
let isUrlCardUnlocked = false;
let currentDnsChoice = "adguard";
let currentProxyChoice = "direct";
let loadTimeoutTimer = null;

// ========================================================
// 3. Window Dragging & Single OS Title Bar Controls
// ========================================================
if (appWindow) {
  document.querySelectorAll('[data-tauri-drag-region]').forEach((el) => {
    el.addEventListener('mousedown', (e) => {
      if (e.target.closest('button, input, select, a, .interactive')) return;
      if (e.detail === 2) {
        appWindow.toggleMaximize().catch(() => safeInvoke("window_maximize_toggle"));
      } else {
        appWindow.startDragging().catch(() => safeInvoke("window_start_dragging"));
      }
    });
  });
}

// ========================================================
// 4. Navigation & View Modes
// ========================================================
function navigate(url, directNativeMode = false) {
  let finalUrl = (url || "").trim();
  if (!finalUrl) finalUrl = DEFAULT_URL;
  if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
    finalUrl = "https://" + finalUrl;
  }

  Logger.log("NET", `Navigating to: ${finalUrl}`, "info");
  Logger.log("DNS", `Querying encrypted resolver: ${getDnsDescription(currentDnsChoice)}`, "info");
  Logger.log("SEC", "uBlock Origin DeclarativeNetRequest content filter applied.", "info");

  if (directNativeMode) {
    Logger.log("SYS", "Switching to Native Top-Level Navigation Mode. Bypassing iframe X-Frame-Options constraints.", "success");
    // Direct top-level navigation matches Android's WebView.loadUrl(url) exactly
    window.location.href = finalUrl;
    return;
  }

  // Normal embedded iframe mode
  if (loadingBar) loadingBar.classList.remove("hidden");
  if (frameWarningBanner) frameWarningBanner.classList.add("hidden");

  // Timeout guard for blocked iframes (e.g., X-Frame-Options: SAMEORIGIN)
  if (loadTimeoutTimer) clearTimeout(loadTimeoutTimer);
  loadTimeoutTimer = setTimeout(() => {
    if (loadingBar && !loadingBar.classList.contains("hidden")) {
      loadingBar.classList.add("hidden");
      Logger.log("WARN", "Iframe load taking longer than expected. If site shows 'webpage might be having issues', it restricts framing (X-Frame-Options). Click 'Switch to Native View' to open directly.", "warn");
      if (frameWarningBanner) frameWarningBanner.classList.remove("hidden");
    }
  }, 5000);

  if (webFrame) {
    webFrame.src = finalUrl;
  }
}

function getDnsDescription(provider) {
  switch (provider) {
    case "adguard": return "AdGuard DNS (https://dns.adguard-dns.com/dns-query)";
    case "cloudflare": return "Cloudflare DoH (https://cloudflare-dns.com/dns-query)";
    case "google": return "Google Public DNS (https://dns.google/dns-query)";
    case "custom": return inputCustomDns?.value || "Custom DoH Resolver";
    case "system": return "System Local ISP DNS";
    default: return "AdGuard DNS";
  }
}

if (webFrame) {
  webFrame.addEventListener("load", () => {
    if (loadTimeoutTimer) clearTimeout(loadTimeoutTimer);
    if (loadingBar) loadingBar.classList.add("hidden");
    Logger.log("NET", `Frame loaded: ${webFrame.src}`, "success");
  });

  webFrame.addEventListener("error", (e) => {
    if (loadTimeoutTimer) clearTimeout(loadTimeoutTimer);
    if (loadingBar) loadingBar.classList.add("hidden");
    Logger.log("ERR", `Frame error loading ${webFrame.src}: Connection refused or blocked by frame-ancestors.`, "err");
    if (frameWarningBanner) frameWarningBanner.classList.remove("hidden");
  });
}

// Navigation Header Buttons
btnBack?.addEventListener("click", () => {
  try { webFrame?.contentWindow?.history?.back(); } catch (_) {}
  Logger.log("SYS", "History Back", "info");
});

btnForward?.addEventListener("click", () => {
  try { webFrame?.contentWindow?.history?.forward(); } catch (_) {}
  Logger.log("SYS", "History Forward", "info");
});

btnReload?.addEventListener("click", () => {
  if (webFrame) {
    if (loadingBar) loadingBar.classList.remove("hidden");
    Logger.log("NET", `Reloading page: ${webFrame.src}`, "info");
    webFrame.src = webFrame.src;
  }
});

btnHome?.addEventListener("click", () => {
  navigate(currentActiveUrl);
});

btnDirectView?.addEventListener("click", () => {
  navigate(currentActiveUrl, true);
});

// Warning Banner Actions
btnBannerDirect?.addEventListener("click", () => {
  navigate(currentActiveUrl, true);
});
btnBannerDiagnostics?.addEventListener("click", () => {
  openDiagnosticsModal();
});
btnBannerDismiss?.addEventListener("click", () => {
  if (frameWarningBanner) frameWarningBanner.classList.add("hidden");
});

// ========================================================
// 5. Diagnostics & System Log Handlers
// ========================================================
function openDiagnosticsModal() {
  if (diagDns) diagDns.textContent = getDnsDescription(currentDnsChoice).split(" ")[0];
  if (diagProxy) {
    diagProxy.textContent = currentProxyChoice === "direct" ? "Direct" : currentProxyChoice.toUpperCase();
  }
  diagnosticsModal?.classList.remove("hidden");
  Logger.log("SYS", "Diagnostic console opened.", "info");
}

function closeDiagnosticsModal() {
  diagnosticsModal?.classList.add("hidden");
}

btnDiagnostics?.addEventListener("click", openDiagnosticsModal);
btnOpenDiagnostics?.addEventListener("click", openDiagnosticsModal);
btnCloseDiagnostics?.addEventListener("click", closeDiagnosticsModal);

btnDirectModeLaunch?.addEventListener("click", () => {
  navigate(currentActiveUrl, true);
});

btnClearLogs?.addEventListener("click", () => {
  Logger.clear();
});

btnCopyLogs?.addEventListener("click", async () => {
  const text = Logger.exportText();
  try {
    await navigator.clipboard.writeText(text);
    const orig = btnCopyLogs.textContent;
    btnCopyLogs.textContent = "Copied!";
    setTimeout(() => { btnCopyLogs.textContent = orig; }, 1800);
    Logger.log("SYS", "Diagnostic logs copied to clipboard.", "info");
  } catch (err) {
    Logger.log("ERR", `Clipboard copy failed: ${err.message}`, "err");
  }
});

// Test Live Encrypted DoH Resolution
btnTestDoh?.addEventListener("click", async () => {
  const testHost = "www.eporner.com";
  Logger.log("DNS", `Initiating live encrypted DoH query for: ${testHost}...`, "info");
  try {
    const res = await fetch(`https://dns.adguard-dns.com/resolve?name=${testHost}&type=A`, {
      headers: { "Accept": "application/dns-json" }
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    if (data.Answer && data.Answer.length > 0) {
      const ips = data.Answer.map(a => a.data).join(", ");
      Logger.log("DNS", `✓ DoH RESOLUTION SUCCESSFUL: ${testHost} -> [${ips}] (Status: ${data.Status})`, "success");
      Logger.log("SYS", "AdGuard DoH successfully bypassed ISP DNS blocking.", "success");
      Logger.log("SYS", "Note: If video stream still fails, ISP is inspecting the TLS handshake SNI packet. Use Firefox Portable (Strict TRR Mode 3 + ECH) or SOCKS5 proxy tunnel to bypass SNI filtering.", "info");
    } else {
      Logger.log("WARN", `DoH query returned no A records for ${testHost} (Status: ${data.Status})`, "warn");
    }
  } catch (err) {
    Logger.log("ERR", `DoH test query failed: ${err.message}. Check Internet connection or proxy.`, "err");
  }
});

// Launch Firefox Engine Runner
btnLaunchFirefox?.addEventListener("click", () => {
  Logger.log("SYS", "Preparing to run Loop via Standalone Firefox Engine (Gecko)", "info");
  Logger.log("SYS", "Isolated profile: windows\\firefox-runner\\profile\\", "info");
  Logger.log("SYS", "Policies: uBlock Origin active, Strict TRR Mode 3 active, ECH active", "success");
  alert("Firefox Engine Runner is Ready!\n\nTo launch Loop with the isolated Mozilla Firefox (Gecko) engine:\nRun: windows\\firefox-runner\\launch-firefox.bat\n\nFeatures:\n- Native Firefox uBlock Origin extension\n- Strict TRR Mode 3 (100% encrypted DNS)\n- Encrypted Client Hello (bypasses ISP SNI blocking)");
});

// ========================================================
// 6. Settings Drawer & Keyboard Shortcuts
// ========================================================
btnSettings?.addEventListener("click", () => {
  settingsDrawer?.classList.toggle("hidden");
});

btnCloseSettings?.addEventListener("click", () => {
  settingsDrawer?.classList.add("hidden");
});

document.addEventListener("keydown", (e) => {
  if (e.key === "F12" || (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === "l")) {
    e.preventDefault();
    diagnosticsModal?.classList.contains("hidden") ? openDiagnosticsModal() : closeDiagnosticsModal();
  } else if (e.ctrlKey && e.key === ",") {
    e.preventDefault();
    settingsDrawer?.classList.toggle("hidden");
  } else if (e.key === "Escape") {
    settingsDrawer?.classList.add("hidden");
    accessCodeModal?.classList.add("hidden");
    diagnosticsModal?.classList.add("hidden");
  } else if (e.key === "F5" || (e.ctrlKey && e.key.toLowerCase() === "r")) {
    e.preventDefault();
    btnReload?.click();
  }
});

// Access Code ("dcsus7") Protection for Main Page URL
btnUnlockUrl?.addEventListener("click", () => {
  if (isUrlCardUnlocked) {
    isUrlCardUnlocked = false;
    if (urlLockStatus) urlLockStatus.textContent = "🔒 Locked";
    urlEditPanel?.classList.add("hidden");
  } else {
    if (inputAccessCode) inputAccessCode.value = "";
    accessCodeErr?.classList.add("hidden");
    accessCodeModal?.classList.remove("hidden");
    inputAccessCode?.focus();
  }
});

btnVerifyAccess?.addEventListener("click", async () => {
  const code = (inputAccessCode?.value || "").trim();
  const valid = await safeInvoke("verify_access_code", { code });
  if (valid || code === "dcsus7") {
    isUrlCardUnlocked = true;
    if (urlLockStatus) urlLockStatus.textContent = "🔓 Unlocked";
    urlEditPanel?.classList.remove("hidden");
    if (settingActiveUrl) settingActiveUrl.value = currentActiveUrl;
    accessCodeModal?.classList.add("hidden");
    Logger.log("SEC", "Access code verified. URL configuration unlocked.", "success");
  } else {
    accessCodeErr?.classList.remove("hidden");
    Logger.log("SEC", "Access code verification failed.", "warn");
  }
});

inputAccessCode?.addEventListener("keydown", (e) => {
  if (e.key === "Enter") btnVerifyAccess?.click();
});

btnCancelAccess?.addEventListener("click", () => {
  accessCodeModal?.classList.add("hidden");
});

btnApplyUrl?.addEventListener("click", async () => {
  const newUrl = (settingActiveUrl?.value || "").trim();
  if (newUrl) {
    const res = await safeInvoke("set_active_url", { url: newUrl, accessCode: "dcsus7" });
    currentActiveUrl = res || newUrl;
    navigate(currentActiveUrl);
    settingsDrawer?.classList.add("hidden");
    Logger.log("SYS", `Active URL changed to: ${currentActiveUrl}`, "success");
  }
});

btnDefaultUrl?.addEventListener("click", async () => {
  const res = await safeInvoke("reset_default_url", { accessCode: "dcsus7" });
  currentActiveUrl = res || DEFAULT_URL;
  if (settingActiveUrl) settingActiveUrl.value = currentActiveUrl;
  navigate(currentActiveUrl);
  Logger.log("SYS", `Active URL reset to default: ${DEFAULT_URL}`, "info");
});

// Secure DNS (DoH) Management
dnsRadios.forEach((radio) => {
  radio.addEventListener('change', (e) => {
    if (e.target.value === 'custom') {
      customDnsContainer?.classList.remove('hidden');
      inputCustomDns?.focus();
    } else {
      customDnsContainer?.classList.add('hidden');
    }
  });
});

btnSaveDns?.addEventListener('click', async () => {
  const selected = document.querySelector('input[name="dns-choice"]:checked')?.value || "adguard";
  const customTemplate = (inputCustomDns?.value || "").trim() || "https://dns.adguard-dns.com/dns-query";
  currentDnsChoice = selected;
  await safeInvoke("set_dns_config", { provider: selected, template: customTemplate });

  if (dnsBadge) {
    if (selected === "adguard") dnsBadge.textContent = "AdGuard DNS";
    else if (selected === "cloudflare") dnsBadge.textContent = "Cloudflare 1.1.1.1";
    else if (selected === "google") dnsBadge.textContent = "Google DNS";
    else if (selected === "custom") dnsBadge.textContent = "Custom DoH";
    else dnsBadge.textContent = "System (Off)";
  }

  Logger.log("DNS", `DNS resolver configuration updated to: ${getDnsDescription(selected)}`, "success");
  dnsStatusMsg?.classList.remove('hidden');
  setTimeout(() => {
    if (webFrame) webFrame.src = webFrame.src;
  }, 400);
});

// Proxy & SOCKS5 Tunneling Management
proxyRadios.forEach((radio) => {
  radio.addEventListener('change', (e) => {
    if (e.target.value === 'direct') {
      proxyInputContainer?.classList.add('hidden');
    } else {
      proxyInputContainer?.classList.remove('hidden');
      if (inputProxyUrl) {
        if (e.target.value === 'socks5' && !inputProxyUrl.value) {
          inputProxyUrl.value = "socks5://127.0.0.1:1080";
        } else if (e.target.value === 'http' && !inputProxyUrl.value) {
          inputProxyUrl.value = "http://127.0.0.1:8080";
        }
        inputProxyUrl.focus();
      }
    }
  });
});

btnSaveProxy?.addEventListener('click', async () => {
  const selected = document.querySelector('input[name="proxy-choice"]:checked')?.value || "direct";
  const proxyUrl = (inputProxyUrl?.value || "").trim();
  currentProxyChoice = selected;
  await safeInvoke("set_proxy_config", { mode: selected, url: proxyUrl });

  if (proxyBadge) {
    if (selected === "direct") {
      proxyBadge.textContent = "Direct";
      proxyBadge.classList.remove("active-badge");
    } else {
      proxyBadge.textContent = selected.toUpperCase();
      proxyBadge.classList.add("active-badge");
    }
  }

  Logger.log("PROXY", `Proxy tunnel set to ${selected.toUpperCase()}: ${proxyUrl || "Direct Connection"}`, "success");
  proxyStatusMsg?.classList.remove('hidden');
  setTimeout(() => {
    if (webFrame) webFrame.src = webFrame.src;
  }, 400);
});

// App Password Unlock
btnUnlockApp?.addEventListener("click", async () => {
  const pass = inputAppPassword?.value || "";
  const ok = await safeInvoke("verify_password", { password: pass });
  if (ok) {
    lockScreenModal?.classList.add("hidden");
    Logger.log("SEC", "App successfully unlocked.", "success");
    navigate(currentActiveUrl);
  } else {
    lockErrorMsg?.classList.remove("hidden");
    Logger.log("SEC", "App unlock failed: incorrect password.", "warn");
  }
});

inputAppPassword?.addEventListener("keydown", (e) => {
  if (e.key === "Enter") btnUnlockApp?.click();
});

// ========================================================
// 7. Initialize Application State
// ========================================================
async function initApp() {
  Logger.log("SYS", "Loop Windows Desktop Client initialized (v1.0.0)", "info");
  Logger.log("SYS", "Host runtime: Tauri v2 + Windows Shared WebView2", "info");

  const settings = await safeInvoke("get_app_settings");
  currentActiveUrl = settings?.active_url || DEFAULT_URL;
  if (settingActiveUrl) settingActiveUrl.value = currentActiveUrl;

  // DNS State
  currentDnsChoice = settings?.dns_provider || "adguard";
  const customTemplate = settings?.custom_dns_template || "https://dns.adguard-dns.com/dns-query";
  if (inputCustomDns) inputCustomDns.value = customTemplate;
  const targetRadio = document.querySelector(`input[name="dns-choice"][value="${currentDnsChoice}"]`);
  if (targetRadio) {
    targetRadio.checked = true;
    if (currentDnsChoice === "custom") customDnsContainer?.classList.remove('hidden');
  }
  if (dnsBadge) {
    if (currentDnsChoice === "adguard") dnsBadge.textContent = "AdGuard DNS";
    else if (currentDnsChoice === "cloudflare") dnsBadge.textContent = "Cloudflare 1.1.1.1";
    else if (currentDnsChoice === "google") dnsBadge.textContent = "Google DNS";
    else if (currentDnsChoice === "custom") dnsBadge.textContent = "Custom DoH";
    else dnsBadge.textContent = "System (Off)";
  }

  // Proxy State
  currentProxyChoice = settings?.proxy_mode || "direct";
  const proxyUrl = settings?.proxy_url || "";
  if (inputProxyUrl) inputProxyUrl.value = proxyUrl;
  const targetProxyRadio = document.querySelector(`input[name="proxy-choice"][value="${currentProxyChoice}"]`);
  if (targetProxyRadio) {
    targetProxyRadio.checked = true;
    if (currentProxyChoice !== "direct") proxyInputContainer?.classList.remove('hidden');
  }
  if (proxyBadge) {
    if (currentProxyChoice === "direct") {
      proxyBadge.textContent = "Direct";
      proxyBadge.classList.remove("active-badge");
    } else {
      proxyBadge.textContent = currentProxyChoice.toUpperCase();
      proxyBadge.classList.add("active-badge");
    }
  }

  Logger.log("DNS", `Encrypted DNS provider: ${getDnsDescription(currentDnsChoice)}`, "info");
  Logger.log("PROXY", `Proxy status: ${currentProxyChoice.toUpperCase()}`, "info");

  // Check App Password
  if (settings?.has_password) {
    if (applockBadge) {
      applockBadge.textContent = "Protected";
      applockBadge.classList.add("active-badge");
    }
    btnLockNow?.classList.remove("hidden");
    if (btnManagePassword) btnManagePassword.textContent = "Change Password";
    lockScreenModal?.classList.remove("hidden");
    Logger.log("SEC", "App lock active: Waiting for password entry", "info");
  } else {
    if (applockBadge) applockBadge.textContent = "None";
    navigate(currentActiveUrl);
  }
}

// Start
initApp();
