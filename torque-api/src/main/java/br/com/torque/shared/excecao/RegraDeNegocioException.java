package br.com.torque.shared.excecao;

/** Violação de regra de negócio (HTTP 422). */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
