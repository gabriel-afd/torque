package br.com.torque.auth.dto;

import br.com.torque.auth.Perfil;

public record LoginResponse(String token, String nome, Perfil perfil) {
}
