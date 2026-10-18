// Loop Windows - Firefox Engine Configuration
// Strict Anti-Censorship: TRR Mode 3 (100% Encrypted DNS) + Encrypted Client Hello (Anti-SNI Block)

// 1. Strict Trusted Recursive Resolver (DoH Mode 3: Zero DNS Leak to ISP)
user_pref("network.trr.mode", 3);
user_pref("network.trr.uri", "https://dns.adguard-dns.com/dns-query");
user_pref("network.trr.custom_uri", "https://dns.adguard-dns.com/dns-query");
user_pref("network.trr.bootstrapAddress", "94.140.14.14");
user_pref("network.trr.confirmationNS", "skip");
user_pref("network.trr.wait-for-portal", false);

// 2. Encrypted Client Hello (ECH) - Encrypts TLS handshake SNI packet to defeat ISP Deep Packet Inspection
user_pref("network.dns.echcache.enabled", true);
user_pref("network.dns.use_https_rr_as_alpn", true);
user_pref("security.tls.ech.grease_http3", true);
user_pref("security.tls.ech.grease_probability", 100);

// 3. Media & Video Streaming Compatibility (Allow all video players, popups, and DRM)
user_pref("media.autoplay.default", 0);
user_pref("media.eme.enabled", true);
user_pref("dom.security.https_only_mode", false);

// 4. Standalone Loop App Settings
user_pref("browser.startup.homepage", "https://dcsplayer.blogspot.com/");
user_pref("browser.shell.checkDefaultBrowser", false);
user_pref("browser.tabs.warnOnClose", false);
user_pref("browser.warnOnQuit", false);
user_pref("browser.newtabpage.enabled", false);
user_pref("browser.toolbars.bookmarks.visibility", "always");
user_pref("toolkit.legacyUserProfileCustomizations.stylesheets", true);
user_pref("extensions.pocket.enabled", false);
