package br.com.torque.shared.web;

import java.util.Map;

/**
 * Formato único de erro da API.
 *
 * @param status   código HTTP
 * @param erro     descrição curta do código HTTP (ex.: "Not Found")
 * @param mensagem mensagem para o usuário
 * @param campos   erros por campo (vazio quando não se aplica)
 */
public record ErroResponse(int status, String erro, String mensagem, Map<String, String> campos) {

    public ErroResponse {
        campos = campos == null ? Map.of() : Map.copyOf(campos);
    }
}
