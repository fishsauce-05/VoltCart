"use client";
// UC08 View Product Details, with Add to Cart (UC09).
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { api, vnd, picture, CATEGORY } from "@/lib/api";
import type { ApiError, Category, Product, SpecKey } from "@/lib/types";

/** One row of the specs table: label, product field and optional unit. */
type SpecRow = [label: string, key: SpecKey, unit?: string];

/** The result of the last action: a success message or an error. */
type Message = { ok?: string; error?: string };

const SPECS: Record<Category, SpecRow[]> = {
  LAPTOP: [["CPU", "cpu"], ["RAM", "ramGb", " GB"], ["Storage", "storageGb", " GB SSD"], ["Screen", "screenInch", " inch"], ["Weight", "weightKg", " kg"]],
  PHONE: [["Chipset", "chipset"], ["RAM", "ramGb", " GB"], ["Storage", "storageGb", " GB"], ["Screen", "screenInch", " inch"], ["Battery", "batteryMah", " mAh"]],
  HEADPHONES: [["Bluetooth", "bluetoothVersion"], ["Battery", "batteryHours", " hours"], ["Noise cancelling", "noiseCancelling"], ["Form", "formFactor"]],
};

export default function ProductPage() {
  const { id } = useParams<{ id: string }>();
  const [p, setP] = useState<Product | null>(null);
  const [qty, setQty] = useState<number | string>(1);
  const [msg, setMsg] = useState<Message | null>(null);

  useEffect(() => { api.product(id).then(setP).catch((e: ApiError) => setMsg({ error: e.message })); }, [id]);

  async function add() {
    try {
      await api.addItem(p!.productId, Number(qty));
      setMsg({ ok: `${qty} × ${p!.name} added to your cart` });
    } catch (e) { setMsg({ error: (e as ApiError).message }); }
  }

  if (!p) return msg ? <div className="notice error">{msg.error}</div> : <p>Loading…</p>;
  const show = (v: Product[SpecKey]) => (v === true ? "Yes" : v === false ? "No" : String(v).replace("_", "-"));
  return (
    <div className="detail">
      <img src={picture(p.imageUrl)} alt={p.name} />
      <div className="panel">
        <span className="badge">{CATEGORY[p.category]}</span>
        <h1>{p.name}</h1>
        <div className="price total">{vnd(p.price)}</div>
        <p>{p.description}</p>
        <table className="specs"><tbody>
          <tr><td>Brand</td><td>{p.brand}</td></tr>
          {SPECS[p.category].map(([label, key, unit]) => <tr key={key}><td>{label}</td><td>{show(p[key])}{unit || ""}</td></tr>)}
          <tr><td>Sold by</td><td>{p.shop.shopName}</td></tr>
          <tr><td>In stock</td><td>{p.availableStock}</td></tr>
        </tbody></table>
        <div className="searchbar">
          <input className="qty" type="number" min="1" max="5" value={qty} onChange={(e) => setQty(e.target.value)} />
          <button className="primary" onClick={add}>Add to cart</button>
          <Link href="/cart" className="button">Go to cart</Link>
        </div>
        {msg?.ok && <div className="notice success">{msg.ok}</div>}
        {msg?.error && <div className="notice error">{msg.error}</div>}
      </div>
    </div>
  );
}
