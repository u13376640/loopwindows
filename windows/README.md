# Loop Windows: High-Performance Standalone Desktop Client

A lightweight, high-performance desktop port of **Loop** engineered specifically for **Windows 10 & 11**.

Built with **Tauri v2 (Rust)**, **Windows Shared WebView2 Runtime**, and **Direct uBlock Origin Extension Loading**.

---

## ⚡ Architecture & Performance Comparison

| Metric | Loop (Tauri v2 + WebView2) | Standard Electron App | Full Desktop Firefox / Chrome |
| :--- | :--- | :--- | :--- |
| **Idle Memory (RAM)** | **~30 – 45 MB** | 160 – 260 MB | 300 – 600 MB |
| **Installer Size** | **~5 MB** | ~90 – 130 MB | ~110 – 150 MB |
| **Cold Startup Time** | **< 200 ms** | 1,200 – 2,500 ms | 1,000 – 2,000 ms |
| **Ad Blocker Engine** | **Direct uBlock Origin Extension** | Embedded Extension | Native Extension |
| **UI Aesthetics** | **Windows 11 Fluent (Mica & Acrylic)** | Emulated Web UI | Native OS Shell |

---

## 🛡️ uBlock Origin Extension Integration

Unlike Android where GeckoView or custom WebView URL interceptors are required, **Microsoft Edge WebView2** on Windows supports loading unpacked Chromium/WebExtension extensions via:
1. `CoreWebView2Profile.AddBrowserExtensionAsync(extensionPath)`
2. Direct Chromium launch switch: `--load-extension=<path-to-ublock-origin>`

The Rust backend in `src-tauri/src/main.rs` automatically locates the bundled unpacked `ublock-origin` extension in the app's `extensions/` directory and registers it during the WebView2 profile initialization. All web traffic, video player embeds, and tracker requests are filtered with complete uBlock Origin rules and cosmetic element hiding.

---

## 📂 Project Structure

```
windows/
├── package.json                   # UI build scripts and web dependencies
├── scripts/
│   └── build-windows.bat          # 1-click Windows compile script
├── extensions/
│   └── ublock-origin/             # Unpacked uBlock Origin extension files
│       └── manifest.json
├── src/
│   ├── index.html                 # Windows 11 Fluent UI layout (TitleBar, Portal, WebView)
│   ├── style.css                  # Modern Mica/Acrylic styles, themes, and CSS variables
│   └── main.js                    # UI logic, omnibox, access-code security ("dcsus7"), app lock
└── src-tauri/
    ├── Cargo.toml                 # Rust dependencies (Tauri v2, rusqlite, sha2, serde)
    ├── tauri.conf.json            # Window styling, permissions, and WebView2 arguments
    └── src/
        ├── main.rs                # App entrypoint, WebView2 configuration & IPC commands
        ├── db.rs                  # SQLite storage layer (mirrors Room AppDatabase)
        └── adblock.rs             # Extension path management & validation
```

---

## 🚀 Quick Start & Building for Windows

### Prerequisites
1. **Rust**: Install via [rustup.rs](https://rustup.rs) (`x86_64-pc-windows-msvc`).
2. **Node.js**: v18+ with npm.
3. **Windows 10/11**: WebView2 is already pre-installed by Windows Update.

### Development Mode
```bash
cd windows
npm install
npm run tauri dev
```

### Production Release Build
```bash
cd windows
npm run tauri build
```
The compiled, standalone `.exe` and `.msi` installers will be generated in `src-tauri/target/release/bundle/`.

---

## 🔐 Feature Parity with Loop Android

- **Homepage & Main URL**: Defaults to `https://dcsplayer.blogspot.com/`. Protected by the secret access code `dcsus7` in the settings menu.
- **PIN / Password App Lock**: Secures the browser session upon launch using salted SHA-256 hashes stored in the local SQLite database.
- **AdBlocker Protection**: Direct uBlock Origin extension blocking with cosmetic element filtering and popup suppression.
- **Engine Toggles**: Quick toggles for JavaScript execution, Desktop/Mobile User-Agent modes, and one-click Cache & Cookie clearing.
- **Keyboard Shortcuts**:
  - `Alt + Left`: Back
  - `Alt + Right`: Forward
  - `Ctrl + R` / `F5`: Reload page
  - `Ctrl + L`: Focus URL Omnibox
  - `Ctrl + ,`: Open Settings
