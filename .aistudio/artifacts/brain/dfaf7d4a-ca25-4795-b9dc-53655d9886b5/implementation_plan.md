# Loop Windows: Custom Loop UI & Custom Branding (Non-Firefox Look)

Completely replaces the default Firefox browser chrome with a brand new custom **Loop UI**, and replaces the default Firefox icon with the custom **Loop App Icon** across the executable, taskbar, and window header.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> 1. **Complete Removal of Default Firefox UI & Creation of New Loop UI**:
>    - **Remove**: Completely collapse and remove the Firefox URL bar (`#urlbar-container`), tab strip (`#TabsToolbar`), bookmarks bar, and Firefox menus.
>    - **Create New UI**: Introduce a dedicated, custom-crafted **Loop Header Bar** that sits directly above the web content:
>      - Loop Logo & Brand Title
>      - Navigation controls: Back (`←`), Forward (`→`), Reload (`↻`), Home (`⌂`)
>      - **"Add-ons / Extensions"** button (`🧩 Add-ons`) to install uBlock Origin or custom add-ons from `addons.mozilla.org` in 1 click
>      - **"Settings"** button (`⚙`) opening the Loop Control Hub (URL configuration with code `dcsus7` and DoH diagnostics)
>      - Window drag region and clean caption controls
>
> 2. **Custom Loop App Icon (No Default Firefox Icon)**:
>    - Firefox by default displays the orange fox icon on the window and taskbar.
>    - **Window & Taskbar Icon**: Configure `profile/chrome/icons/default/main-window.ico` and `default.ico` with Loop's custom logo.
>    - **Executable Icon**: Embed `icon.ico` into `Loop.exe` via Windows PE resource script (`app.rc`) so the `.exe` file on disk and in the Windows taskbar displays the Loop icon.

---

## 1. Visual Layout of the New Custom Loop UI

```
┌────────────────────────────────────────────────────────────────────────┐
│ [Loop Icon] Loop   [←] [→] [↻] [⌂]       [🧩 Add-ons] [⚙ Settings] [- □ ✕]│
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│                      Web Content Area (Full Height)                    │
│                      (No URL bar & No Tab bar)                         │
│                                                                        │
│                                                                        │
│               - Strict AdGuard DoH (TRR Mode 3 Active)                 │
│               - Encrypted Client Hello (Anti-SNI Active)               │
│               - Video streams (eporner.com, etc.) load smoothly        │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technical Implementation Steps

### Step 1: Replace Firefox UI with the New Loop UI (`windows/profile/chrome/userChrome.css`)
- Hide the default Firefox chrome:
  - `#urlbar-container`, `#urlbar` -> `display: none !important;`
  - `#TabsToolbar` -> `display: none !important;`
  - `#PersonalToolbar` -> `display: none !important;`
- Construct the new custom **Loop Header**:
  - Style the navigation bar into a sleek 38px dark toolbar (`#1e1e1e`) with Loop branding.
  - Position the Back, Forward, Reload, and Home buttons with modern hover animations.
  - Add the custom **"Add-ons"** and **"Settings"** action buttons.
  - Maximize the web content area (`#browser`, `#appcontent`) to occupy the rest of the window.

### Step 2: Implement Custom Loop Icons & Window Branding
- Place Loop's `.ico` icon into `windows/profile/chrome/icons/default/main-window.ico` and `windows/profile/chrome/icons/default/default.ico`. Firefox uses this folder to override its window and taskbar icon.
- Configure Windows PE resource script (`app.rc`) in `windows/src-tauri/` to embed `icon.ico` directly into `Loop.exe`.

### Step 3: Configure Profile & Add-on Integration (`windows/profile/user.js`)
- Ensure `toolkit.legacyUserProfileCustomizations.stylesheets = true` is enabled.
- Retain strict TRR Mode 3 (AdGuard DoH: `https://dns.adguard-dns.com/dns-query`) and Encrypted Client Hello (ECH) to keep streaming video hosts unblocked.
- Set default homepage to `https://dcsplayer.blogspot.com/`.

### Step 4: Verification & GitHub Actions Build
1. Verify `npm --prefix windows run build` completes cleanly.
2. Verify Cargo compiles `Loop.exe` with embedded icon.
3. Verify Android app compiles cleanly with `compile_applet`.
