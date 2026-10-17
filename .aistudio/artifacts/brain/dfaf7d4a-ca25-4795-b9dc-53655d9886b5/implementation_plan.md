# Loop Windows: Native Webview & Proxy Architecture Plan

Eliminates `X-Frame-Options` and CSP load errors by migrating from an embedded `<iframe>` to a native top-level Webview, and adds full SOCKS5/HTTP proxy support to bypass country-level ISP firewalls and SNI filtering.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural updates below directly resolve the load failures and country-level censorship blocks:
>
> 1. **Native Top-Level Webview (Eliminates `X-Frame-Options`)**:
>    - **Problem**: Modern sites (including blogspot, streaming players, banking, and secured portals) send `X-Frame-Options: SAMEORIGIN` or `frame-ancestors: 'self'`. When loaded inside an `<iframe>`, Chromium/WebView2 blocks them and shows the "webpage might be having issues" error.
>    - **Solution**: Load the website directly as the top-level document in WebView2. This gives the site full native browser privileges with zero iframe restrictions, zero cookie sandboxing, and full DRM/player compatibility.
> 2. **SOCKS5 & HTTP Proxy Support in Settings**:
>    - **Problem**: Many countries use Deep Packet Inspection (DPI) to block domains at the TLS SNI (Server Name Indication) or IP level. In these regions, DNS-over-HTTPS alone cannot connect.
>    - **Solution**: Add configurable Proxy / SOCKS5 tunneling (`socks5://`, `http://`, `https://`) in Settings. Users can easily route their traffic through local proxy clients (such as Shadowsocks, V2Ray, Clash, Cloudflare WARP, or Psiphon) or remote proxy servers.
> 3. **Native Window Management**:
>    - Enable native Windows decorations with Mica/Acrylic so Windows natively handles drag, snap layouts, minimize, maximize, and exit controls seamlessly without interference from web content.

---

## 1. Overview & Core Concept

- **What It Delivers**: A completely uninhibited Windows browsing client where any webpage loads reliably regardless of iframe security policies or regional ISP blocking.
- **Target Use Case**: Users accessing restricted, geo-blocked, or security-hardened websites that fail in ordinary embedded frame browsers.
- **Key Advantage**: Combines direct uBlock Origin extension filtering with full SOCKS5 proxy tunneling and native WebView rendering.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Web Viewing**: The website renders directly full-frame in the application window. No `X-Frame-Options` or iframe blocking errors can occur.
2. **Accessing Settings & Security**:
   - Pressing `Ctrl+,` (or `F1` / clicking the floating access button) brings up the sleek Loop Settings drawer over the active page.
   - Users can update the Main Page URL (unlocked via `dcsus7`), change DNS resolvers, or configure SOCKS5/HTTP proxies.
3. **Configuring SOCKS5 / HTTP Proxy**:
   - Inside Settings, a new **"Proxy & Firewall Bypass"** card provides options for:
     - **Direct Connection** (Default / No Proxy)
     - **SOCKS5 Proxy** (e.g., `127.0.0.1:1080` for Shadowsocks/V2Ray/WARP)
     - **HTTP / HTTPS Proxy** (e.g., `proxy.mycompany.com:8080`)
     - **Bypass List** (`<local>, 127.0.0.1, localhost`)
   - Saves instantly to the SQLite database and updates the WebView2 proxy switches.

---

## 3. Technical Architecture & Component Mapping

### System Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                   Loop Windows (Native Host)                     │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────┐        ┌─────────────────────────┐  │
│  │     Rust Host Core      │◄──────►│   SQLite Local Storage  │  │
│  │ (Tauri v2 + WebView2)   │        │ (Proxy, URL, Passwords) │  │
│  └───────────┬─────────────┘        └─────────────────────────┘  │
│              │                                                   │
│              ▼                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                Top-Level WebView2 Runtime                  │  │
│  │  ┌──────────────────────────────────────────────────────┐  │  │
│  │  │ Native Top-Level Page (Zero iframe restrictions)     │  │  │
│  │  │ - uBlock Origin Active                               │  │  │
│  │  │ - AdGuard DoH DNS Resolver                           │  │  │
│  │  │ - SOCKS5 / HTTP Tunnel Active                        │  │  │
│  │  └──────────────────────────────────────────────────────┘  │  │
│  │  ┌──────────────────────────────────────────────────────┐  │  │
│  │  │ Injected Loop Controls & Overlay HUD (Ctrl+,)        │  │  │
│  │  └──────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Key Technical Changes

1. **`windows/src-tauri/src/main.rs` & `adblock.rs`**:
   - Ingest `proxy_mode`, `proxy_host`, `proxy_port`, and `proxy_bypass` from `DatabaseManager`.
   - Append `--proxy-server=<protocol>://<host>:<port>` and `--proxy-bypass-list=<list>` to `WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS`.
   - Add IPC command `set_proxy_config(mode, host, port, bypass)`.
2. **`windows/src-tauri/tauri.conf.json`**:
   - Set `"decorations": true` for native Windows 11 Fluent titlebar and drag-snap behavior, avoiding custom titlebar bugs while providing full native window controls.
   - Configure `--disable-web-security` and `--disable-features=IsolateOrigins,site-per-process` as additional safeguards.
3. **Settings UI (`windows/index.html` & `windows/src/main.js`)**:
   - Add the dedicated **Proxy & Firewall Bypass** card with SOCKS5 / HTTP toggles, host/port inputs, and test status indicators.
   - Update navigation logic to load directly in top-level webview.
