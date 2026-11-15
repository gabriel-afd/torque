package br.com.torque.catalogo.dto;

import br.com.torque.catalogo.Servico;
import java.math.BigDecimal;

public record ServicoResponse(Long id, String descricao, BigDecimal precoMaoDeObra, boolean ativo) {

    public static ServicoResponse de(Servico servico) {
        return new ServicoResponse(servico.getId(), servico.getDescricao(), servico.getPrecoMaoDeObra(),
                servico.isAtivo());
    }
}
