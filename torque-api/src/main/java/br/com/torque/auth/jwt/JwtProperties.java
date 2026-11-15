package br.com.torque.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret         chave HMAC (variável JWT_SECRET), mínimo 32 bytes
 * @param expiracaoHoras validade do token em horas
 */
@ConfigurationProperties(prefix = "torque.jwt")
public record JwtProperties(String secret, int expiracaoHoras) {
}
