package com.fabriciosanches.userservice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarSenha {
    public static void main(String[] args) {
        String password = "event@123";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        System.out.println("PASSWORD=" + password);
        System.out.println("HASH=" + hash);
        System.out.println("MATCHES=" + encoder.matches(password, hash));
    }
}
