package com.example

import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.MainPageScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LoopViewModel
import kotlinx.coroutines.flow.collectLatest

enum class LoopTab(val title: String) {
    MAIN("Main Page"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LoopApp()
            }
        }
    }
}

@Composable
fun LoopApp(
    viewModel: LoopViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val fullscreenCustomView by viewModel.fullscreenCustomView.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(LoopTab.MAIN) }
    var isBottomBarVisible by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Control immersive system bars when showing video fullscreen
    DisposableEffect(fullscreenCustomView) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (fullscreenCustomView != null) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val win = activity?.window
            if (win != null) {
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Display snackbars from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (isAppLocked) {
        // App Lock Screen when password is required
        AppLockScreen(viewModel = viewModel)
    } else {
        // Root container: Keeps Scaffold and WebView mounted at all times
        Box(modifier = Modifier.fillMaxSize()) {
            // Back handler: returns to Main Page from Settings if back is pressed
            BackHandler(enabled = currentTab != LoopTab.MAIN && fullscreenCustomView == null) {
                currentTab = LoopTab.MAIN
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    // Navigation bar with ONLY the 2 items for perfect centering and alignment
                    AnimatedVisibility(
                        visible = isBottomBarVisible && fullscreenCustomView == null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .testTag("bottom_navigation_bar")
                                .then(if (isLandscape) Modifier.height(48.dp) else Modifier),
                            tonalElevation = androidx.compose.material3.NavigationBarDefaults.Elevation
                        ) {
                            // Menu 1: Main Page
                            NavigationBarItem(
                                selected = currentTab == LoopTab.MAIN,
                                onClick = { currentTab = LoopTab.MAIN },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == LoopTab.MAIN) Icons.Filled.Language else Icons.Outlined.Language,
                                        contentDescription = "Main Page",
                                        modifier = if (isLandscape) Modifier.size(20.dp) else Modifier.size(24.dp)
                                    )
                                },
                                label = if (!isLandscape) { { Text(LoopTab.MAIN.title) } } else null,
                                modifier = Modifier.testTag("nav_tab_main")
                            )

                            // Menu 2: Settings
                            NavigationBarItem(
                                selected = currentTab == LoopTab.SETTINGS,
                                onClick = { currentTab = LoopTab.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == LoopTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        modifier = if (isLandscape) Modifier.size(20.dp) else Modifier.size(24.dp)
                                    )
                                },
                                label = if (!isLandscape) { { Text(LoopTab.SETTINGS.title) } } else null,
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition",
                    modifier = Modifier.padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        LoopTab.MAIN -> {
                            MainPageScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentTab = LoopTab.SETTINGS }
                            )
                        }
                        LoopTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToMain = { currentTab = LoopTab.MAIN }
                            )
                        }
                    }
                }
            }

            // Landscape-only floating toggle to hide/show bottom bar without disturbing item alignment
            if (isLandscape && fullscreenCustomView == null) {
                Surface(
                    onClick = { isBottomBarVisible = !isBottomBarVisible },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = if (isBottomBarVisible) 56.dp else 12.dp)
                        .size(36.dp)
                        .testTag("landscape_toggle_bottom_bar")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBottomBarVisible) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                            contentDescription = if (isBottomBarVisible) "Hide bottom bar" else "Show bottom bar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Fullscreen Custom View Overlay: Placed on top of the hierarchy so WebView stays alive
            if (fullscreenCustomView != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("fullscreen_video_container")
                ) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)
                                val parent = fullscreenCustomView?.parent as? ViewGroup
                                parent?.removeView(fullscreenCustomView)
                                addView(
                                    fullscreenCustomView,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    BackHandler {
                        viewModel.hideCustomView()
                    }
                }
            }
        }
    }
}
