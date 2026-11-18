// Shapes of the data exchanged with the Spring Boot backend.

/** Product categories sold on VoltCart. */
export type Category = "LAPTOP" | "PHONE" | "HEADPHONES";

/** How an order is paid for. */
export type PaymentMethod = "COD" | "BANK_TRANSFER";

export interface Shop {
  shopId: number;
  shopName: string;
}

export interface Customer {
  customerId: number;
  fullName: string;
  defaultAddress?: string | null;
}

/** Category-specific technical details; only those of the product's category are filled in. */
export interface ProductSpecs {
  // laptop
  cpu?: string | null;
  weightKg?: number | null;
  // phone
  chipset?: string | null;
  batteryMah?: number | null;
  // laptop & phone
  ramGb?: number | null;
  storageGb?: number | null;
  screenInch?: number | null;
  // headphones
  bluetoothVersion?: string | null;
  batteryHours?: number | null;
  noiseCancelling?: boolean | null;
  formFactor?: string | null;
}

/** The keys of {@link ProductSpecs}, shown in the product details table. */
export type SpecKey = keyof ProductSpecs;

export interface Product extends ProductSpecs {
  productId: number;
  name: string;
  brand: string;
  category: Category;
  description: string;
  specSummary: string;
  price: number;
  imageUrl: string;
  availableStock: number;
  shop: Shop;
}

/** A search answers either with the list itself or with an object carrying the list. */
export type SearchResult = Product[] | { products: Product[] };

/** A voice search also returns what the speech recogniser heard. */
export interface VoiceSearchResult {
  transcript: string;
  products: Product[];
}

export interface CartItem {
  cartItemId: number;
  product: Product;
  quantity: number;
  lineAmount: number;
}

export interface Cart {
  customer: Customer;
  items: CartItem[];
}

/** The cart's items of one shop, with their subtotal (one checkout per shop). */
export interface ShopGroup {
  shop: Shop;
  items: CartItem[];
  subtotal: number;
}

/** The body of a checkout, without the customer (added by the API layer). */
export interface CheckoutData {
  shopId: number;
  deliveryAddress: string;
  paymentMethod: PaymentMethod;
  expectedTotal: number;
  requestReference: string;
}

export interface OrderItem {
  orderItemId: number;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  lineAmount: number;
}

export interface Payment {
  method: PaymentMethod;
  status: string;
}

export interface Order {
  orderId: number;
  status: string;
  shop: Shop;
  customer: Customer;
  payment: Payment;
  deliveryAddress: string;
  createdAt: string;
  items: OrderItem[];
  totalAmount: number;
}

/** An error thrown by the API layer, carrying the HTTP status of the failed request. */
export type ApiError = Error & { status?: number };
