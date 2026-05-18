DELETE FROM usuarios;
ALTER TABLE usuarios AUTO_INCREMENT = 1;

INSERT INTO usuarios
(login, senha, role, nome)
VALUES('eventmaster@teste.com',
       '$2a$10$2DiXZWqvZxXeN9MITJabPeLN5w.fFZlsbBWJNH4yVjyu5cmIFPb8y',
       'ADMIN',
       'Eventmaster Admin');

