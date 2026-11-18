"use client";
// Order confirmation after checkout.
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { api, vnd } from "@/lib/api";
import type { ApiError, Order } from "@/lib/types";

export default function OrderPage() {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string>("");
  useEffect(() => { api.order(id).then(setOrder).catch((e: ApiError) => setError(e.message)); }, [id]);

  if (!order) return error ? <div className="notice error">{error}</div> : <p>Loading…</p>;
  const placed = order.status === "PLACED";
  return (
    <div className="panel">
      <div className="notice success">✔ Order #{order.orderId} has been {placed ? "placed" : "created and is awaiting your bank transfer"}.</div>
      <h1>Order #{order.orderId} — {order.shop.shopName}</h1>
      <table className="specs"><tbody>
        <tr><td>Status</td><td>{order.status}</td></tr>
        <tr><td>Payment</td><td>{order.payment.method} · {order.payment.status}</td></tr>
        <tr><td>Deliver to</td><td>{order.customer.fullName}, {order.deliveryAddress}</td></tr>
        <tr><td>Placed at</td><td>{order.createdAt.replace("T", " ")}</td></tr>
      </tbody></table>
      <table className="specs"><tbody>
        {order.items.map((i) => (
          <tr key={i.orderItemId}><td>{i.quantity} × {i.productName} ({i.sku})</td><td>{vnd(i.unitPrice)} each — {vnd(i.lineAmount)}</td></tr>
        ))}
        <tr><td><strong>Total</strong></td><td className="total">{vnd(order.totalAmount)}</td></tr>
      </tbody></table>
      <Link href="/cart" className="button">Back to cart</Link> <Link href="/" className="button">Continue shopping</Link>
    </div>
  );
}
