"use strict";
// Lista pequena, intencionalmente conservadora, para reduzir quebras em leitores.
// Não constitui um substituto completo das listas de filtros do uBlock Origin.
const blockedHosts = new Set([
  "doubleclick.net", "googleads.g.doubleclick.net", "googlesyndication.com",
  "googleadservices.com", "adservice.google.com", "pagead2.googlesyndication.com",
  "securepubads.g.doubleclick.net", "amazon-adsystem.com", "adsrvr.org",
  "taboola.com", "outbrain.com", "propellerads.com", "popads.net",
  "popcash.net", "exoclick.com", "trafficjunky.net", "adsterra.com",
  "clickadu.com", "hilltopads.net", "onclicka.com", "mgid.com",
  "criteo.com", "criteo.net", "adnxs.com", "pubmatic.com",
  "monetag.com", "highperformanceformat.com", "highperformancedisplayformat.com",
  "ad-maven.com", "richads.com", "pushnative.com",
  "rubiconproject.com", "openx.net", "smartadserver.com",
  "scorecardresearch.com", "quantserve.com"
]);
function isBlocked(hostname) {
  const host = hostname.toLowerCase().replace(/\.$/, "");
  for (const item of blockedHosts) {
    if (host === item || host.endsWith("." + item)) return true;
  }
  return false;
}
browser.webRequest.onBeforeRequest.addListener(
  details => {
    try { if (isBlocked(new URL(details.url).hostname)) return { cancel: true }; }
    catch (_) { /* URI não HTTP: ignorar */ }
    return {};
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);
