# Loop Windows: Custom App Window with Embedded Firefox Engine & Add-ons Support

Integrates the Firefox (Gecko) engine directly into Loop's custom window interface (the exact toolbar and titlebar from your screenshot), enabling direct installation of custom add-ons from `addons.mozilla.org`, strict TRR Mode 3 (AdGuard DoH), and Encrypted Client Hello (ECH) to stream blocked video hosts without ISP interference.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> 1. **Preserving the Custom Loop Window UI**:
>    - The user specifically requested: **"Keep current Loop window and enable extensions directly"**.
>    - We will retain the exact custom titlebar and header toolbar from the screenshot:
>      - Loop logo & brand
>      - Navigation controls: Back (`←`), Forward (`→`), Reload (`↻`), Home (`⌂`)
>      - **"Add-ons / Extensions"** button (`🧩 Add-ons`) directly in the header
>      - Direct View (`⤢`), System Log & Diagnostics (`>_`), and Settings (`⚙`)
> 2. **Enabling Custom Add-ons from `addons.mozilla.org`**:
>    - Chromium/WebView2 rejects `.xpi` extensions from `addons.mozilla.org` because Mozilla's store restricts installation to Firefox.
>    - By powering the underlying viewport with the **Firefox (Gecko)** engine inside the Loop window, the user can visit `addons.mozilla.org`, click **"Add to Firefox"**, and install any custom extension directly into their Loop profile without bundling.
> 3. **Fixing Video Host (`eporner.com`) ISP Blocks**:
>    - Video hosts inside sub-frames fail in WebView2 due to unencrypted SNI packet inspection and iframe sandbox restrictions.
>    - The Firefox Gecko engine utilizes **Strict TRR Mode 3 (`network.trr.mode = 3`)** with AdGuard DoH (`https://dns.adguard-dns.com/dns-query`) and **Encrypted Client Hello (ECH)** (`network.dns.echcache.enabled = true`, `security.tls.ech.grease_http3 = true`), completely bypassing ISP Deep Packet Inspection.

---

## 1. Architecture & Component Layout

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Loop Custom Window Interface                      │
├────────────────────────────────────────────────────────────────────────┤
│  [Loop Logo] [←] [→] [↻] [⌂]       Loop       [🧩 Add-ons] [>_] [⚙] [- □ ✕]│
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│                 Embedded Mozilla Firefox Engine (Gecko)                │
│                                                                        │
│   - Direct installation from addons.mozilla.org (uBlock, etc.)         │
│   - Strict TRR Mode 3 (100% Encrypted AdGuard DoH)                     │
│   - Encrypted Client Hello (ECH Anti-SNI Filtering)                    │
│   - Full video streaming support (Zero iframe sandbox limits)          │
│   - Isolated Loop profile (windows/profile/)                           │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technical Implementation Steps

### Step 1: Add-ons Button & Navigation in Loop Toolbar (`windows/index.html` & `windows/src/index.html`)
- In the top header toolbar, add an **"Add-ons"** button:
  ```html
  <button id="btn-addons" class="nav-btn" title="Install extensions from addons.mozilla.org">
    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
      <path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7s2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5s-1.12-2.5-2.5-2.5z"/>
    </svg>
    <span class="btn-label">Add-ons</span>
  </button>
  ```
- Clicking this button navigates to `https://addons.mozilla.org/firefox/addon/ublock-origin/` (or the AMO search portal), allowing instant manual 1-click installation.

### Step 2: Configure Dedicated Firefox Profile (`windows/profile/user.js`)
- Configure Firefox preferences to allow manual extension installations and bypass all ISP censorship:
  ```javascript
  // Strict Trusted Recursive Resolver (DoH Mode 3: 100% Encrypted)
  user_pref("network.trr.mode", 3);
  user_pref("network.trr.uri", "https://dns.adguard-dns.com/dns-query");
  user_pref("network.trr.bootstrapAddress", "94.140.14.14");
  // Encrypted Client Hello (Bypasses ISP SNI deep packet inspection)
  user_pref("network.dns.echcache.enabled", true);
  user_pref("network.dns.use_https_rr_as_alpn", true);
  user_pref("security.tls.ech.grease_http3", true);
  // Full extension freedom (Allow manual installation from AMO)
  user_pref("xpinstall.signatures.required", false);
  user_pref("extensions.autoDisableScopes", 0);
  user_pref("extensions.enabledScopes", 15);
  // Media and video stream playback
  user_pref("media.autoplay.default", 0);
  user_pref("media.eme.enabled", true);
  ```

### Step 3: Implement Window Integration & Controller (`windows/src-tauri/src/main.rs`)
- In the native Windows launcher:
  - Automatically launches the custom Loop window interface with the Firefox engine container.
  - Supports opening `addons.mozilla.org` directly within the Loop window.
  - Passes default portal `https://dcsplayer.blogspot.com/` (or custom URL if unlocked with `dcsus7`).
  - Hides the console window on launch.

### Step 4: Verification & GitHub Actions Pipeline
1. Verify `npm --prefix windows run build` completes cleanly without errors.
2. Verify `windows/src-tauri` Cargo build compiles cleanly.
3. Verify Android app compiles cleanly with `compile_applet`.
