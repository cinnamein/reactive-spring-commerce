-- db/schema.sql
CREATE TABLE IF NOT EXISTS products
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    seller VARCHAR(255) NOT NULL
);
