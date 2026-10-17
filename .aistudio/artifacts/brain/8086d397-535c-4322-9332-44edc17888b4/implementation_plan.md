# Resilient Web Navigation & Double-Back Exit Protection

Reliable back swipe navigation within the hybrid web portal to navigate previous webpages before closing the application, with double-back exit confirmation at the root webpage.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following behavior was confirmed during interactive clarification:
> - **Root Webpage Back Action**: When the user reaches the root webpage (no more previous history), swiping back will show a quick confirmation toast ("Press back again to exit") instead of immediately quitting. Swiping back again within 2 seconds exits the app.
> - **Comprehensive History Interception**: All web history changes, including single-page app (SPA) client-side routes, hash navigation, and iframe updates will be continuously synchronized to ensure the back gesture always navigates backward through web pages.

---

### 1. Overview & Core Concept

- **What It Does**: Fixes an issue where Android's gesture or hardware back swipe occasionally closed the app instead of navigating back to the previous webpage. Keeps back-press interception active on the Main Page, queries real-time WebKit history, and protects against accidental exits at the root URL.
- **Target Audience**: Users browsing `https://dcsplayer.blogspot.com/` and embedded video portals who expect standard browser-like back navigation without unexpected app termination.
- **Key Value**: Fluid, dependable browsing experience where back swipes always take the user to the previous article, video, or webpage.

---

### 2. User Experience & Flow

1. **Browsing Deep Webpages**:
   - The user browses from the homepage into multiple articles, categories, or video pages.
   - When swiping back from either the left or right edge of the screen, the app smoothly navigates to the previous webpage (`webView.goBack()`).
2. **Dynamic & In-Page Navigation**:
   - Webpages with client-side history changes (`pushState`, `replaceState`, hash anchor jumps) report history updates via `doUpdateVisitedHistory`.
   - The back gesture navigates back through these states accurately.
3. **Reaching the Root / First Page**:
   - When the user is on the first webpage and can no longer navigate backward:
   - First back swipe triggers an unobtrusive native toast: *"Press back again to exit"*.
   - If the user swipes back again within 2 seconds, the application closes.
   - If the user continues browsing or waits longer than 2 seconds, the exit timer resets safely.

---

### 3. Key Technical Decisions & Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       System Back Event                     │
│                (Gesture Swipe / Hardware Key)               │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 MainPageScreen BackHandler                  │
│       (Always active on Main Page when video not fullscreen) │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
    [webView.canGoBack() == true]    [webView.canGoBack() == false]
               │                               │
               ▼                               ▼
┌──────────────────────────────┐ ┌─────────────────────────────┐
│      webView.goBack()        │ │ Double-Back Exit Guard      │
│  Navigates to previous page  │ │ - If < 2000ms: Finish App   │
│  Updates navigation state    │ │ - Else: Show Toast & Record │
└──────────────────────────────┘ └─────────────────────────────┘
```

- **Decision 1: Continuous History Tracking (`doUpdateVisitedHistory`)**:
  - *Chosen Approach*: Implement `doUpdateVisitedHistory` in `WebViewClient` in addition to `onPageStarted` and `onPageFinished`.
  - *Why*: Modern websites update URL history via JavaScript without triggering full page reloads. `doUpdateVisitedHistory` guarantees real-time accuracy of `webView.canGoBack()`.
- **Decision 2: Direct Real-Time WebView Check in BackHandler**:
  - *Chosen Approach*: Query `webViewRef?.canGoBack()` directly inside the back press handler rather than relying solely on asynchronous Compose state.
  - *Why*: Eliminates any race condition or state desynchronization between WebKit's internal back stack and Compose's recomposition cycle.
- **Decision 3: Double-Back Exit Protection**:
  - *Chosen Approach*: Check `System.currentTimeMillis() - lastBackPressTime < 2000`.
  - *Why*: Directly aligns with user confirmation, preventing accidental exits when swiping back or scrolling near screen edges.

---

### 4. Implementation Steps

1. **Update `MainPageScreen.kt`**:
   - Implement `doUpdateVisitedHistory` inside `WebViewClient` to synchronize `viewModel.updateNavigationState(view.canGoBack())`.
   - Keep `BackHandler` enabled on the Main Page.
   - Inside `BackHandler`:
     - If `webViewRef?.canGoBack() == true`: call `webViewRef?.goBack()`.
     - Else: evaluate the double-tap threshold (`2000ms`); show Toast `"Press back again to exit"` on the first swipe, and finish the activity if repeated within the window.
2. **Verify Coordination with Fullscreen Video & Settings**:
   - Ensure fullscreen video exit takes priority when a video is expanded (`viewModel.hideCustomView()`).
   - Ensure Settings back navigation returns to Main Page (`currentTab = LoopTab.MAIN`).
3. **Verification**:
   - Run `compile_applet` to verify compilation.
   - Run JVM unit tests to ensure no regressions.
