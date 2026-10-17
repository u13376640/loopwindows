use std::path::{Path, PathBuf};
use log::{info, warn};

pub struct AdBlockManager {
    extension_dir: Option<PathBuf>,
}

impl AdBlockManager {
    pub fn new() -> Self {
        let candidate_paths = [
            PathBuf::from("./extensions/ublock-origin"),
            PathBuf::from("../extensions/ublock-origin"),
            PathBuf::from("../../extensions/ublock-origin"),
            std::env::current_exe()
                .ok()
                .and_then(|p| p.parent().map(|d| d.join("extensions/ublock-origin")))
                .unwrap_or_else(|| PathBuf::from("extensions/ublock-origin")),
        ];

        let mut found_dir = None;
        for path in &candidate_paths {
            if path.exists() && path.join("manifest.json").exists() {
                found_dir = Some(path.canonicalize().unwrap_or_else(|_| path.clone()));
                break;
            }
        }

        if let Some(ref dir) = found_dir {
            info!("Found unpacked uBlock Origin extension at: {:?}", dir);
        } else {
            warn!("uBlock Origin extension directory not found; will fallback to built-in rules filter.");
        }

        Self {
            extension_dir: found_dir,
        }
    }

    pub fn get_extension_path(&self) -> Option<&Path> {
        self.extension_dir.as_deref()
    }

    /// Formats the command line switch for WebView2 initialization:
    /// e.g. `--load-extension="C:\Path\To\ublock-origin"`
    pub fn get_chromium_args(&self) -> Vec<String> {
        let mut args = vec![
            "--disable-features=PreloadMediaEngagementData,AutofillServerCommunication".to_string(),
        ];

        if let Some(ext_path) = self.get_extension_path() {
            args.push(format!("--load-extension={}", ext_path.display()));
        }

        args
    }
}
