-- Physical data model of Figure 4.3 (design/data_model.txt). Rules that protect the data are constraints here,
-- so a program error still cannot break them: stock never negative, one payment per order, SKU unique per shop.
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS customers (
    customerId     INTEGER PRIMARY KEY,
    fullName       VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL UNIQUE,
    phone          VARCHAR(20),
    defaultAddress VARCHAR(255),
    passwordHash   VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'ADMINISTRATOR'))
);

CREATE TABLE IF NOT EXISTS sellers (
    sellerId     INTEGER PRIMARY KEY,
    fullName     VARCHAR(120) NOT NULL,
    email        VARCHAR(160) NOT NULL UNIQUE,
    passwordHash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL CHECK (role = 'SELLER')
);

CREATE TABLE IF NOT EXISTS shops (
    shopId        INTEGER PRIMARY KEY,
    sellerId      INTEGER NOT NULL REFERENCES sellers (sellerId),
    shopName      VARCHAR(120) NOT NULL UNIQUE,
    pickupAddress VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS products (
    productId      INTEGER PRIMARY KEY,
    shopId         INTEGER NOT NULL REFERENCES shops (shopId),
    sku            VARCHAR(40)  NOT NULL,
    name           VARCHAR(160) NOT NULL,
    brand          VARCHAR(60)  NOT NULL,
    category       VARCHAR(20)  NOT NULL CHECK (category IN ('LAPTOP', 'PHONE', 'HEADPHONES')),
    description    TEXT         NOT NULL,
    price          INTEGER      NOT NULL CHECK (price > 0),
    availableStock INTEGER      NOT NULL CHECK (availableStock >= 0),
    committedStock INTEGER      NOT NULL DEFAULT 0 CHECK (committedStock >= 0),
    imageUrl       VARCHAR(255) NOT NULL,
    status         VARCHAR(20)  NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'DISCONTINUED')),
    UNIQUE (shopId, sku)
);

CREATE TABLE IF NOT EXISTS laptops (
    productId  INTEGER PRIMARY KEY REFERENCES products (productId),
    cpu        VARCHAR(80) NOT NULL,
    ramGb      INTEGER     NOT NULL CHECK (ramGb > 0),
    storageGb  INTEGER     NOT NULL CHECK (storageGb > 0),
    screenInch DOUBLE      NOT NULL,
    weightKg   DOUBLE      NOT NULL
);

CREATE TABLE IF NOT EXISTS phones (
    productId  INTEGER PRIMARY KEY REFERENCES products (productId),
    chipset    VARCHAR(80) NOT NULL,
    ramGb      INTEGER     NOT NULL CHECK (ramGb > 0),
    storageGb  INTEGER     NOT NULL CHECK (storageGb > 0),
    screenInch DOUBLE      NOT NULL,
    batteryMah INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS bluetoothHeadphones (
    productId        INTEGER PRIMARY KEY REFERENCES products (productId),
    bluetoothVersion VARCHAR(10) NOT NULL,
    batteryHours     INTEGER     NOT NULL,
    noiseCancelling  BOOLEAN     NOT NULL,
    formFactor       VARCHAR(20) NOT NULL CHECK (formFactor IN ('OVER_EAR', 'ON_EAR', 'IN_EAR'))
);

CREATE TABLE IF NOT EXISTS productVectors (
    productId    INTEGER PRIMARY KEY REFERENCES products (productId),
    textVector   BLOB         NOT NULL,
    imageVector  BLOB,
    modelVersion VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS carts (
    cartId     INTEGER PRIMARY KEY,
    customerId INTEGER     NOT NULL REFERENCES customers (customerId),
    status     VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CHECKED_OUT'))
);

CREATE TABLE IF NOT EXISTS cartItems (
    cartItemId INTEGER PRIMARY KEY,
    cartId     INTEGER   NOT NULL REFERENCES carts (cartId),
    productId  INTEGER   NOT NULL REFERENCES products (productId),
    quantity   INTEGER   NOT NULL CHECK (quantity BETWEEN 1 AND 5),
    addedAt    TIMESTAMP NOT NULL,
    UNIQUE (cartId, productId)
);

CREATE TABLE IF NOT EXISTS orders (
    orderId          INTEGER PRIMARY KEY,
    customerId       INTEGER      NOT NULL REFERENCES customers (customerId),
    shopId           INTEGER      NOT NULL REFERENCES shops (shopId),
    status           VARCHAR(30)  NOT NULL CHECK (status IN ('AWAITING_PAYMENT', 'PLACED', 'CANCELLED')),
    deliveryAddress  VARCHAR(255) NOT NULL CHECK (length(trim(deliveryAddress)) > 0),
    totalAmount      INTEGER      NOT NULL CHECK (totalAmount > 0),
    requestReference VARCHAR(64)  NOT NULL UNIQUE,
    createdAt        TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS orderItems (
    orderItemId INTEGER PRIMARY KEY,
    orderId     INTEGER      NOT NULL REFERENCES orders (orderId),
    productId   INTEGER      NOT NULL REFERENCES products (productId),
    sku         VARCHAR(40)  NOT NULL,
    productName VARCHAR(160) NOT NULL,
    unitPrice   INTEGER      NOT NULL CHECK (unitPrice > 0),
    quantity    INTEGER      NOT NULL CHECK (quantity > 0)
);

CREATE TABLE IF NOT EXISTS payments (
    paymentId INTEGER PRIMARY KEY,
    orderId   INTEGER     NOT NULL UNIQUE REFERENCES orders (orderId),
    method    VARCHAR(20) NOT NULL CHECK (method IN ('COD', 'BANK_TRANSFER')),
    amount    INTEGER     NOT NULL CHECK (amount > 0),
    status    VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_COLLECTION', 'AWAITING_TRANSFER', 'PAID', 'FAILED')),
    reference VARCHAR(64)
);
