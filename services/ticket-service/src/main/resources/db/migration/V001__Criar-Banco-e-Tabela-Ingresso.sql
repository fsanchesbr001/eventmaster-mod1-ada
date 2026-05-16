CREATE DATABASE IF NOT EXISTS ingresso CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ingresso (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_ingresso BIGINT NOT NULL,
    evento VARCHAR(255) NOT NULL,
    data DATE NOT NULL,
    hora TIME NOT NULL,
    tipo_ingresso VARCHAR(25) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    nome_participante VARCHAR(255) NOT NULL,
    cpf_participante VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ingresso_id_ingresso UNIQUE (id_ingresso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tabela de ingressos';

