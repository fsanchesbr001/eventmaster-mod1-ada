-- fichatecnica.usuarios definição

CREATE TABLE `usuarios` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `login` varchar(255) NOT NULL,
                            `senha` varchar(255) NULL,
                            `nome` varchar(255) NULL,
                            `role` varchar(100) NULL,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tabela com usuários';

INSERT INTO usuarios
(login, senha, role, nome)
VALUES('fsanchesbr001@gmail.com',
       '$2a$10$i42msHOOetd3HmoNwsbwXO3l.RVdpvXVqdvAY/VT3oZUhANMeBXkO',
       'ADMIN',
       'Fabricio Sanches');

INSERT INTO usuarios
(login, senha, role, nome)
VALUES('fabricio@fabriciosanches.com',
       '$2a$10$i42msHOOetd3HmoNwsbwXO3l.RVdpvXVqdvAY/VT3oZUhANMeBXkO',
       'USER',
       'Fabricio Sanches 2');
