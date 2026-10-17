// Loop Windows Desktop Client Controller
import { invoke } from '@tauri-apps/api/core';
import { getCurrentWindow } from '@tauri-apps/api/window';

const DEFAULT_URL = "https://dcsplayer.blogspot.com/";

// Initialize Window Instance
let appWindow;
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
        blocked_ads_count: 142
      };
    }
    if (cmd === "verify_access_code") return args.code === "dcsus7";
    if (cmd === "verify_password") return args.password === localStorage.getItem("loop_password");
    if (cmd === "set_active_url") {
      localStorage.setItem("loop_url", args.url);
      return args.url;
    }
    return null;
  }
};

// DOM Elements
const webFrame = document.getElementById("web-frame");
const loadingBar = document.getElementById("loading-bar");
const btnBack = document.getElementById("btn-back");
const btnForward = document.getElementById("btn-forward");
const btnReload = document.getElementById("btn-reload");
const btnHome = document.getElementById("btn-home");
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

// App Lock Elements
const lockScreenModal = document.getElementById("lock-screen-modal");
const inputAppPassword = document.getElementById("input-app-password");
const btnUnlockApp = document.getElementById("btn-unlock-app");
const lockErrorMsg = document.getElementById("lock-error-msg");
const applockBadge = document.getElementById("applock-badge");
const btnManagePassword = document.getElementById("btn-manage-password");
const btnLockNow = document.getElementById("btn-lock-now");

// Caption Controls
const btnWinMin = document.getElementById("btn-win-min");
const btnWinMax = document.getElementById("btn-win-max");
const btnWinClose = document.getElementById("btn-win-close");

let currentActiveUrl = DEFAULT_URL;
let isUrlCardUnlocked = false;

// 1. Window Dragging & Caption Handlers
if (appWindow) {
  // Titlebar drag regions
  document.querySelectorAll('[data-tauri-drag-region]').forEach((el) => {
    el.addEventListener('mousedown', (e) => {
      // Ignore if clicking on interactive buttons/inputs
      if (e.target.closest('button, input, select, a, .interactive')) return;
      if (e.detail === 2) {
        // Double-click titlebar to maximize / restore
        appWindow.toggleMaximize().catch(() => safeInvoke("window_maximize_toggle"));
      } else {
        appWindow.startDragging().catch(() => safeInvoke("window_start_dragging"));
      }
    });
  });

  // Minimize
  btnWinMin.addEventListener('click', (e) => {
    e.stopPropagation();
    appWindow.minimize().catch(() => safeInvoke("window_minimize"));
  });

  // Maximize / Restore
  btnWinMax.addEventListener('click', (e) => {
    e.stopPropagation();
    appWindow.toggleMaximize().catch(() => safeInvoke("window_maximize_toggle"));
  });

  // Close
  btnWinClose.addEventListener('click', (e) => {
    e.stopPropagation();
    appWindow.close().catch(() => safeInvoke("window_close"));
  });
} else {
  // Fallback direct IPC for window controls
  btnWinMin.addEventListener('click', () => safeInvoke("window_minimize"));
  btnWinMax.addEventListener('click', () => safeInvoke("window_maximize_toggle"));
  btnWinClose.addEventListener('click', () => safeInvoke("window_close"));
}

// 2. Navigation
function navigate(url) {
  let finalUrl = url.trim();
  if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
    finalUrl = "https://" + finalUrl;
  }
  loadingBar.classList.remove("hidden");
  webFrame.src = finalUrl;
}

webFrame.addEventListener("load", () => {
  loadingBar.classList.add("hidden");
});

btnBack.addEventListener("click", () => {
  try { webFrame.contentWindow.history.back(); } catch (_) {}
});

btnForward.addEventListener("click", () => {
  try { webFrame.contentWindow.history.forward(); } catch (_) {}
});

btnReload.addEventListener("click", () => {
  loadingBar.classList.remove("hidden");
  webFrame.src = webFrame.src;
});

