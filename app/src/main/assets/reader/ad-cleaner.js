"use strict";
(() => {
  // Cleaner runs separately from the reader controller: no effect on scroll/progress.
  const neoToons = /(^|\.)neotoons\.com\.br$/i.test(location.hostname);
  const hiddenAttribute = "data-mangaflow-ad-hidden";
  const knownAdHosts = /(?:^|\.)(?:doubleclick\.net|googlesyndication\.com|googleadservices\.com|popads\.net|popcash\.net|adsterra\.com|monetag\.com|exoclick\.com|propellerads\.com|onclicka\.com|highperformanceformat\.com|highperformancedisplayformat\.com)$/i;

  const style = document.createElement("style");
  style.id = "mangaflow-ad-cleaner";
  style.textContent = [
    'ins.adsbygoogle, [data-ad-client], [data-ad-slot],',
    '[id^="div-gpt-ad"], [id^="google_ads_"],',
    'iframe[src*="doubleclick.net"], iframe[src*="googlesyndication.com"],',
    'iframe[src*="adsterra.com"], iframe[src*="popads.net"],',
    '[' + hiddenAttribute + '="1"] { display:none !important; visibility:hidden !important; }'
  ].join("\n");

  const protectedTags = new Set(["HTML", "BODY", "MAIN", "ARTICLE", "HEADER", "NAV"]);
  const strongAdClass = /(?:^|[\s_-])(?:adsbygoogle|adslot|adunit|adserver|advertisement|advertising|sponsored-ad|popup-ad|sticky-ad)(?:$|[\s_-])/i;
  const promoText = /(?:teste\s+gr[aá]tis|testar\s+gr[aá]tis|experimente\s+gr[aá]tis)/i;
  const mediaText = /(?:iptv|canais.{0,35}filmes|filmes.{0,35}s[eé]ries)/i;
  const brandPromo = /(?:abertura\s+oficial\s+neotoons|neotoons.{0,25}(?:promo|publicidade|an[uú]ncio))/i;
  const candidatesSelector = "div,section,aside,a,button,iframe,[role='dialog'],[role='alert'],ins";
  let scheduled = false;
  const pending = new Set();

  function injectStyle() {
    if (style.isConnected || !document.documentElement) return;
    (document.head || document.documentElement).appendChild(style);
  }

  function hide(element) {
    if (!element || protectedTags.has(element.tagName)) return false;
    if (element.getAttribute(hiddenAttribute) === "1") return false;
    element.setAttribute(hiddenAttribute, "1");
    return true;
  }

  function adLink(element) {
    const href = element.getAttribute("href") || element.getAttribute("src") || "";
    if (!href) return false;
    try {
      const parsed = new URL(href, location.href);
      return knownAdHosts.test(parsed.hostname);
    } catch (_) {
      return false;
    }
  }

  function isReadingImage(element) {
    if (!element || !element.querySelectorAll) return false;
    // Do not hide chapter image containers: a promo detector must not remove panels.
    const images = element.querySelectorAll("img, picture, canvas");
    if (images.length > 2) return true;
    for (const image of images) {
      if (image.closest("[class*='chapter'], [class*='reader'], [id*='chapter']")) {
        const meta = (image.getAttribute("alt") || "") + " " +
                     (image.getAttribute("src") || "");
        if (!brandPromo.test(meta)) return true;
      }
    }
    return false;
  }

  function compactText(element) {
    const text = (element.textContent || "").trim();
    return text.length > 440 ? "" : text.replace(/\s+/g, " ");
  }

  function suspiciousPromo(element) {
    const text = compactText(element);
    return Boolean(text && promoText.test(text) && mediaText.test(text));
  }

  function floatingContainer(element) {
    // Prefer the exact visual overlay instead of removing any chapter/navigation root.
    let current = element;
    for (let depth = 0; current && depth < 7; depth++, current = current.parentElement) {
      if (protectedTags.has(current.tagName) || isReadingImage(current)) break;
      const rect = current.getBoundingClientRect();
      if (!rect || rect.width < 65 || rect.height < 20 ||
          rect.height > innerHeight * 0.66) continue;
      const css = getComputedStyle(current);
      const zIndex = Number(css.zIndex);
      const floating = css.position === "fixed" || css.position === "sticky" ||
        (css.position === "absolute" && Number.isFinite(zIndex) && zIndex >= 15);
      if (floating) return current;
    }
    return null;
  }

  function check(element) {
    if (!element || element.nodeType !== 1 ||
        element.hasAttribute(hiddenAttribute) ||
        protectedTags.has(element.tagName)) return;
    // Only high-confidence class names: never hide all banners, modals or sticky elements.
    const name = String(element.id || "") + " " +
      (typeof element.className === "string" ? element.className : "");
    if (strongAdClass.test(name) && !isReadingImage(element)) {
      hide(element);
      return;
    }
    if ((element.tagName === "IFRAME" || element.tagName === "A") &&
        adLink(element) && !isReadingImage(element)) {
      if (element.tagName === "IFRAME") hide(element);
      else hide(floatingContainer(element) || element);
      return;
    }
    if (neoToons) {
      const meta = (element.getAttribute("aria-label") || "") + " " +
        (element.getAttribute("title") || "") + " " +
        (element.getAttribute("alt") || "");
      if (brandPromo.test(meta) && !isReadingImage(element)) {
        hide(element);
        return;
      }
      // The photographed "TESTE GRÁTIS / IPTV" bottom ad is injected as a
      // dynamic floating group. Hide its overlay, not NeoToons chapter controls.
      if (suspiciousPromo(element) && !isReadingImage(element)) {
        const floater = floatingContainer(element);
        if (floater) hide(floater);
      }
    }
  }

  function scan(root) {
    if (!root || root.nodeType !== 1) return;
    check(root);
    if (!root.querySelectorAll) return;
    let count = 0;
    for (const element of root.querySelectorAll(candidatesSelector)) {
      if (++count > 1200) break;
      check(element);
    }
  }

  function schedule(root) {
    if (root && root.nodeType === 1) pending.add(root);
    if (scheduled) return;
    scheduled = true;
    setTimeout(() => {
      scheduled = false;
      injectStyle();
      const roots = [...pending];
      pending.clear();
      roots.forEach(scan);
    }, 250);
  }

  function initialize() {
    injectStyle();
    const root = document.documentElement;
    if (!root) { setTimeout(initialize, 30); return; }
    schedule(document.body || root);
    const observer = new MutationObserver(records => {
      for (const record of records) {
        for (const node of record.addedNodes) {
          if (node.nodeType === 1) schedule(node);
          else if (node.nodeType === 3 && node.parentElement) schedule(node.parentElement);
        }
      }
    });
    observer.observe(root, {subtree: true, childList: true});
    addEventListener("pageshow", () => schedule(document.body || root), {passive: true});
    // MutationObserver avoids costly full-page scans while the reader is scrolling.
  }
  initialize();
})();