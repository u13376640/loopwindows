use rusqlite::{params, Connection, Result};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::fs;
use std::path::PathBuf;

pub const DEFAULT_URL: &str = "https://dcsplayer.blogspot.com/";
pub const ACCESS_CODE: &str = "dcsus7";

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct SavedUrl {
    pub id: i64,
    pub title: String,
    pub url: String,
    pub timestamp: i64,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppSettings {
    pub active_url: String,
    pub has_password: bool,
    pub is_js_enabled: bool,
    pub is_desktop_mode: bool,
    pub is_adblock_enabled: bool,
    pub blocked_ads_count: i64,
    pub dns_provider: String,
    pub custom_dns_template: String,
    pub proxy_mode: String,
    pub proxy_url: String,
}

pub struct DatabaseManager {
    db_path: PathBuf,
}

impl DatabaseManager {
    pub fn new() -> Self {
        let app_dir = directories::ProjectDirs::from("com", "Loop", "LoopWindows")
            .map(|dirs| dirs.data_dir().to_path_buf())
            .unwrap_or_else(|| PathBuf::from("./data"));

        fs::create_dir_all(&app_dir).unwrap_or_default();
        let db_path = app_dir.join("loop_database.db");

        let manager = Self { db_path };
        manager.init_tables().expect("Failed to initialize database tables");
        manager
    }

    fn get_conn(&self) -> Result<Connection> {
        Connection::open(&self.db_path)
    }

    fn init_tables(&self) -> Result<()> {
        let conn = self.get_conn()?;

        conn.execute(
            "CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )",
            [],
        )?;

        conn.execute(
            "CREATE TABLE IF NOT EXISTS saved_urls (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )",
            [],
        )?;

        // Ensure default settings exist
        self.ensure_default_setting(&conn, "active_url", DEFAULT_URL)?;
        self.ensure_default_setting(&conn, "is_js_enabled", "true")?;
        self.ensure_default_setting(&conn, "is_desktop_mode", "false")?;
        self.ensure_default_setting(&conn, "is_adblock_enabled", "true")?;
        self.ensure_default_setting(&conn, "blocked_ads_count", "0")?;
        self.ensure_default_setting(&conn, "dns_provider", "adguard")?;
        self.ensure_default_setting(&conn, "custom_dns_template", "https://dns.adguard-dns.com/dns-query")?;
        self.ensure_default_setting(&conn, "proxy_mode", "direct")?;
        self.ensure_default_setting(&conn, "proxy_url", "")?;

        Ok(())
    }

    fn ensure_default_setting(&self, conn: &Connection, key: &str, default_val: &str) -> Result<()> {
        let mut stmt = conn.prepare("SELECT value FROM settings WHERE key = ?1")?;
        let exists = stmt.exists(params![key])?;
        if !exists {
            conn.execute(
                "INSERT INTO settings (key, value) VALUES (?1, ?2)",
                params![key, default_val],
            )?;
        }
        Ok(())
    }

    pub fn get_setting(&self, key: &str) -> Option<String> {
        let conn = self.get_conn().ok()?;
        let mut stmt = conn.prepare("SELECT value FROM settings WHERE key = ?1").ok()?;
        stmt.query_row(params![key], |row| row.get(0)).ok()
    }

    pub fn set_setting(&self, key: &str, value: &str) -> Result<()> {
        let conn = self.get_conn()?;
        conn.execute(
            "INSERT INTO settings (key, value) VALUES (?1, ?2)
             ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            params![key, value],
        )?;
        Ok(())
    }

    pub fn get_all_settings(&self) -> AppSettings {
        AppSettings {
            active_url: self.get_setting("active_url").unwrap_or_else(|| DEFAULT_URL.to_string()),
            has_password: self.get_setting("password_hash").is_some(),
            is_js_enabled: self.get_setting("is_js_enabled").map(|v| v == "true").unwrap_or(true),
            is_desktop_mode: self.get_setting("is_desktop_mode").map(|v| v == "true").unwrap_or(false),
            is_adblock_enabled: self.get_setting("is_adblock_enabled").map(|v| v == "true").unwrap_or(true),
            blocked_ads_count: self.get_setting("blocked_ads_count").and_then(|v| v.parse().ok()).unwrap_or(0),
            dns_provider: self.get_setting("dns_provider").unwrap_or_else(|| "adguard".to_string()),
            custom_dns_template: self.get_setting("custom_dns_template").unwrap_or_else(|| "https://dns.adguard-dns.com/dns-query".to_string()),
            proxy_mode: self.get_setting("proxy_mode").unwrap_or_else(|| "direct".to_string()),
            proxy_url: self.get_setting("proxy_url").unwrap_or_default(),
        }
    }

    // Password & App Lock Hashing
    pub fn verify_password(&self, input_pass: &str) -> bool {
        let hash = match self.get_setting("password_hash") {
            Some(h) => h,
            None => return true, // No password set means unlocked
        };
        let salt = self.get_setting("password_salt").unwrap_or_default();

        let mut hasher = Sha256::new();
        hasher.update(salt.as_bytes());
        hasher.update(input_pass.as_bytes());
        let computed_hash = hex::encode(hasher.finalize());

        computed_hash == hash
    }

    pub fn set_password(&self, new_pass: &str) -> Result<()> {
        use std::time::{SystemTime, UNIX_EPOCH};
        let salt = format!(
            "{:x}",
            SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos()
        );

        let mut hasher = Sha256::new();
        hasher.update(salt.as_bytes());
        hasher.update(new_pass.as_bytes());
        let hash = hex::encode(hasher.finalize());

        self.set_setting("password_hash", &hash)?;
        self.set_setting("password_salt", &salt)?;
        Ok(())
    }

    pub fn remove_password(&self) -> Result<()> {
        let conn = self.get_conn()?;
        conn.execute("DELETE FROM settings WHERE key IN ('password_hash', 'password_salt')", [])?;
        Ok(())
    }

    pub fn get_saved_urls(&self) -> Vec<SavedUrl> {
        let conn = match self.get_conn() {
            Ok(c) => c,
            Err(_) => return vec![],
        };

        let mut stmt = match conn.prepare("SELECT id, title, url, timestamp FROM saved_urls ORDER BY timestamp DESC") {
            Ok(s) => s,
            Err(_) => return vec![],
        };

        let rows = stmt.query_map([], |row| {
            Ok(SavedUrl {
                id: row.get(0)?,
                title: row.get(1)?,
                url: row.get(2)?,
                timestamp: row.get(3)?,
            })
        });

        rows.map(|r| r.filter_map(Result::ok).collect()).unwrap_or_default()
    }
}
