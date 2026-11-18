# VoltCart — Frontend (TypeScript)

TypeScript version of the VoltCart storefront (Next.js App Router). It is functionally identical to the
JavaScript version in `../frontend` and runs on its own.

## Structure

```
app/
  layout.tsx                 header + navigation
  page.tsx                   UC07 Search Products (text / voice / image)
  products/[id]/page.tsx     UC08 View Product Details + Add to Cart
  cart/page.tsx              UC09 Manage Cart (grouped by shop)
  checkout/[shopId]/page.tsx UC10 Checkout of one shop
  orders/[id]/page.tsx       Order confirmation
  globals.css
lib/
  api.ts                     calls to the Spring Boot backend + helpers (vnd, picture, groupByShop)
  types.ts                   types of the data exchanged with the backend
```

## Running

The Spring Boot backend must be running (default `http://localhost:8080`). Change it with the
`NEXT_PUBLIC_API` environment variable (e.g. in `.env.local`).

```bash
npm install
npm run dev        # http://localhost:3000
npm run build && npm run start
npm run typecheck  # next typegen + tsc --noEmit
npm run lint
```
