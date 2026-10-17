package com.example.ui.viewmodel

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.repository.LoopRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WebViewAction {
    object GoBack : WebViewAction
    object Reload : WebViewAction
    data class LoadUrl(val url: String) : WebViewAction
    object ClearCache : WebViewAction
}

class LoopViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = LoopRepository(database)

    val activeUrl: StateFlow<String> = repository.activeUrl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoopRepository.DEFAULT_URL
        )

    val isJsEnabled: StateFlow<Boolean> = repository.isJsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val isDesktopMode: StateFlow<Boolean> = repository.isDesktopMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasPassword: StateFlow<Boolean> = repository.hasPassword
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isAdBlockEnabled: StateFlow<Boolean> = repository.isAdBlockEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val blockedAdsCount: StateFlow<Int> = repository.blockedAdsCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Dynamic WebView Runtime State
    private val _currentLoadedUrl = MutableStateFlow(LoopRepository.DEFAULT_URL)
    val currentLoadedUrl: StateFlow<String> = _currentLoadedUrl.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _webViewActions = MutableSharedFlow<WebViewAction>(extraBufferCapacity = 1)
    val webViewActions: SharedFlow<WebViewAction> = _webViewActions.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Fullscreen video Custom View state
    private val _fullscreenCustomView = MutableStateFlow<View?>(null)
    val fullscreenCustomView: StateFlow<View?> = _fullscreenCustomView.asStateFlow()

    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = callback
        _fullscreenCustomView.value = view
    }

    fun hideCustomView() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        _fullscreenCustomView.value = null
    }

    init {
        viewModelScope.launch {
            // Check if password exists on startup; if so, lock app
            val passwordSet = repository.isPasswordSet()
            _isAppLocked.value = passwordSet
        }
    }

    // AdBlocker Operations
    fun toggleAdBlock(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAdBlockEnabled(enabled)
            _webViewActions.tryEmit(WebViewAction.Reload)
            _snackbarMessage.tryEmit("AdBlocker ${if (enabled) "enabled" else "disabled"}")
        }
    }

    fun onAdBlocked() {
        viewModelScope.launch {
            repository.incrementBlockedAdsCount(1)
        }
    }

    fun resetBlockedAdsCount() {
        viewModelScope.launch {
            repository.resetBlockedAdsCount()
            _snackbarMessage.tryEmit("Ad blocker stats reset")
        }
    }

    // App Password Operations
    fun unlockApp(password: String, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        viewModelScope.launch {
            val valid = repository.verifyPassword(password)
            if (valid) {
                _isAppLocked.value = false
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun lockApp() {
        viewModelScope.launch {
            if (repository.isPasswordSet()) {
                _isAppLocked.value = true
            }
        }
    }

    fun setAppPassword(password: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.setPassword(password)
            _snackbarMessage.tryEmit("App password created successfully")
            onComplete()
        }
    }

    fun changeAppPassword(
        currentPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            val valid = repository.verifyPassword(currentPass)
            if (valid) {
                repository.setPassword(newPass)
                _snackbarMessage.tryEmit("App password updated")
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun removeAppPassword(
        currentPass: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            val valid = repository.verifyPassword(currentPass)
            if (valid) {
                repository.removePassword()
                _snackbarMessage.tryEmit("App password removed")
                onSuccess()
            } else {
                onError()
            }
        }
    }

    // URL & Engine Operations
    fun setActiveUrl(rawUrl: String, showFeedback: Boolean = true) {
        viewModelScope.launch {
            val sanitized = repository.setActiveUrl(rawUrl)
            _hasError.value = false
            _errorMessage.value = null
            _webViewActions.tryEmit(WebViewAction.LoadUrl(sanitized))
            if (showFeedback) {
                _snackbarMessage.tryEmit("Main page URL updated: $sanitized")
            }
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val defaultUrl = repository.resetToDefaultUrl()
            _hasError.value = false
            _errorMessage.value = null
            _webViewActions.tryEmit(WebViewAction.LoadUrl(defaultUrl))
            _snackbarMessage.tryEmit("Reset to default (dcsplayer)")
        }
    }

    fun toggleJs(enabled: Boolean) {
        viewModelScope.launch {
            repository.setJsEnabled(enabled)
            _snackbarMessage.tryEmit("JavaScript ${if (enabled) "enabled" else "disabled"}")
        }
    }

    fun toggleDesktopMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDesktopMode(enabled)
            _webViewActions.tryEmit(WebViewAction.Reload)
            _snackbarMessage.tryEmit("Desktop mode ${if (enabled) "enabled" else "disabled"}")
        }
    }

    fun reloadCurrentPage() {
        _hasError.value = false
        _errorMessage.value = null
        _webViewActions.tryEmit(WebViewAction.Reload)
    }

    fun goBack() {
        _webViewActions.tryEmit(WebViewAction.GoBack)
    }

    fun clearCacheAndCookies() {
        viewModelScope.launch {
            try {
                WebStorage.getInstance().deleteAllData()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                _webViewActions.tryEmit(WebViewAction.ClearCache)
                _snackbarMessage.tryEmit("Cache and cookies cleared")
            } catch (e: Exception) {
                _snackbarMessage.tryEmit("Error clearing cache: ${e.localizedMessage}")
            }
        }
    }

    // Callbacks from WebView
    fun onPageStarted(url: String) {
        _currentLoadedUrl.value = url
        _isLoading.value = true
        _hasError.value = false
        _errorMessage.value = null
    }

    fun onPageFinished(url: String) {
        _currentLoadedUrl.value = url
        _isLoading.value = false
        _loadingProgress.value = 100
    }

    fun onProgressChanged(newProgress: Int) {
        _loadingProgress.value = newProgress
        _isLoading.value = newProgress < 100
    }

    fun updateNavigationState(canBack: Boolean) {
        _canGoBack.value = canBack
    }

    fun onErrorReceived(description: String) {
        _hasError.value = true
        _errorMessage.value = description
        _isLoading.value = false
    }
}
