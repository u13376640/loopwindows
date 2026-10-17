#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod adblock;
mod db;

use adblock::AdBlockManager;
use db::{AppSettings, DatabaseManager, ACCESS_CODE, DEFAULT_URL};
use std::sync::Mutex;
use tauri::{Manager, State, Window};

struct AppState {
    db: Mutex<DatabaseManager>,
    adblock: AdBlockManager,
}

#[tauri::command]
fn get_app_settings(state: State<AppState>) -> AppSettings {
    let db = state.db.lock().unwrap();
    db.get_all_settings()
}

#[tauri::command]
fn verify_access_code(code: String) -> bool {
    code.trim() == ACCESS_CODE
}

#[tauri::command]
fn set_active_url(url: String, access_code: String, state: State<AppState>) -> Result<String, String> {
    if access_code.trim() != ACCESS_CODE {
        return Err("Unauthorized: Invalid access code".to_string());
    }
    let db = state.db.lock().unwrap();
    let cleaned_url = if !url.starts_with("http://") && !url.starts_with("https://") {
        format!("https://{}", url)
    } else {
        url
    };
    db.set_setting("active_url", &cleaned_url)
        .map_err(|e| e.to_string())?;
    Ok(cleaned_url)
}

#[tauri::command]
fn reset_default_url(access_code: String, state: State<AppState>) -> Result<String, String> {
    if access_code.trim() != ACCESS_CODE {
        return Err("Unauthorized: Invalid access code".to_string());
    }
    let db = state.db.lock().unwrap();
    db.set_setting("active_url", DEFAULT_URL)
        .map_err(|e| e.to_string())?;
    Ok(DEFAULT_URL.to_string())
}

#[tauri::command]
fn verify_password(password: String, state: State<AppState>) -> bool {
    let db = state.db.lock().unwrap();
    db.verify_password(&password)
}

#[tauri::command]
fn set_app_password(password: String, state: State<AppState>) -> Result<(), String> {
    let db = state.db.lock().unwrap();
    db.set_password(&password).map_err(|e| e.to_string())
}

#[tauri::command]
fn remove_app_password(current_password: String, state: State<AppState>) -> Result<(), String> {
    let db = state.db.lock().unwrap();
    if !db.verify_password(&current_password) {
        return Err("Current password incorrect".to_string());
    }
    db.remove_password().map_err(|e| e.to_string())
}

#[tauri::command]
fn toggle_setting(key: String, value: bool, state: State<AppState>) -> Result<(), String> {
    let db = state.db.lock().unwrap();
    db.set_setting(&key, if value { "true" } else { "false" })
        .map_err(|e| e.to_string())
}

#[tauri::command]
fn window_minimize(window: Window) {
    let _ = window.minimize();
}

#[tauri::command]
fn window_maximize_toggle(window: Window) {
    if let Ok(is_max) = window.is_maximized() {
        if is_max {
            let _ = window.unmaximize();
        } else {
            let _ = window.maximize();
        }
    }
}

#[tauri::command]
fn window_close(window: Window) {
    let _ = window.close();
}

fn main() {
    let adblock = AdBlockManager::new();
    let chromium_args = adblock.get_chromium_args();
    let db = DatabaseManager::new();

    // Set Chromium command-line switches for WebView2 before creation
    let args_str = chromium_args.join(" ");
    std::env::set_var("WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS", &args_str);

    tauri::Builder::default()
        .manage(AppState {
            db: Mutex::new(db),
            adblock,
        })
        .invoke_handler(tauri::generate_handler![
            get_app_settings,
            verify_access_code,
            set_active_url,
            reset_default_url,
            verify_password,
            set_app_password,
            remove_app_password,
            toggle_setting,
            window_minimize,
            window_maximize_toggle,
            window_close,
        ])
        .setup(|_app| {
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running Loop Windows application");
}
