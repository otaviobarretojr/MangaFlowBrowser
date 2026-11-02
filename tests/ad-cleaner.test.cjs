"use strict";
const fs = require("node:fs");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const { test } = require("node:test");
const source = fs.readFileSync("app/src/main/assets/reader/ad-cleaner.js", "utf8");
const manifest = JSON.parse(fs.readFileSync("app/src/main/assets/reader/manifest.json", "utf8"));

function element(tag, options = {}) {
  const attrs = { ...(options.attrs || {}) };
  return {
    tagName: tag.toUpperCase(),
    nodeType: 1,
    parentElement: options.parent || null,
    className: options.className || "",
    id: options.id || "",
    textContent: options.text || "",
    style: {},
    position: options.position || "static",
    childrenToScan: options.childrenToScan || [],
    images: options.images || [],
    getAttribute(name) { return attrs[name] ?? null; },
    setAttribute(name, value) { attrs[name] = value; },
    hasAttribute(name) { return name in attrs; },
    querySelectorAll(selector) {
      return selector === "img, picture, canvas" ? this.images : this.childrenToScan;
    },
    getBoundingClientRect() { return options.rect || {width: 355, height: 130}; }
  };
}

function run(host, elements) {
  const html = element("html");
  const body = element("body", {childrenToScan: elements});
  const head = { appendChild(node) { node.isConnected = true; } };
  const doc = {documentElement: html, body, head, createElement: () => ({isConnected: false, textContent: ""})};
  const context = {
    document: doc,
    location: { hostname: host, href: "https://" + host + "/obras/teste/capitulo-191" },
    innerHeight: 900,
    innerWidth: 400,
    getComputedStyle: node => ({position: node.position, zIndex: "100"}),
    MutationObserver: class { observe() {} },
    setTimeout: fn => fn(),
    addEventListener() {},
    URL
  };
  vm.runInNewContext(source, context, {timeout: 1500});
}

test("NeoToons: floating IPTV trial advertisement is removed", () => {
  const overlay = element("div", {
    text: "TESTE GRÁTIS IPTV com canais, filmes e séries",
    position: "fixed",
    rect: {width: 370, height: 145}
  });
  run("neotoons.com.br", [overlay]);
  assert.equal(overlay.getAttribute("data-mangaflow-ad-hidden"), "1");
});

test("NeoToons: chapter navigation and manga panel remain visible", () => {
  const nav = element("nav", {text: "Swordmaster Capítulo 191"});
  const legitimate = element("div", {className: "sticky-header", text: "Cap. 191"});
  const picture = {closest: () => ({}), getAttribute: () => "manga-page.webp"};
  const chapter = element("div", {className: "chapter-reader advertisement", images: [picture]});
  run("neotoons.com.br", [nav, legitimate, chapter]);
  assert.equal(nav.getAttribute("data-mangaflow-ad-hidden"), null);
  assert.equal(legitimate.getAttribute("data-mangaflow-ad-hidden"), null);
  assert.equal(chapter.getAttribute("data-mangaflow-ad-hidden"), null);
});

test("Ad-labelled container is removed without generic modal filtering", () => {
  const ad = element("div", {className: "advertisement"});
  const modal = element("div", {className: "modal"});
  run("example.org", [ad, modal]);
  assert.equal(ad.getAttribute("data-mangaflow-ad-hidden"), "1");
  assert.equal(modal.getAttribute("data-mangaflow-ad-hidden"), null);
});

test("Manifest upgrades the installed GeckoView extension", () => {
  assert.equal(manifest.version, "0.3.0");
  assert.ok(manifest.content_scripts.some(x => x.js.includes("ad-cleaner.js") && x.run_at === "document_start"));
  assert.ok(manifest.content_scripts.some(x => x.js.includes("reader.js") && x.run_at === "document_idle"));
});