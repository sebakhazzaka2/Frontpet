-- =====================================================================
-- V6 — Preço original para produtos e variantes com desconto
-- =====================================================================
-- price_original NULLABLE: quando preenchido, o produto está em promoção.
-- price = preço atual (de venda). price_original = preço riscado.
-- A seção "Nuestros descuentos" da landing filtra WHERE price_original IS NOT NULL.
-- =====================================================================

ALTER TABLE products
    ADD COLUMN price_original NUMERIC(10,2) CHECK (price_original > price);

ALTER TABLE product_variants
    ADD COLUMN price_original NUMERIC(10,2) CHECK (price_original > price);

COMMENT ON COLUMN products.price_original IS
    'Preço original antes do desconto. NULL = sem promoção. Quando preenchido, price é o preço de oferta.';

COMMENT ON COLUMN product_variants.price_original IS
    'Preço original antes do desconto. NULL = sem promoção. Quando preenchido, price é o preço de oferta.';
