-- Older data can contain negative stock locks left by interrupted rollbacks.
-- Normalize those derived values before enforcing the invariant.
UPDATE t_stock
SET locked = LEAST(GREATEST(locked, 0), GREATEST(total, 0)),
    available = GREATEST(total, 0) - LEAST(GREATEST(locked, 0), GREATEST(total, 0)),
    total = GREATEST(total, 0)
WHERE total < 0 OR locked < 0 OR available < 0 OR total <> locked + available;

ALTER TABLE t_order
    ADD CONSTRAINT chk_order_amount CHECK (total_amount >= 0);

ALTER TABLE t_order_item
    ADD CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    ADD CONSTRAINT chk_order_item_amount CHECK (price >= 0 AND subtotal >= 0);

ALTER TABLE t_stock
    ADD CONSTRAINT chk_stock_values CHECK (total >= 0 AND locked >= 0 AND available >= 0 AND total = locked + available);

ALTER TABLE t_shopping_cart
    ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0);
