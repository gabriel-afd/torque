package br.com.torque.shared.excecao;

/**
 * O usuário não tem permissão para a operação de domínio (HTTP 403), ex.: mecânico alterando
 * OS que não está atribuída a ele (RN09). Não depende do Spring Security, para o domínio
 * continuar testável sem framework.
 */
public class OperacaoNaoPermitidaException extends RuntimeException {

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
