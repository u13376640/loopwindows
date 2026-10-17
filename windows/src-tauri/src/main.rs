#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod adblock;
mod db;

use adblock::AdBlockManager;
use db::{AppSettings, DatabaseManager, ACCESS_CODE, DEFAULT_URL};
use std::sync::Mutex;
use tauri::{State, Window};

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

#[tauri::command]
fn window_start_dragging(window: Window) {
    let _ = window.start_dragging();
}

#[tauri::command]
fn set_dns_config(provider: String, template: String, state: State<AppState>) -> Result<(), String> {
    let db = state.db.lock().unwrap();
    db.set_setting("dns_provider", &provider).map_err(|e| e.to_string())?;
    db.set_setting("custom_dns_template", &template).map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
fn set_proxy_config(mode: String, host: String, port: String, bypass: String, state: State<AppState>) -> Result<(), String> {
    let db = state.db.lock().unwrap();
    db.set_setting("proxy_mode", &mode).map_err(|e| e.to_string())?;
    db.set_setting("proxy_host", &host).map_err(|e| e.to_string())?;
    db.set_setting("proxy_port", &port).map_err(|e| e.to_string())?;
    db.set_setting("proxy_bypass", &bypass).map_err(|e| e.to_string())?;
    Ok(())
}

fn main() {
    let adblock = AdBlockManager::new();
    let mut chromium_args = adblock.get_chromium_args();
    let db = DatabaseManager::new();

    // Disable frame/origin restrictions so any web player or site loads without X-Frame-Options blocks
    chromium_args.push("--disable-web-security".to_string());
    chromium_args.push("--allow-running-insecure-content".to_string());
    chromium_args.push("--disable-site-isolation-trials".to_string());
    chromium_args.push("--disable-features=IsolateOrigins,site-per-process".to_string());

    // Configure AdGuard / Custom DNS-over-HTTPS (DoH) to bypass country-level website blocks
    let dns_provider = db.get_setting("dns_provider").unwrap_or_else(|| "adguard".to_string());
    let custom_dns = db.get_setting("custom_dns_template").unwrap_or_else(|| "https://dns.adguard-dns.com/dns-query".to_string());

    let doh_template = match dns_provider.as_str() {
        "adguard" => Some("https://dns.adguard-dns.com/dns-query"),
        "cloudflare" => Some("https://cloudflare-dns.com/dns-query"),
        "google" => Some("https://dns.google/dns-query"),
        "custom" => if !custom_dns.is_empty() { Some(custom_dns.as_str()) } else { Some("https://dns.adguard-dns.com/dns-query") },
        "system" => None,
        _ => Some("https://dns.adguard-dns.com/dns-query"),
    };

    if let Some(template) = doh_template {
        chromium_args.push("--enable-features=DnsOverHttps".to_string());
        chromium_args.push(format!("--dns-over-https-template={}", template));
    }

    // Configure SOCKS5 / HTTP Proxy Tunnel to bypass regional ISP/SNI firewalls
    let proxy_mode = db.get_setting("proxy_mode").unwrap_or_else(|| "direct".to_string());
    let proxy_host = db.get_setting("proxy_host").unwrap_or_else(|| "127.0.0.1".to_string());
    let proxy_port = db.get_setting("proxy_port").unwrap_or_else(|| "1080".to_string());
    let proxy_bypass = db.get_setting("proxy_bypass").unwrap_or_else(|| "<local>;127.0.0.1;localhost".to_string());

    if proxy_mode == "socks5" {
        chromium_args.push(format!("--proxy-server=socks5://{}:{}", proxy_host, proxy_port));
        chromium_args.push(format!("--proxy-bypass-list={}", proxy_bypass));
    } else if proxy_mode == "http" {
        chromium_args.push(format!("--proxy-server=http://{}:{}", proxy_host, proxy_port));
        chromium_args.push(format!("--proxy-bypass-list={}", proxy_bypass));
    }

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
            window_start_dragging,
            set_dns_config,
            set_proxy_config,
        ])
        .setup(|_app| {
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running Loop Windows application");
}
