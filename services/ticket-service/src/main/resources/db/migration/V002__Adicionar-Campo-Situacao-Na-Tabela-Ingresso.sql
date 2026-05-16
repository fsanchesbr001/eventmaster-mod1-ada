ALTER TABLE ingresso
    ADD COLUMN situacao VARCHAR(20) NOT NULL DEFAULT 'Disponivel' AFTER cpf_participante;

