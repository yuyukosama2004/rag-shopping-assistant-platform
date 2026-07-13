ALTER TABLE t_order
    ADD CONSTRAINT chk_order_amount CHECK (total_amount >= 0);

ALTER TABLE t_order_item
    ADD CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    ADD CONSTRAINT chk_order_item_amount CHECK (price >= 0 AND subtotal >= 0);

ALTER TABLE t_stock
    ADD CONSTRAINT chk_stock_values CHECK (total >= 0 AND locked >= 0 AND available >= 0 AND total = locked + available);

ALTER TABLE t_shopping_cart
    ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0);
