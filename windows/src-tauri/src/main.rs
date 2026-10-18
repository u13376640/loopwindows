#![windows_subsystem = "windows"]

use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;

const DEFAULT_URL: &str = "https://dcsplayer.blogspot.com/";

fn find_firefox_executable(app_dir: &Path) -> Option<PathBuf> {
    // 1. Check local portable Firefox folder
    let local_ff = app_dir.join("Firefox").join("firefox.exe");
    if local_ff.exists() {
        return Some(local_ff);
    }

    let local_ff2 = app_dir.join("firefox").join("firefox.exe");
    if local_ff2.exists() {
        return Some(local_ff2);
    }

    // 2. Check standard Windows installation paths
    let standard_paths = [
        r"C:\Program Files\Mozilla Firefox\firefox.exe",
        r"C:\Program Files (x86)\Mozilla Firefox\firefox.exe",
    ];

    for path_str in &standard_paths {
        let p = PathBuf::from(path_str);
        if p.exists() {
            return Some(p);
        }
    }

    // 3. Check %LOCALAPPDATA%\Mozilla Firefox\firefox.exe
    if let Ok(local_app_data) = env::var("LOCALAPPDATA") {
        let p = PathBuf::from(local_app_data)
            .join("Mozilla Firefox")
            .join("firefox.exe");
        if p.exists() {
            return Some(p);
        }
    }

    // 4. Check PATH environment variable
    if let Ok(path_var) = env::var("PATH") {
        for dir in env::split_paths(&path_var) {
            let candidate = dir.join("firefox.exe");
            if candidate.exists() {
                return Some(candidate);
            }
        }
    }

    None
}

fn download_portable_firefox(app_dir: &Path) -> bool {
    let ps_script = format!(
        "$dest = '{}\\Firefox'; "
        + "Write-Host 'Downloading Firefox Portable...'; "
        + "$url = 'https://download.mozilla.org/?product=firefox-latest-ssl&os=win64&lang=en-US'; "
        + "$installer = '{}\\firefox-setup.exe'; "
        + "Invoke-WebRequest -Uri $url -OutFile $installer; "
        + "Start-Process -FilePath $installer -ArgumentList '/S /D=' + $dest -Wait; "
        + "Remove-Item -Path $installer -Force -ErrorAction SilentlyContinue;",
        app_dir.display(),
        app_dir.display()
    );

    let status = Command::new("powershell")
        .args(["-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", &ps_script])
        .status();

    match status {
        Ok(s) => s.success(),
        Err(_) => false,
    }
}

fn main() {
    let current_exe = env::current_exe().unwrap_or_else(|_| PathBuf::from("."));
    let app_dir = current_exe
        .parent()
        .unwrap_or_else(|| Path::new("."))
        .to_path_buf();

    // Check if profile directory exists next to exe or in project root
    let mut profile_dir = app_dir.join("profile");
    if !profile_dir.exists() {
        let parent_profile = app_dir.join("..").join("profile");
        if parent_profile.exists() {
            profile_dir = parent_profile;
        } else {
            let _ = fs::create_dir_all(&profile_dir);
        }
    }

    // Determine target URL
    let mut target_url = DEFAULT_URL.to_string();
    let url_file = profile_dir.join("active_url.txt");
    if url_file.exists() {
        if let Ok(custom) = fs::read_to_string(&url_file) {
            let trimmed = custom.trim();
            if !trimmed.is_empty() {
                target_url = trimmed.to_string();
            }
        }
    }

    // Find or download Firefox
    let ff_binary = match find_firefox_executable(&app_dir) {
        Some(bin) => bin,
        None => {
            // Attempt automatic download
            download_portable_firefox(&app_dir);
            find_firefox_executable(&app_dir).unwrap_or_else(|| {
                // If still not found, open Mozilla download page
                let _ = Command::new("cmd")
                    .args(["/c", "start", "https://www.mozilla.org/firefox/"])
                    .spawn();
                std::process::exit(1);
            })
        }
    };

    // Canonicalize profile path for Firefox
    let abs_profile = fs::canonicalize(&profile_dir).unwrap_or(profile_dir);

    // Launch Firefox in dedicated Loop profile with strict TRR Mode 3 and ECH
    let _ = Command::new(ff_binary)
        .arg("-no-remote")
        .arg("-profile")
        .arg(abs_profile)
        .arg("-new-window")
        .arg(target_url)
        .spawn();
}