btnHome.addEventListener("click", () => {
  navigate(currentActiveUrl);
});

// 3. Settings Drawer & Keyboard Shortcuts
btnSettings.addEventListener("click", () => {
  settingsDrawer.classList.toggle("hidden");
});

btnCloseSettings.addEventListener("click", () => {
  settingsDrawer.classList.add("hidden");
});

document.addEventListener("keydown", (e) => {
  if (e.ctrlKey && e.key === ",") {
    e.preventDefault();
    settingsDrawer.classList.toggle("hidden");
  } else if (e.key === "Escape") {
    settingsDrawer.classList.add("hidden");
    accessCodeModal.classList.add("hidden");
  } else if (e.key === "F5" || (e.ctrlKey && e.key.toLowerCase() === "r")) {
    e.preventDefault();
    loadingBar.classList.remove("hidden");
    webFrame.src = webFrame.src;
  }
});

// 4. Access Code ("dcsus7") Protection for Main Page URL
btnUnlockUrl.addEventListener("click", () => {
  if (isUrlCardUnlocked) {
    isUrlCardUnlocked = false;
    urlLockStatus.textContent = "🔒 Locked";
    urlEditPanel.classList.add("hidden");
  } else {
    inputAccessCode.value = "";
    accessCodeErr.classList.add("hidden");
    accessCodeModal.classList.remove("hidden");
    inputAccessCode.focus();
  }
});

btnVerifyAccess.addEventListener("click", async () => {
  const code = inputAccessCode.value.trim();
  const valid = await safeInvoke("verify_access_code", { code });
  if (valid || code === "dcsus7") {
    isUrlCardUnlocked = true;
    urlLockStatus.textContent = "🔓 Unlocked";
    urlEditPanel.classList.remove("hidden");
    settingActiveUrl.value = currentActiveUrl;
    accessCodeModal.classList.add("hidden");
  } else {
    accessCodeErr.classList.remove("hidden");
  }
});

inputAccessCode.addEventListener("keydown", (e) => {
  if (e.key === "Enter") btnVerifyAccess.click();
});

btnCancelAccess.addEventListener("click", () => {
  accessCodeModal.classList.add("hidden");
});

btnApplyUrl.addEventListener("click", async () => {
  const newUrl = settingActiveUrl.value.trim();
  if (newUrl) {
    const res = await safeInvoke("set_active_url", { url: newUrl, accessCode: "dcsus7" });
    currentActiveUrl = res || newUrl;
    navigate(currentActiveUrl);
    settingsDrawer.classList.add("hidden");
  }
});

btnDefaultUrl.addEventListener("click", async () => {
  const res = await safeInvoke("reset_default_url", { accessCode: "dcsus7" });
  currentActiveUrl = res || DEFAULT_URL;
  settingActiveUrl.value = currentActiveUrl;
  navigate(currentActiveUrl);
});

// 5. Initialize Application State
async function initApp() {
  const settings = await safeInvoke("get_app_settings");
  currentActiveUrl = settings?.active_url || DEFAULT_URL;
  settingActiveUrl.value = currentActiveUrl;

  // Check App Lock Password
  if (settings?.has_password) {
    applockBadge.textContent = "Protected";
    applockBadge.classList.add("active-badge");
    btnLockNow.classList.remove("hidden");
    btnManagePassword.textContent = "Change Password";
    lockScreenModal.classList.remove("hidden");
  } else {
    applockBadge.textContent = "None";
    navigate(currentActiveUrl);
  }
}

btnUnlockApp.addEventListener("click", async () => {
  const pass = inputAppPassword.value;
  const ok = await safeInvoke("verify_password", { password: pass });
  if (ok) {
    lockScreenModal.classList.add("hidden");
    navigate(currentActiveUrl);
  } else {
    lockErrorMsg.classList.remove("hidden");
  }
});

inputAppPassword.addEventListener("keydown", (e) => {
  if (e.key === "Enter") btnUnlockApp.click();
});

// Start
initApp();
