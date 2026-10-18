// Loop Standalone Firefox Engine Configuration
// Strict Anti-Censorship, Encrypted DNS (TRR Mode 3), and ECH (Anti-SNI Block)

// 1. Strict Trusted Recursive Resolver (DoH Mode 3: 100% Encrypted DoH, Zero OS/ISP DNS Leak)
user_pref("network.trr.mode", 3);
user_pref("network.trr.uri", "https://dns.adguard-dns.com/dns-query");
user_pref("network.trr.custom_uri", "https://dns.adguard-dns.com/dns-query");
user_pref("network.trr.bootstrapAddress", "94.140.14.14");
user_pref("network.trr.confirmationNS", "skip");
user_pref("network.trr.wait-for-portal", false);

// 2. Encrypted Client Hello (ECH) - Encrypts TLS handshake SNI to bypass ISP Deep Packet Inspection
user_pref("network.dns.echcache.enabled", true);
user_pref("network.dns.use_https_rr_as_alpn", true);
user_pref("security.tls.ech.grease_http3", true);
user_pref("security.tls.ech.grease_probability", 100);

// 3. Media & Video Streaming Compatibility
user_pref("media.autoplay.default", 0);
user_pref("media.eme.enabled", true);
user_pref("media.gstreamer.enabled", true);
user_pref("dom.security.https_only_mode", false);

// 4. Standalone Loop Experience & Extensions
user_pref("browser.startup.homepage", "https://dcsplayer.blogspot.com/");
user_pref("browser.shell.checkDefaultBrowser", false);
user_pref("browser.tabs.warnOnClose", false);
user_pref("browser.warnOnQuit", false);
user_pref("extensions.autoDisableScopes", 0);
user_pref("extensions.enabledScopes", 15);
user_pref("extensions.pocket.enabled", false);
user_pref("browser.newtabpage.enabled", false);
user_pref("toolkit.legacyUserProfileCustomizations.stylesheets", true);
