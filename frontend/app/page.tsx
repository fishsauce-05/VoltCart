"use client";
// UC07 Search Products: by text, by voice (recorded in the browser) or by image.
import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import Link from "next/link";
import { api, vnd, picture, CATEGORY } from "@/lib/api";
import type { ApiError, Category, Product, SearchResult } from "@/lib/types";

export default function SearchPage() {
  const [q, setQ] = useState<string>("");
  const [category, setCategory] = useState<Category | "">("");
  const [products, setProducts] = useState<Product[]>([]);
  const [info, setInfo] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [busy, setBusy] = useState<boolean>(false);
  const [recording, setRecording] = useState<boolean>(false);
  const [preview, setPreview] = useState<string | null>(null);
  const recorder = useRef<MediaRecorder | null>(null);

  useEffect(() => { run(() => api.searchText("", ""), (_, list) => `${list.length} products on sale`); }, []);

  async function run<T extends SearchResult>(task: () => Promise<T>, message: (result: T, list: Product[]) => string) {
    setBusy(true); setError("");
    try {
      const result = await task();
      const list = Array.isArray(result) ? result : result.products;
      setProducts(list);
      setInfo(message(result, list));
    } catch (e) {
      setError((e as ApiError).message); setProducts([]); setInfo("");
    } finally { setBusy(false); }
  }

  const searchText = (e?: FormEvent<HTMLFormElement>) => {
    e?.preventDefault(); setPreview(null);
    run(() => api.searchText(q, category), (_, list) =>
      list.length ? `${list.length} products for “${q || "all"}”` : "No product matches; try other words");
  };

  async function toggleVoice() {
    if (recording) { recorder.current!.stop(); return; }
    setError("");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const chunks: Blob[] = [];
      const rec = new MediaRecorder(stream);
      rec.ondataavailable = (e: BlobEvent) => chunks.push(e.data);
      rec.onstop = () => {
        stream.getTracks().forEach((t) => t.stop());
        setRecording(false); setPreview(null);
        run(() => api.searchVoice(new Blob(chunks, { type: "audio/webm" })), (r, list) => {
          setQ(r.transcript);
          return `Heard: “${r.transcript}” — ${list.length} products. Edit the text and press Search if it is wrong.`;
        });
      };
      recorder.current = rec; rec.start(); setRecording(true);
    } catch { setError("The microphone is not available; please type your query"); }
  }

  function searchImage(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setPreview(URL.createObjectURL(file)); setQ("");
    run(() => api.searchImage(file), (_, list) =>
      list.length ? `${list.length} products look like your picture` : "No product looks like this picture");
    e.target.value = "";
  }

  return (
    <>
      <h1>Find a laptop, phone or Bluetooth headphones</h1>
      <form className="panel" onSubmit={searchText}>
        <div className="searchbar">
          <input type="text" value={q} onChange={(e) => setQ(e.target.value)}
                 placeholder="e.g. light laptop for travel, tai nghe chống ồn, budget phone with a big battery" />
          <select value={category} onChange={(e) => setCategory(e.target.value as Category | "")}>
            <option value="">All categories</option>
            {Object.entries(CATEGORY).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <button className="primary" disabled={busy}>Search</button>
          <button type="button" className={recording ? "rec" : ""} onClick={toggleVoice} disabled={busy && !recording}>
            {recording ? "■ Stop" : "🎤 Voice"}
          </button>
          <label className="button">📷 Image<input type="file" accept="image/png,image/jpeg" hidden onChange={searchImage} /></label>
        </div>
        <div className="hint">Search understands meaning, in English or Vietnamese. Results show only products on sale.</div>
        {preview && <div className="notice"><img src={preview} alt="query" height="70" style={{ verticalAlign: "middle" }} /> Your picture</div>}
        {info && <div className="notice">{info}</div>}
        {error && <div className="notice error">{error}</div>}
      </form>
      <div className="grid">
        {products.map((p) => (
          <Link key={p.productId} href={`/products/${p.productId}`} className="card">
            <img src={picture(p.imageUrl)} alt={p.name} />
            <span className="badge">{CATEGORY[p.category]}</span>
            <span className="name">{p.name}</span>
            <span className="spec">{p.specSummary}</span>
            <span className="price">{vnd(p.price)}</span>
            <span className="shopname">{p.shop.shopName} · {p.availableStock} in stock</span>
          </Link>
        ))}
      </div>
    </>
  );
}
