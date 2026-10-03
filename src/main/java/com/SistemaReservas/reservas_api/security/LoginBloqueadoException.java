package com.SistemaReservas.reservas_api.security;

public class LoginBloqueadoException extends RuntimeException {
    public LoginBloqueadoException(String message) {
        super(message);
    }
}
