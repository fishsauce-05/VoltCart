# VoltCart: marketplace for laptops, phones and Bluetooth headphones

Prototype of the essay *Information System Analysis and Design*: Search, Cart and Checkout of a multi-vendor
marketplace, with a multimodal AI search (text in Vietnamese or English, voice, image).

- Author: Ngô Đức Mạnh, B23DCAT180, group 04, class E23TNTT01
- Lecturer: PGS.TS. Trần Đình Quế
- Posts and Telecommunications Institute of Technology, 2026

Every class, method, table and column has the name it has in the diagrams of the report.

## Requirements

Java 21 or newer, Python 3.11 or newer, Node.js 20 or newer. No paid service or key is needed. The first start
of ai-search downloads three open models (about 0.7 GB) into `ai-search/models/`; after that it works offline.

## Run on localhost (three terminals)

```bash
# 1. AI search service, http://localhost:8000
cd ai-search
python -m venv .venv
.venv\Scripts\activate            # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --port 8000
```

```bash
# 2. Backend, http://localhost:8080/api   (creates backend/data/app.db and builds the search index)
cd backend
mvnw spring-boot:run              # macOS/Linux: ./mvnw spring-boot:run
```

```bash
# 3. Frontend, http://localhost:3000
cd frontend
npm install
npm run dev
```

Open http://localhost:3000. The screens act as the demo customer Lê Minh Anh (customer 1).
Start ai-search before the backend so the index is built at once; otherwise call
`POST http://localhost:8080/api/search/index` later. Without ai-search, text search still works with keywords.

## Tests

```bash
cd backend && mvnw test           # 25 end-to-end tests (a fake ai-search is started by the tests)
cd ai-search && pytest -v tests   # 9 tests of the endpoints, with fake models
```

`ai-search/tests/eval_search.py` measures search quality against the running system (Chapter 5 of the report).

## Demo data

`backend/src/main/resources/db/seed.sql`: 5 shops, 35 products (12 laptops, 11 phones, 12 Bluetooth
headphones), customers 1 and 2 and an administrator (3). Every demo account has the password `Demo@123`,
stored as a salted PBKDF2 hash (sign-in is outside the prototype). Prices are approximate and only for the demo.

## API

| Method | Path | Use case |
|---|---|---|
| GET | /api/search?q=&category=&limit= | UC07 text search |
| POST | /api/search/voice (multipart `audio`) | UC07 voice search |
| POST | /api/search/image (multipart `image`) | UC07 image search |
| POST | /api/search/index | rebuild the product vectors |
| GET | /api/products/{id} | UC08 product page |
| GET | /api/customers/{id}/cart | UC09 |
| POST / PATCH / DELETE | /api/customers/{id}/cart/items[/{productId}] | UC09 |
| POST | /api/checkout | UC10 |
| GET | /api/orders/{id} | order confirmation |

## Structure

```text
ecommerce/
├── frontend/                 Next.js screens (client of the API; not in the diagrams)
├── backend/                  Spring Boot + Maven
│   ├── pom.xml
│   └── src/main/java/com/ecommerce/
│       ├── EcommerceApplication.java
│       ├── presentation/     SearchController, CartController, CheckoutController
│       ├── application/      SearchService, CartService, CheckoutService
│       ├── infrastructure/   CartRepository, ProductRepository, ProductVectorRepository, OrderRepository
│       ├── domain/user/      Customer, Seller, UserRole, Shop
│       ├── domain/product/   Product, ProductVector, Laptop, Phone, BluetoothHeadphones
│       ├── domain/order/     Cart, CartItem, Order, OrderItem, Payment
│       └── common/           config/ (CorsConfig, RestClientConfig), infrastructure/ (BaseRepository),
│                             enums/ (SearchType, CartStatus)
│   └── src/main/resources/   application.yaml, db/schema.sql, db/seed.sql, static/images/
├── ai-search/                Python FastAPI
│   ├── main.py, SearchController.py
│   ├── voice/                VoiceSearchService, TextEmbedding, SpeechToTextAdapter
│   └── image/                ImageSearchService, ImageEncoder
└── README.md
```
