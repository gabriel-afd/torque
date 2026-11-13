package br.com.torque.ordens.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estado desejado do orçamento. Itens já existentes são referenciados por {@code itemId} (mantêm
 * descrição e preço congelados — RN10); itens novos, por {@code servicoId}/{@code pecaId} (copiados do
 * catálogo agora). Itens existentes não enviados são removidos.
 *
 * @param versaoOs versão da OS que o cliente carregou (opcional); diferente da atual → 409
 */
public record OrcamentoRequest(
        @NotNull @Valid List<ItemServico> servicos,
        @NotNull @Valid List<ItemPeca> pecas,
        @NotNull @DecimalMin(value = "0.00", message = "O desconto não pode ser negativo")
        @Digits(integer = 8, fraction = 2) BigDecimal desconto,
        Integer versaoOs) {

    public record ItemServico(Long itemId, Long servicoId) {

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "Informe itemId (item existente) ou servicoId (item novo), não os dois")
        public boolean isReferenciaUnica() {
            return (itemId == null) != (servicoId == null);
        }
    }

    public record ItemPeca(Long itemId, Long pecaId,
                           @NotNull @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade) {

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "Informe itemId (item existente) ou pecaId (item novo), não os dois")
        public boolean isReferenciaUnica() {
            return (itemId == null) != (pecaId == null);
        }
    }
}
