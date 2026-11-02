"use strict";
(() => {
  // O content script roda em ambiente isolado; não substitui APIs da página.
  let port;
  try { port = browser.runtime.connectNative("mangaflow"); }
  catch (_) { return; }

  let playing = false;
  let readerMode = false;
  let speed = 45; // pixels CSS por segundo
  let lastFrame = 0;
  let lastReport = 0;
  let progressSaved = -1;
  let resumeAttempted = false;
  const maximum = () => Math.max(0, document.documentElement.scrollHeight - innerHeight);
  const progress = () => maximum() ? Math.min(1, Math.max(0, scrollY / maximum())) : 0;
  const send = (obj) => { try { port.postMessage(obj); } catch (_) {} };

  const style = document.createElement("style");
  style.id = "mangaflow-reader-css";
  style.textContent = `
    ins.adsbygoogle, [id^="google_ads_"], [id^="div-gpt-ad"],
    iframe[src*="doubleclick.net"], iframe[src*="googlesyndication.com"],
    [data-ad-client], [data-ad-slot] { display: none !important; }
    html.mangaflow-reading, html.mangaflow-reading body { overflow-x: hidden !important; }
    html.mangaflow-reading img { max-width: 100% !important; height: auto; }
    html.mangaflow-reading header, html.mangaflow-reading nav,
    html.mangaflow-reading footer, html.mangaflow-reading aside,
    html.mangaflow-reading .advertisement, html.mangaflow-reading .adsbygoogle,
    html.mangaflow-reading .sidebar { display: none !important; }
  `;
  (document.head || document.documentElement).appendChild(style);

  function setReader(enabled) {
    readerMode = !!enabled;
    document.documentElement.classList.toggle("mangaflow-reading", readerMode);
    send({ type: "reader", enabled: readerMode });
  }
  function report(force = false) {
    const now = performance.now();
    if (!force && now - lastReport < 1200) return;
    lastReport = now;
    send({ type: "progress", url: location.href, title: document.title,
      progress: progress(), playing, reader: readerMode });
  }
  function frame(now) {
    if (!playing) { lastFrame = 0; return; }
    if (!lastFrame) lastFrame = now;
    const delta = Math.min(100, Math.max(0, now - lastFrame));
    lastFrame = now;
    // Pausa implícita se a aba não estiver visível.
    if (!document.hidden) window.scrollBy(0, speed * delta / 1000);
    if (maximum() > 0 && scrollY >= maximum() - 2) {
      playing = false;
      send({ type: "finished" });
      report(true);
      return;
    }
    report();
    requestAnimationFrame(frame);
  }
  function setPlaying(enabled) {
    if (playing === !!enabled) return;
    playing = !!enabled;
    lastFrame = 0;
    send({ type: "playing", playing });
    report(true);
    if (playing) requestAnimationFrame(frame);
  }
  function attemptRestore(target) {
    if (resumeAttempted || target <= 0 || target >= 1) return;
    resumeAttempted = true;
    const apply = () => window.scrollTo(0, maximum() * target);
    // Repetir porque imagens lazy-loading alteram a altura do documento.
    [400, 1200, 2400].forEach(delay => setTimeout(apply, delay));
  }
  port.onMessage.addListener(msg => {
    if (!msg || typeof msg !== "object") return;
    switch (msg.command) {
      case "play": setPlaying(true); break;
      case "pause": setPlaying(false); break;
      case "speed": speed = Math.max(10, Math.min(160, Number(msg.value) || 45)); break;
      case "reader": setReader(!!msg.enabled); break;
      case "restore": attemptRestore(Number(msg.progress) || 0); break;
      case "next-screen": window.scrollBy(0, innerHeight * 0.7); report(true); break;
      case "ping": send({type:"ready", url:location.href}); break;
    }
  });
  port.onDisconnect.addListener(() => { playing = false; });
  addEventListener("scroll", () => report(), {passive:true});
  addEventListener("pagehide", () => { setPlaying(false); report(true); });
  addEventListener("touchstart", () => { if (playing) setPlaying(false); }, {passive:true});
  document.addEventListener("visibilitychange", () => report(true));
  send({type:"ready", url:location.href, title:document.title});
  report(true);
})();
