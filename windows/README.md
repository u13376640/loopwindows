# Loop Windows: 100% Firefox-Based Desktop Application

A dedicated desktop client for **Loop** built entirely on the **Mozilla Firefox (Gecko)** engine.

---

## Why Firefox (Gecko) Over Chromium?

1. **Native Firefox Extension Support**:
   - Allows installing full-strength Firefox extensions such as **uBlock Origin** without Chromium Manifest V3 limits.
   - **No bundled extensions**: A dedicated **"🧩 Install uBlock Origin Extension"** button is provided on the Control Hub (`profile/home.html`) that opens the official Mozilla Add-ons store in 1 click for manual installation.
2. **Strict TRR Mode 3 (100% Encrypted DNS)**:
   - Configured via `network.trr.mode = 3`.
   - Forces all domain requests through **AdGuard DoH (`https://dns.adguard-dns.com/dns-query`)**, completely bypassing ISP DNS blocking.
3. **Encrypted Client Hello (ECH)**:
   - Prevents ISP Deep Packet Inspection (SNI filtering) from inspecting the requested domain during the TLS handshake.
   - Resolves loading failures on sub-resource video hosts (e.g., `eporner.com` embedded players).
4. **Isolated Loop Profile**:
   - Stores all preferences, cache, and extensions in `./profile/`, completely isolated from any personal Firefox installations.

---

## How to Run

### Option 1: 1-Click Launch (No compilation needed)
Double-click:
```
Loop.bat
```
This automatically finds your Firefox installation (or downloads portable Firefox into `./Firefox/` in 1 click if not present), loads the `./profile` with strict AdGuard DoH and ECH, and opens your default portal.

### Option 2: Native Executable (`Loop.exe`)
Run `build-windows.bat`. If you have Rust/Cargo installed, it compiles `Loop.exe`. Then double-click `Loop.exe`.

---

## Features & Controls

- **Android-Style Clean App Interface**:
  - The Firefox browser URL bar, tabs bar, and navigation chrome are completely removed via custom `userChrome.css`.
  - Content runs in clean edge-to-edge view, just like the Loop Android app.
  - A sleek, minimal floating button/pill in the top corner provides access to **Add-ons (uBlock Origin)**, **Settings**, **Back**, and **Reload**.
- **Custom Application Icon**:
  - Custom Loop logo (`main-window.ico` / `main-window.png`) replaces the standard Firefox branding.
- **Default Homepage**: `https://dcsplayer.blogspot.com/`
- **Control Hub**: `profile/home.html` (Accessible anytime via `file:///.../profile/home.html` or Home action)
  - **Install uBlock Origin Button**: 1-click link to Mozilla Add-ons.
  - **URL Customization**: Unlock with authorization code `dcsus7`.
  - **Live DoH Probe**: Real-time DNS resolution test against AdGuard DoH with multi-provider testing.
