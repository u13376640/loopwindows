# Loop

**Loop** is a modern, minimalist Android hybrid web application and portal built with Kotlin and Jetpack Compose. It pairs an immersive, borderless web viewing experience for `https://dcsplayer.blogspot.com/` (or any custom user-configured endpoint) with an invisible **AdGuard Private DNS** ad-blocking engine, pull-to-refresh UX, app password security, and stealth admin controls.

---

## 📱 Features & Highlights

### 1. Immersive Hybrid Web Experience (Main Page)
- **Zero Browser Clutter**: The top URL bar, navigation controls, and browser chrome have been completely removed. The page occupies the full screen for a clean, app-like feel.
- **Embedded & Iframe Fullscreen Video**: Full HTML5 video fullscreen support via `WebChromeClient.onShowCustomView` and `onHideCustomView`. Embedded players expand into a borderless, immersive container with system bars hidden.
- **Landscape Optimization & Collapsible Bottom Bar**: In horizontal/landscape phone orientation, the bottom navigation bar switches to an ultra-compact 48dp height without cluttering the screen. A down-arrow button allows hiding the bar completely, with a subtle floating "Show Menu" pill to restore it.
- **Pull-To-Refresh Gesture**: Users can pull down from the top of the screen to seamlessly reload the web page, integrated using Material 3 `PullToRefreshBox` and nested scrolling.
- **Resilient Back Swipe Navigation & Exit Protection**: System back gestures navigate backward through web history (including client-side SPA routing and iframes via continuous visited history tracking). When reaching the root webpage, a double-back exit confirmation ("Press back again to exit" within 2s) prevents accidental app closures.
- **Error & Offline Handling**: Friendly native retry card when no internet connectivity is detected.

### 2. Built-in AdGuard Private DNS & Multi-Layer AdBlocker
- **Invisible DNS-over-HTTPS (DoH)**:
  - Intercepts requests using `WebViewClient.shouldInterceptRequest`.
  - Queries AdGuard Private DNS (`https://dns.adguard-dns.com/resolve`) in real time.
  - Automatically kills any resource that resolves to `0.0.0.0` or returns `NXDOMAIN`.
- **Local In-Memory Cache**:
  - `ConcurrentHashMap` pre-seeded with known ad networks, trackers, popunder services, and telemetry domains for instant (0ms) lookup without network overhead.
- **Cosmetic DOM Filtering**:
  - Injects CSS rules that collapse and hide residual `.adsbygoogle`, `.widget.AdSense`, iframe banners, and video player overlay ads.
- **Anti-Clickjacking & Anti-Popup**:
  - Rejects unsolicited `onCreateWindow` popup tabs and disables rogue `window.open` scripts that hijack video playback taps.
- **Real-Time Counter**:
  - Displays the total number of blocked ads and trackers inside the Settings tab, with a one-tap reset option.

### 3. Stealth URL Protection ("dcsus7")
- **Hidden by Default**: Regular users cannot see or modify the Main Page URL. The configuration card is completely hidden.
- **Subtle Lock Logo**: A discrete, non-obvious lock icon in the Settings top bar acts as the secret trigger.
- **Access Code Verification**:
  - Tapping the lock icon prompts for the access code.
  - Entering **`dcsus7`** grants admin access and reveals the **Main Page URL** card.
  - Allows setting any custom URL (auto-formats with `https://`) or resetting to the default `https://dcsplayer.blogspot.com/`.

### 4. App Password Protection
- **Default State**: Empty (no password, immediate access).
- **Setup in Settings**:
  - Users can create a password with confirmation and visibility toggles.
  - Passwords are securely hashed with **SHA-256** and saved in the local Room database.
- **Lock Screen**:
  - When a password is active, opening the app prompts for the password on the dedicated `AppLockScreen`.
  - Settings allows changing the password, removing protection (requires verification of the current password), or locking the app on demand.

### 5. Engine Preferences
- **JavaScript Toggle**: Can be enabled or disabled based on preference.
- **Desktop View Mode**: Switches the WebView User-Agent to desktop Chrome for desktop site layouts.
- **Clear Cache & Cookies**: Fully wipes local storage, WebSQL, and cookie jars.

---

## 🏗️ Architecture & Component Logic

The app follows **MVVM (Model-View-ViewModel)** with the **Repository Pattern**:

```
app/
├── data/
│   ├── db/
│   │   ├── SettingEntity.kt     # Room entity for key-value settings & SHA-256 password
│   │   ├── SettingsDao.kt       # DAO interface for reading/writing settings
│   │   └── AppDatabase.kt       # Room database definition
│   └── repository/
│       └── LoopRepository.kt    # Data source abstraction, URL sanitization, hashing
├── ui/
│   ├── components/
│   │   └── LoopCircleLogo.kt    # Minimalist vector canvas logo with glow & orbital dot
│   ├── screens/
│   │   ├── MainPageScreen.kt    # Fullscreen WebView + PullToRefreshBox + AdBlocker
│   │   ├── SettingsScreen.kt    # Settings, stealth lock trigger, password & engine controls
│   │   └── AppLockScreen.kt     # Secure fullscreen lock screen prompt
│   ├── theme/
│   │   ├── Color.kt             # Dark slate and cyan/indigo theme palette
│   │   └── Theme.kt             # Material 3 dark/light color schemes
│   └── viewmodel/
│       └── LoopViewModel.kt     # UI state management, coroutine operations, WebView actions
└── util/
    └── AdBlocker.kt             # AdGuard DoH DNS engine, domain matcher & cosmetic CSS
```

---

## 🔐 Key Credentials & Codes

| Feature | Value / Code | Description |
| :--- | :--- | :--- |
| **Default URL** | `https://dcsplayer.blogspot.com/` | Default home address loaded on the Main Page |
| **Admin Access Code** | `dcsus7` | Secret code to unlock the Main Page URL configuration |
| **AdGuard DoH Host** | `https://dns.adguard-dns.com/resolve` | AdGuard Private DNS endpoint for real-time ad resolution |
| **App Password** | *Configured by user* (Default: Empty) | SHA-256 hashed app security lock |

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.2+
- **UI Toolkit**: Jetpack Compose (Material 3 with WindowInsets & edge-to-edge support)
- **Local Persistence**: Android Room Database (`androidx.room`) with KSP
- **Networking**: OkHttp 4 for DNS-over-HTTPS queries
- **Web Engine**: Android WebKit (`WebView`, `WebViewClient`, `WebChromeClient`)
- **Architecture**: Kotlin Coroutines & StateFlow for reactive UI state
