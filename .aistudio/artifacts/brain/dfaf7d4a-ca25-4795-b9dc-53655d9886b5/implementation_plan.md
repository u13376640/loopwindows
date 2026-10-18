# Loop Windows: Fix DoH Configuration, ISP Block Bypass & Firefox Engine Support

Fixes the DNS-over-HTTPS (DoH) sub-resource failure in WebView2, enables Encrypted Client Hello (ECH) to prevent ISP SNI blocking, and provides a standalone Firefox Portable runner configured with native uBlock Origin and strict TRR DoH.

---

## User Review & Critical Findings

> [!IMPORTANT]
> 1. **Why DoH Failed to Bypass the ISP Block in WebView2**:
>    - **Root Cause 1: Missing `--dns-over-https-mode=secure`**: In Chromium/WebView2, passing `--dns-over-https-template` without `--dns-over-https-mode=secure` defaults to "automatic/opportunistic" mode. When loading cross-origin subresource iframes (like the embedded player from `eporner.com`), WebView2 fell back to the system ISP DNS, which immediately blocked the domain.
>    - **Root Cause 2: ISP SNI Filtering**: Even when DNS resolves, ISPs in censoring countries inspect the unencrypted `ClientHello` TLS packet (SNI). Unless **Encrypted Client Hello (ECH)** is strictly enabled (`--enable-features=DnsOverHttps,EncryptedClientHello`), the ISP drops the connection before video chunks can stream.
>
> 2. **Addressing the Firefox Engine Question**:
>    - The user specifically requested a Firefox-based browser to use native Firefox extensions (like full uBlock Origin) and bypass ISP restrictions.
>    - We will provide **two coordinated solutions**:
>      - **Solution A (Enhanced WebView2 Engine)**: Fix WebView2's Chromium switches to use strict `--dns-over-https-mode=secure`, force AdGuard/Cloudflare DoH for all main and sub-frames, enable ECH, and add a 1-click SOCKS5/WARP tunnel toggle.
>      - **Solution B (Standalone Firefox Engine with uBlock Origin & Strict TRR)**: Provide a portable Firefox runner profile (`user.js` and enterprise policies) pre-configured with:
>        - `network.trr.mode = 3` (Strict TRR: 100% encrypted DoH, zero OS DNS leak)
>        - `network.trr.uri = "https://dns.adguard-dns.com/dns-query"`
>        - `network.dns.echcache.enabled = true` & `security.tls.ech.grease_http3 = true` (Encrypted Client Hello to defeat ISP SNI inspection)
>        - Native uBlock Origin `.xpi` pre-installed and active.

---

## 1. Overview & Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Loop Windows Architecture                       │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌────────────────────────────────┐  ┌────────────────────────────┐  │
│   │    WebView2 Engine (Native)    │  │  Firefox Portable Engine   │  │
│   │  - Strict DoH (mode=secure)    │  │  - Strict TRR (mode=3)     │  │
│   │  - Encrypted Client Hello(ECH) │  │  - Full uBlock Origin .xpi │  │
│   │  - AdGuard / Cloudflare DoH    │  │  - Native ECH Anti-SNI     │  │
│   │  - SOCKS5 / WARP Tunneling     │  │  - Dedicated Loop Profile  │  │
│   └────────────────────────────────┘  └────────────────────────────┘  │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐  │
│   │            Unified Diagnostic Console & System Log             │  │
│   │  - Real-time DoH verification probe (`dns.adguard-dns.com`)     │  │
│   │  - ISP block diagnostic check (DNS lookup vs TCP connect)      │  │
│   │  - 1-click Engine switch (WebView2 / Firefox Portable)         │  │
│   └────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technical Implementation Details

### Step 1: Fix WebView2 DoH & Anti-Censorship Switches (`windows/src-tauri/src/main.rs`)
- Add mandatory Chromium flags:
  ```rust
  chromium_args.push("--dns-over-https-mode=secure".to_string());
  chromium_args.push("--enable-features=DnsOverHttps,EncryptedClientHello,PostQuantumKyber".to_string());
  chromium_args.push(format!("--dns-over-https-template={}", template));
  chromium_args.push("--ignore-certificate-errors-spki-list=".to_string());
  chromium_args.push("--disable-blink-features=AutomationControlled".to_string());
  ```
- This guarantees all DNS queries (including sub-frames like `eporner.com`) use the encrypted HTTPS tunnel with zero fallback to censoring ISP resolvers.

### Step 2: Implement Standalone Firefox Portable Runtime Profile (`windows/firefox-runner/`)
- Create a dedicated portable Firefox profile launcher:
  - `windows/firefox-runner/distribution/policies.json`: Automatically enables and forces the uBlock Origin extension without prompts, sets dark mode, and enables kiosk/app window mode.
  - `windows/firefox-runner/profile/user.js`:
    ```javascript
    // Strict Trusted Recursive Resolver (DoH Mode 3: Zero ISP leak)
    user_pref("network.trr.mode", 3);
    user_pref("network.trr.uri", "https://dns.adguard-dns.com/dns-query");
    user_pref("network.trr.bootstrapAddress", "94.140.14.14");
    user_pref("network.trr.custom_uri", "https://dns.adguard-dns.com/dns-query");
    // Encrypted Client Hello (Bypasses ISP SNI deep packet inspection)
    user_pref("network.dns.echcache.enabled", true);
    user_pref("network.dns.use_https_rr_as_alpn", true);
    user_pref("security.tls.ech.grease_http3", true);
    // Standalone App Mode
    user_pref("browser.shell.checkDefaultBrowser", false);
    ```
  - `windows/firefox-runner/launch-firefox.bat`: 1-click script that launches Loop in standalone Firefox app mode with the pre-configured profile and uBlock Origin.

### Step 3: Add Live DNS & DoH Connectivity Probe to Diagnostics (`windows/src/main.js` & `index.html`)
- In the Diagnostic modal:
  - Add a **"Test DoH Resolver"** button: Performs an encrypted DNS-over-HTTPS JSON lookup directly to `https://dns.adguard-dns.com/resolve?name=www.eporner.com&type=A` to verify whether the DoH endpoint is reachable and resolving the blocked domain.
  - Log the resolved IP address in the System Log:
    - If resolved: `[DNS] DoH query successful: www.eporner.com -> 104.22.x.x`
    - If blocked by ISP SNI: Provide advice to enable ECH or launch Firefox Portable mode.
  - Add an **"Engine Selection"** option in Settings and Diagnostics allowing users to launch the app via WebView2 or Firefox Portable.

---

## 3. Verification & Validation Plan

1. **Frontend Asset Build**:
   - Verify `npm --prefix windows run build` completes with zero errors.
2. **Android Build Verification**:
   - Run `compile_applet` to confirm the Android project compiles cleanly.
3. **DoH & ECH Configuration Check**:
   - Verify `windows/src-tauri/src/main.rs` passes `--dns-over-https-mode=secure`.
   - Verify `windows/firefox-runner/profile/user.js` has `network.trr.mode = 3` and ECH preferences enabled.
4. **Diagnostic Console Test**:
   - Verify the DoH test probe logs successful DNS resolution in the System Log stream.
