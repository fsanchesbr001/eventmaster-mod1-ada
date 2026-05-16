ALTER TABLE ingresso
    ADD COLUMN pedido_id BIGINT NULL;

CREATE INDEX idx_ingresso_pedido_id ON ingresso (pedido_id);

