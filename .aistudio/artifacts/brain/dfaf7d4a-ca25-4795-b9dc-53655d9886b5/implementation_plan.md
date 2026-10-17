# Loop Windows Port: High-Performance Architecture & Blueprint

A standalone high-performance Windows desktop port of the Loop browser with ultra-low memory footprint, instant startup, and direct uBlock Origin extension support.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural decisions below directly align with your requirements for lowest memory usage, instantaneous responsiveness, direct uBlock Origin extension support, and a standalone Windows architecture:
>
> 1. **Engine & Runtime Choice (Tauri v2 + Rust + Windows Shared WebView2)**:
>    - **Why not Electron?** Electron bundles a private copy of Chromium and Node.js, running at 150–250MB idle memory.
>    - **Why not Firefox/Gecko?** Mozilla deprecated desktop embedding (GeckoView is Android-only; desktop GeckoFX is dead and unmaintained).
>    - **Chosen Stack**: Tauri v2 with Rust backend and native Windows WebView2. It consumes only ~30–45MB of RAM (over 4x lighter than Electron), starts in under 200ms, and leverages Windows 10/11's built-in OS-shared Edge rendering engine with hardware-accelerated DirectX rendering.
> 2. **Direct uBlock Origin Extension Loading**:
>    - Leverages WebView2's native `CoreWebView2Profile.AddBrowserExtensionAsync` API and `--load-extension` flags to load the unpacked uBlock Origin extension directly on startup without needing a full-blown browser installation.
> 3. **Delivery Structure**:
>    - Formatted as a standalone, production-ready Windows project architecture blueprint with complete Rust backend services, Windows Native host bindings, IPC bridge, and UI shell.

---

## 1. Overview & Core Concept

- **What It Does**: A standalone Windows desktop client mirroring the Loop Android browsing experience (custom portals, PIN/app lock, favorites, customizable home screens, and seamless ad-free web viewing) engineered for Windows 10 and 11.
- **Target Audience**: Users seeking a lightweight, distraction-free, ad-blocked desktop portal and browser that doesn't consume system resources while running in the background.
- **Key Value**: Delivers sub-50MB RAM consumption, native Windows Fluent Design visuals (Mica/Acrylic material backdrops), and full uBlock Origin content-blocking capabilities without the bloat of standard desktop browsers.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Launch & Authentication**: Instant launch (<200ms). If PIN/Password lock is enabled in settings, a native Windows Hello or PIN modal locks the interface before rendering session data.
2. **Browsing & Extension Shield**: Webpages render with uBlock Origin extension active in the profile, blocking telemetry, video ads, trackers, and banners silently.
3. **Loop Control Bar & Navigation**:
   - Modern Windows title bar with integrated address input, back/forward history navigation, reload, extension status badge, and settings gear.
   - Quick switch between the Loop Portal / Dashboard (favorites, quick bookmarks, clock, custom search) and the active web viewport.
4. **Settings & Customization**: Seamless panel for configuring homepage URL, search engine, ad-blocking strictness, and app lock security.

### Visual Identity & Theme
- **Aesthetic Direction**: Windows 11 Fluent Design with semi-transparent Mica backdrop material, subtle border strokes, and dark/light system theme harmony.
- **Color Palette & Accent**:
  - Primary Accent: Windows modern blue / indigo (`#0078D4` / `#4F46E5`) matching Loop's brand identity.
  - Backgrounds: Dynamic Mica surface (`#F3F3F3` in Light / `#202020` in Dark) with layered card elevations (`#FFFFFF` / `#2B2B2B`).
  - Text: High-contrast typography (`Segoe UI Variable` / `Segoe UI` system fonts).
- **Interactive Feedback**: Native Windows hover states, smooth CSS transform transitions, keyboard shortcuts (Alt+Left for Back, Ctrl+R for Reload, Ctrl+L to focus address bar), and discreet badge toasts.

---

## 3. Key Product Decisions & Trade-Offs

- **Tech Stack: Tauri v2 (Rust) vs. .NET WinUI 3 vs. Electron**:
  - *Chosen*: Tauri v2 (Rust) using Windows WebView2 runtime.
  - *Why*: Delivers the smallest binary size (~5MB installer vs ~90MB for Electron), minimal memory consumption (shared OS memory pages), memory safety, and high-speed native IPC.
  - *Alternatives Considered*: 
    - Electron was rejected due to heavy RAM overhead (150-300MB) and sluggish cold start.
    - Raw WinUI 3 was considered, but Tauri provides cross-engine portability and easier integration with modern reactive UI shells and local Rust SQLite storage.
- **Extension Integration Strategy**:
  - *Chosen*: Pre-packaged unpacked uBlock Origin WebExtension in the application assets folder, registered via WebView2's extension initialization hooks (`CoreWebView2Profile.AddBrowserExtensionAsync` / Chromium extension arguments).
  - *Why*: Allows true zero-compromise filtering matching the user's explicit requirement for uBlock Origin rules and cosmetic filtering.
- **Persistence Layer**:
  - *Chosen*: Local SQLite database managed via Rust (`rusqlite`) or local JSON storage, mirroring the Android Room entities (`SavedUrlEntity`, `SettingEntity`).

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                   Loop Windows Desktop App                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────┐        ┌─────────────────────────┐  │
│  │     Loop UI Shell       │  IPC   │     Rust Host Core      │  │
│  │ (TitleBar, URL Bar,     │◄──────►│ (Tauri v2 App Engine)   │  │
│  │  Portal, Bookmarks, PIN)│        │                         │  │
│  └───────────┬─────────────┘        └────────────┬────────────┘  │
│              │                                   │               │
│              ▼                                   ▼               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │             Windows WebView2 Runtime Engine                │  │
│  │  ┌───────────────────────┐      ┌───────────────────────┐  │  │
│  │  │ Web Viewport Canvas   │      │ uBlock Origin Core    │  │  │
│  │  │ (DirectX Accelerated) │◄────►│ (Unpacked Extension)  │  │  │
│  │  └───────────────────────┘      └───────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                  │                               │
│                                  ▼                               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │          Local App Data / SQLite (PIN, URLs, Config)       │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Component Breakdown & Implementation Blueprint

1. **Rust Core Configuration (`src-tauri/tauri.conf.json`)**:
   - Window configuration with Mica/Acrylic effects enabled (`titleBarStyle: "Overlay"`).
   - WebView2 custom initialization arguments enabling extension loading (`--load-extension=...`).
2. **Extension Loader (`src-tauri/src/main.rs`)**:
   - Resolves the unpacked uBlock Origin extension directory from the application resource bundle.
   - Configures the WebView2 environment options and profile hooks to inject the extension before any web navigation occurs.
3. **Database & Settings Module (`src-tauri/src/db.rs`)**:
   - SQLite tables matching Android schema: `saved_urls` (id, title, url, timestamp) and `settings` (key, value, salt/hash for PIN).
4. **Desktop UI Shell (`src/`)**:
   - Responsive layout adapting to multi-monitor setups, snap layouts, and keyboard shortcuts.
   - Omnibox with auto-completion, bookmark management, search engine switching, and app lock security dialog.
