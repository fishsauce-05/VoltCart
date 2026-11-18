"use client";
// UC10 Checkout of one shop's items.
import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api, vnd, groupByShop } from "@/lib/api";
import type { ApiError, Cart, PaymentMethod } from "@/lib/types";

export default function CheckoutPage() {
  const { shopId } = useParams<{ shopId: string }>();
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [address, setAddress] = useState<string>("");
  const [method, setMethod] = useState<PaymentMethod>("COD");
  const [error, setError] = useState<string>("");
  const [busy, setBusy] = useState<boolean>(false);
  // one reference per visit of the page: pressing the button twice cannot create two orders (BR10)
  // eslint-disable-next-line react-hooks/purity -- computed once per visit on purpose (kept from the JS version)
  const reference = useMemo(() => "web-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8), []);

  async function load() {
    const c = await api.cart();
    setCart(c);
    setAddress((a) => a || c.customer.defaultAddress || "");
  }
  // eslint-disable-next-line react-hooks/set-state-in-effect -- loads the cart once on mount (kept from the JS version)
  useEffect(() => { load().catch((e: ApiError) => setError(e.message)); }, []);

  const group = groupByShop(cart).find((g) => String(g.shop.shopId) === String(shopId));

  async function placeOrder() {
    setBusy(true); setError("");
    try {
      const order = await api.checkout({ shopId: Number(shopId), deliveryAddress: address, paymentMethod: method,
                                         expectedTotal: group!.subtotal, requestReference: reference });
      router.push(`/orders/${order.orderId}`);
    } catch (e) {
      const err = e as ApiError;
      setError(err.message);
      if (err.status === 409) await load();      // show the current prices and stock
      setBusy(false);
    }
  }

  if (!cart) return error ? <div className="notice error">{error}</div> : <p>Loading…</p>;
  if (!group) return <div className="panel">There is nothing of this shop in your cart.</div>;
  return (
    <>
      <h1>Checkout — {group.shop.shopName}</h1>
      <div className="two">
        <div className="panel">
          <strong>Delivery address</strong>
          <textarea rows={2} value={address} onChange={(e) => setAddress(e.target.value)} />
          <div className="hint">Prefilled from your profile; changing it here changes only this order.</div>
          <p><strong>Payment method</strong></p>
          <label className="radio"><input type="radio" checked={method === "COD"} onChange={() => setMethod("COD")} /> Cash on delivery (up to 50,000,000 ₫)</label>
          <label className="radio"><input type="radio" checked={method === "BANK_TRANSFER"} onChange={() => setMethod("BANK_TRANSFER")} /> Bank transfer</label>
          {error && <div className="notice error">{error}</div>}
        </div>
        <div className="panel">
          <strong>Order summary</strong>
          {group.items.map((i) => (
            <div key={i.cartItemId} style={{ display: "flex", justifyContent: "space-between", padding: "6px 0" }}>
              <span>{i.quantity} × {i.product.name}</span><span>{vnd(i.lineAmount)}</span>
            </div>
          ))}
          <hr />
          <div style={{ display: "flex", justifyContent: "space-between" }}><span>Total</span><span className="total">{vnd(group.subtotal)}</span></div>
          <button className="primary" style={{ width: "100%", marginTop: 14 }} onClick={placeOrder} disabled={busy}>
            {busy ? "Placing order…" : "Place order"}
          </button>
        </div>
      </div>
    </>
  );
}
