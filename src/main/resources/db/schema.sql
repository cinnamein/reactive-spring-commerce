CREATE TABLE IF NOT EXISTS products
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    255
) NOT NULL,
    price BIGINT NOT NULL,
    seller VARCHAR
(
    255
) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    status VARCHAR
(
    20
) NOT NULL DEFAULT 'DRAFT'
    );

CREATE TABLE IF NOT EXISTS product_options
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    product_id
    BIGINT
    NOT
    NULL
    REFERENCES
    products
(
    id
) ON DELETE CASCADE,
    size VARCHAR
(
    10
) NOT NULL,
    color VARCHAR
(
    20
) NOT NULL,
    additional_price BIGINT NOT NULL DEFAULT 0,
    stock_quantity INT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS product_images
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    product_id
    BIGINT
    NOT
    NULL
    REFERENCES
    products
(
    id
) ON DELETE CASCADE,
    url VARCHAR
(
    500
) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    primary_image BOOLEAN NOT NULL DEFAULT FALSE
    );