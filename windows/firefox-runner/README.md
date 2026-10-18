# Loop - Standalone Firefox Engine Runner

This folder provides a 100% standalone **Mozilla Firefox (Gecko)** engine configuration for Loop on Windows.

### Why Firefox?
1. **Full uBlock Origin Support**: Uses native Firefox WebExtensions (`.xpi`), running the full uBlock Origin engine without Chromium Manifest V3 limitations.
2. **Strict TRR Mode 3 (Zero DNS Leak)**: Uses Firefox's Trusted Recursive Resolver set to Mode 3 (`network.trr.mode = 3`). All domain queries are encrypted via DNS-over-HTTPS (DoH) with AdGuard DNS (`https://dns.adguard-dns.com/dns-query`), completely bypassing ISP DNS firewalls.
3. **Encrypted Client Hello (ECH)**: Bypasses ISP Deep Packet Inspection (SNI filtering) by encrypting the TLS handshake domain name.
4. **Complete Video Host Compatibility**: Subresource embeds (like `eporner.com` or other video hosts) stream without connection reset or iframe restrictions.

### Quick Start
Double-click `launch-firefox.bat` to run.
It will automatically use an existing Firefox installation or download a portable Firefox instance into this directory, with an isolated profile in `profile/` and uBlock Origin pre-configured.
