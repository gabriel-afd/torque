package br.com.torque.ordens.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Link para enviar o resumo do orçamento vigente ao cliente pelo WhatsApp. */
public record CompartilhamentoResponse(
        @Schema(example = "https://wa.me/5583999990001?text=Ol%C3%A1%2C%20Joana%21...") String linkWhatsapp) {
}
