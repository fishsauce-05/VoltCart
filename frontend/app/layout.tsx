import type { Metadata } from "next";
import "./globals.css";
import Link from "next/link";

export const metadata: Metadata = { title: "VoltCart", description: "Laptops, phones and Bluetooth headphones" };

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>
        <header className="top">
          <Link href="/" className="brand">⚡ VoltCart</Link>
          <span className="tag">Laptops · Phones · Bluetooth audio</span>
          <nav>
            <Link href="/">Search</Link>
            <Link href="/cart">Cart</Link>
          </nav>
          <span className="who">Nguyễn Thị Minh Hằng</span>
        </header>
        <main>{children}</main>
      </body>
    </html>
  );
}
