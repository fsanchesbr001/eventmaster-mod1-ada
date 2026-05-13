package com.fabriciosanches.userservice.repository;

import com.fabriciosanches.userservice.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    UserDetails findByLogin(String login);

    @Query(value = "SELECT * FROM usuarios u WHERE u.login = :login",
           nativeQuery = true)
    Usuario findByLoginUsuario(String login);
}
