// uBlock Origin Lite background coordinator for Loop Windows WebView2
console.log("[uBlock Origin Core] Shield active on Loop Windows WebView2 runtime.");

chrome.runtime.onInstalled.addListener(() => {
  console.log("[uBlock Origin Core] Declarative rulesets successfully registered.");
});
