# Loop Windows: Window Controls Fix, Top-Level Navigation & System Log

Fixes the double window controls bug, eliminates iframe loading restrictions via direct top-level navigation, and introduces an integrated System Log and Diagnostic Console.

## User Review & Critical Decisions

> [!IMPORTANT]
> 1. **Removal of Redundant HTML Window Controls**:
>    - **Root Cause**: `tauri.conf.json` had `"decorations": true` enabled (native Windows 11 title bar), while `index.html` was still rendering custom HTML caption buttons (`_`, `□`, `✕`), resulting in two sets of window controls.
>    - **Fix**: Remove the custom HTML caption buttons. Windows 11 natively provides the standard minimize, maximize, snap layouts, and close buttons on the OS title bar.
> 2. **Eliminating Website Load Failures via Direct Top-Level Navigation**:
>    - **Root Cause**: Websites that prohibit framing (e.g., Blogspot, streaming players, Google authentication) refuse to load inside `<iframe>` tags regardless of flags.
>    - **Fix**: Support direct top-level webview navigation (exactly matching Android's `WebView.loadUrl(url)`), completely removing the `<iframe>` constraint so all websites render with full desktop privileges.
> 3. **Integrated System Log & Diagnostic Console**:
>    - Provide a real-time Diagnostics panel in Settings (and via shortcut `Ctrl+Shift+L` or `F12`) displaying:
>      - Active DoH resolver (`https://dns.adguard-dns.com/dns-query`) and status
>      - Proxy configuration (`direct`, `socks5://...`, or `http://...`)
>      - Chromium network events, HTTP status codes, and error codes (`ERR_NAME_NOT_RESOLVED`, `ERR_CONNECTION_REFUSED`, etc.)
>      - One-click "Switch to Top-Level Native View" if an iframe ever encounters an error.

---

## 1. Overview & Core Concept

- **What It Does**: Cleans up the desktop interface to look and feel like a native Windows 11 app without visual glitches, ensures 100% webpage compatibility, and provides transparency into network/DNS health through live diagnostic logging.
- **Key Value**: Users can immediately identify why a website might be failing (DNS failure vs. ISP block vs. certificate error) and resolve it instantly with DoH or SOCKS5 proxies.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Window Experience**:
   - Single, clean native Windows 11 title bar with standard OS caption buttons and Snap Layout support.
   - Loop controls (Back, Forward, Reload, Home, uBlock shield, Settings gear) are neatly positioned in the header bar.
2. **Accessing Diagnostics & System Log**:
   - Open Settings (`Ctrl+,` or click gear icon).
   - Click the **"Diagnostics & System Log"** card to open the live console.
   - Shows active browser switches, DoH endpoints, proxy status, and a scrolling event log with timestamps.
3. **Seamless Site Loading**:
   - If a website blocks framing, Loop automatically logs the error and provides a one-click button to load it directly in the top-level native webview.

---

## 3. Technical Implementation Strategy

### Component Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                   Loop Windows (Native Host)                     │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────┐        ┌─────────────────────────┐  │
│  │   Loop Header Controls  │        │   Diagnostic Console    │  │
│  │   (Back, Reload, Home,  │        │   (Live Network & DNS   │  │
│  │    uBlock, Settings)    │        │    System Event Logs)   │  │
│  └───────────┬─────────────┘        └────────────┬────────────┘  │
│              │                                   │               │
│              ▼                                   ▼               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │             Windows WebView2 Runtime Engine                │  │
│  │  - Native Windows 11 OS Controls (decorations: true)       │  │
│  │  - Top-Level Document Navigation (Zero iframe limits)      │  │
│  │  - Encrypted AdGuard DoH DNS Resolver                      │  │
│  │  - SOCKS5 / HTTP Proxy Tunneling                           │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Specific Changes
1. **`windows/index.html` & `windows/src/index.html`**:
   - Remove `<div class="window-controls">` and its caption buttons.
   - Add the **Diagnostics & System Log** panel and modal dialog with real-time log stream.
   - Add a fallback mechanism for direct top-level navigation.
2. **`windows/src/style.css`**:
   - Remove custom caption button styling to prevent visual duplication.
   - Style the dark-themed diagnostic console (monospaced logs, colored status chips, auto-scrolling).
3. **`windows/src/main.js`**:
   - Implement `Logger` service capturing all navigation events, DNS status, and network errors.
   - Wire diagnostic console triggers and export log functionality.
