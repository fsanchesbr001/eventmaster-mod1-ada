package com.fabriciosanches.userservice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class Utilidades {
    public static Boolean validarCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        boolean allEqual = true;
        for (int i = 1; i < cpf.length(); i++) {
            if (cpf.charAt(i) != cpf.charAt(0)) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            return false;
        }

        // Validação do CPF
        int soma = 0;
        int peso = 10;

        for (int i = 0; i < 9; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * peso--;
        }

        int primeiroDigitoVerificador = 11 - (soma % 11);
        if (primeiroDigitoVerificador >= 10) {
            primeiroDigitoVerificador = 0;
        }

        soma = 0;
        peso = 11;

        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * peso--;
        }

        int segundoDigitoVerificador = 11 - (soma % 11);
        if (segundoDigitoVerificador >= 10) {
            segundoDigitoVerificador = 0;
        }

        return primeiroDigitoVerificador == Character.getNumericValue(cpf.charAt(9))
                && segundoDigitoVerificador == Character.getNumericValue(cpf.charAt(10));
    }

    public static String encriptaSenha(String senha){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(senha);
    }

    // Método para converter uma string normal em base64
    public static String encodeToBase64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    // Método para converter uma string em base64 para uma string normal
    public static String decodeFromBase64(String base64Input) {
        return new String(Base64.getDecoder().decode(base64Input));
    }

    public static String gerarSenhaAleatoria() {
        final int LENGTH = 10;
        final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        final String DIGITS = "0123456789";
        final String SPECIAL = "!@#$%^&*()-_+=[]{};:,.<>?/"; // sem espaços
        final String ALPHANUM = UPPER + LOWER + DIGITS;
        final String ALL_ALLOWED = ALPHANUM + SPECIAL;

        SecureRandom random = new SecureRandom();
        char[] password = new char[LENGTH];

        // 1) Garantir que o primeiro caractere seja letra ou número
        password[0] = ALPHANUM.charAt(random.nextInt(ALPHANUM.length()));
        boolean hasUpper = Character.isUpperCase(password[0]);
        boolean hasLower = Character.isLowerCase(password[0]);
        boolean hasDigit = Character.isDigit(password[0]);
        boolean hasSpecial = false; // não pode ser special no primeiro

        // 2) Preparar lista de caracteres obrigatórios que faltam
        List<Character> requiredChars = new ArrayList<>();
        if (!hasUpper) requiredChars.add(UPPER.charAt(random.nextInt(UPPER.length())));
        if (!hasLower) requiredChars.add(LOWER.charAt(random.nextInt(LOWER.length())));
        if (!hasDigit) requiredChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        if (!hasSpecial) requiredChars.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        int remainingPositions = LENGTH - 1;
        int requiredCount = requiredChars.size();

        List<Character> pool = new ArrayList<>();
        // 3) Preencher com caracteres aleatórios (exceto os que já garantimos) até sobrar espaço para os obrigatórios
        for (int i = 0; i < remainingPositions - requiredCount; i++) {
            pool.add(ALL_ALLOWED.charAt(random.nextInt(ALL_ALLOWED.length())));
        }
        // 4) Adicionar os caracteres obrigatórios
        pool.addAll(requiredChars);

        // 5) Embaralhar os caracteres que irão para as posições 1..9
        Collections.shuffle(pool, random);

        for (int i = 0; i < pool.size(); i++) {
            password[i + 1] = pool.get(i);
        }

        return new String(password);
    }

}
