package br.com.torque.catalogo.dto;

import br.com.torque.catalogo.Peca;
import java.math.BigDecimal;

public record PecaResponse(Long id, String codigo, String descricao, BigDecimal precoVenda, boolean ativo) {

    public static PecaResponse de(Peca peca) {
        return new PecaResponse(peca.getId(), peca.getCodigo(), peca.getDescricao(), peca.getPrecoVenda(),
                peca.isAtivo());
    }
}
