package br.com.torque.ordens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** CT45 (PDF do orçamento) e CT46 (link do WhatsApp) pela API. */
class OrcamentoDocumentosIntegracaoTest extends OrdemServicoApiTestBase {

    private ResultActions pdf(long os, int versao, String token) throws Exception {
        return mvc.perform(get("/api/ordens/" + os + "/orcamentos/" + versao + "/pdf")
                .header(HttpHeaders.AUTHORIZATION, token).accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON));
    }

    private ResultActions compartilhamento(long os, String token) throws Exception {
        return mvc.perform(get("/api/ordens/" + os + "/compartilhamento").header(HttpHeaders.AUTHORIZATION, token));
    }

    private long numero(long os) throws Exception {
        return corpo(detalhar(os, tokenAtendente)).get("numero").asLong();
    }

    // CT45 ------------------------------------------------------------------------

    @Test
    void deveGerarPdfDoOrcamentoComNumeroDaOsPlacaETotal() throws Exception {
        long os = osAprovadaComMecanico();

        byte[] arquivo = pdf(os, 1, tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"orcamento-os-" + numero(os) + "-v1.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        String texto = OrcamentoPdfTest.texto(arquivo);
        assertThat(texto)
                .contains("Oficina Torque")
                .contains("OS nº " + numero(os))
                .contains("Joana Fictícia")
                .contains("Placa: ABC1D23")
                .contains("Total R$ 190,00")
                .contains("Situação do orçamento: Aprovado");
    }

    @Test
    void devePermitirPdfAoAdministrador() throws Exception {
        long os = abrirOs();

        pdf(os, 1, tokenAdmin).andExpect(status().isOk());
    }

    @Test
    void deveNegarPdfAoMecanico() throws Exception {
        long os = osAprovadaComMecanico();

        pdf(os, 1, tokenMecanico).andExpect(status().isForbidden());
    }

    @Test
    void deveExigirLoginParaPdf() throws Exception {
        long os = abrirOs();

        mvc.perform(get("/api/ordens/" + os + "/orcamentos/1/pdf")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveResponder404ParaVersaoOuOsInexistente() throws Exception {
        long os = abrirOs();

        pdf(os, 9, tokenAtendente).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("versão 9")));
        pdf(999_999, 1, tokenAtendente).andExpect(status().isNotFound());
    }

    // CT46 ------------------------------------------------------------------------

    @Test
    void deveGerarLinkDoWhatsAppComTelefoneDoClienteETotal() throws Exception {
        long os = osAprovadaComMecanico();

        String link = corpo(compartilhamento(os, tokenAtendente).andExpect(status().isOk())).get("linkWhatsapp").asText();

        assertThat(link).startsWith("https://wa.me/5583999990001?text=");
        String texto = URLDecoder.decode(URI.create(link).getRawQuery().substring("text=".length()),
                StandardCharsets.UTF_8);
        assertThat(texto).contains("OS nº " + numero(os)).contains("ABC1D23").contains("Total: R$ 190,00");
    }

    @Test
    void deveNegarCompartilhamentoAoMecanico() throws Exception {
        long os = osAprovadaComMecanico();

        compartilhamento(os, tokenMecanico).andExpect(status().isForbidden());
    }

    @Test
    void deveResponder404AoCompartilharOsInexistente() throws Exception {
        compartilhamento(999_999, tokenAtendente).andExpect(status().isNotFound());
    }

    @Test
    void deveRecusarCompartilhamentoDeClienteComTelefoneInvalido() throws Exception {
        long os = abrirOs();
        jdbc.update("update cliente set telefone = '12345'"); // dado legado sem DDD

        compartilhamento(os, tokenAtendente).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("telefone")));
    }
}
