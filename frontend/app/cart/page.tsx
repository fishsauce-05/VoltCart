"use client";
// UC09 Manage Cart: the items grouped by shop, each group checked out on its own.
import { useEffect, useState } from "react";
import Link from "next/link";
import { api, vnd, picture, groupByShop } from "@/lib/api";
import type { ApiError, Cart } from "@/lib/types";

export default function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string>("");

  useEffect(() => { api.cart().then(setCart).catch((e: ApiError) => setError(e.message)); }, []);

  async function change(task: () => Promise<Cart>) {
    setError("");
    try { setCart(await task()); } catch (e) { setError((e as ApiError).message); }
  }

  if (!cart) return error ? <div className="notice error">{error}</div> : <p>Loading…</p>;
  const groups = groupByShop(cart);
  return (
    <>
      <h1>Your cart</h1>
      {error && <div className="notice error">{error}</div>}
      {!groups.length && <div className="panel">The cart is empty. <Link href="/" className="price">Search products</Link></div>}
      {groups.map((g) => (
        <div key={g.shop.shopId} className="panel shopgroup">
          <strong>🏪 {g.shop.shopName}</strong>
          {g.items.map((i) => (
            <div key={i.cartItemId} className="line">
              <img src={picture(i.product.imageUrl)} alt="" />
              <div><div>{i.product.name}</div><div className="shopname">{i.product.availableStock} in stock</div></div>
              <div>{vnd(i.product.price)}</div>
              <input className="qty" type="number" min="1" max="5" defaultValue={i.quantity}
                     onChange={(e) => { if (e.target.value) change(() => api.updateQuantity(i.product.productId, Number(e.target.value))); }} />
              <div className="price">{vnd(i.lineAmount)}</div>
              <button onClick={() => change(() => api.removeItem(i.product.productId))}>Remove</button>
            </div>
          ))}
          <div className="groupfoot">
            <span>Subtotal of this shop: <span className="total">{vnd(g.subtotal)}</span></span>
            <Link className="button primary" href={`/checkout/${g.shop.shopId}`}>Check out {g.shop.shopName}</Link>
          </div>
        </div>
      ))}
      {groups.length > 1 && <div className="hint">Each shop ships its own parcel, so each shop is checked out separately.</div>}
    </>
  );
}
