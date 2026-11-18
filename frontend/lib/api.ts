// Calls to the Spring Boot backend. The demo signs in nobody: customer 1 (Lê Minh Anh) is used throughout.
import type {
  ApiError,
  Cart,
  Category,
  CheckoutData,
  Order,
  Product,
  SearchResult,
  ShopGroup,
  VoiceSearchResult,
} from "@/lib/types";

export const BACKEND: string = process.env.NEXT_PUBLIC_API || "http://localhost:8080";
export const CUSTOMER_ID = 1;

async function call<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(BACKEND + "/api" + path, options);
  const body: unknown = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error: ApiError = new Error((body as { error?: string }).error || `Request failed (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return body as T;
}

const json = (method: string, data: unknown): RequestInit => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(data),
});

export const api = {
  searchText: (q: string, category: Category | "") =>
    call<SearchResult>(`/search?q=${encodeURIComponent(q)}&category=${category || ""}&limit=24`),
  searchVoice: (blob: Blob) => {
    const form = new FormData();
    form.append("audio", blob, "query.webm");
    return call<VoiceSearchResult>("/search/voice?limit=24", { method: "POST", body: form });
  },
  searchImage: (file: File) => {
    const form = new FormData();
    form.append("image", file, file.name);
    return call<SearchResult>("/search/image?limit=12", { method: "POST", body: form });
  },
  product: (id: number | string) => call<Product>(`/products/${id}`),
  cart: () => call<Cart>(`/customers/${CUSTOMER_ID}/cart`),
  addItem: (productId: number, quantity: number) =>
    call<Cart>(`/customers/${CUSTOMER_ID}/cart/items`, json("POST", { productId, quantity })),
  updateQuantity: (productId: number, quantity: number) =>
    call<Cart>(`/customers/${CUSTOMER_ID}/cart/items/${productId}`, json("PATCH", { quantity })),
  removeItem: (productId: number) =>
    call<Cart>(`/customers/${CUSTOMER_ID}/cart/items/${productId}`, { method: "DELETE" }),
  checkout: (data: CheckoutData) => call<Order>("/checkout", json("POST", { customerId: CUSTOMER_ID, ...data })),
  order: (id: number | string) => call<Order>(`/orders/${id}`),
};

export const vnd = (n: number): string => new Intl.NumberFormat("vi-VN").format(n) + " ₫";
export const picture = (url: string): string => BACKEND + url;

export const CATEGORY: Record<Category, string> = { LAPTOP: "Laptop", PHONE: "Phone", HEADPHONES: "Bluetooth headphones" };

/** The cart's items grouped by shop, with each group's subtotal (one checkout per shop). */
export function groupByShop(cart: Cart | null | undefined): ShopGroup[] {
  const groups = new Map<number, ShopGroup>();
  for (const item of cart?.items || []) {
    const shop = item.product.shop;
    if (!groups.has(shop.shopId)) groups.set(shop.shopId, { shop, items: [], subtotal: 0 });
    const g = groups.get(shop.shopId)!;
    g.items.push(item);
    g.subtotal += item.lineAmount;
  }
  return [...groups.values()];
}
