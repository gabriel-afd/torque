package br.com.torque.ordens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * Auditoria A02: o MECANICO não precisa de CPF/CNPJ, telefone e e-mail do cliente (minimização de dados,
 * LGPD). Ele continua vendo o nome; atendente e administrador veem tudo.
 */
class DadosPessoaisMecanicoIntegracaoTest extends OrdemServicoApiTestBase {

    @Test
    void naoDeveExporDocumentoTelefoneEEmailDoClienteAoMecanicoNoDetalheDaOs() throws Exception {
        long os = osAprovadaComMecanico();

        detalhar(os, tokenMecanico)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nome").value("Joana Fictícia"))
                .andExpect(jsonPath("$.cliente.documento").doesNotExist())
                .andExpect(jsonPath("$.cliente.telefone").doesNotExist())
                .andExpect(jsonPath("$.cliente.email").doesNotExist());
    }

    @Test
    void deveExporDadosDoClienteAoAtendenteNoDetalheDaOs() throws Exception {
        long os = osAprovadaComMecanico();

        detalhar(os, tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.documento").value("52998224725"))
                .andExpect(jsonPath("$.cliente.telefone").value("83999990001"));
    }

    @Test
    void naoDeveExporDocumentoTelefoneEEmailDoDonoAoMecanicoNaBuscaPorPlaca() throws Exception {
        mvc.perform(get("/api/veiculos?placa=abc1d23").header(HttpHeaders.AUTHORIZATION, tokenMecanico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dono.nome").value("Joana Fictícia"))
                .andExpect(jsonPath("$.dono.documento").doesNotExist())
                .andExpect(jsonPath("$.dono.telefone").doesNotExist())
                .andExpect(jsonPath("$.dono.email").doesNotExist());
    }

    @Test
    void deveExporDadosDoDonoAoAtendenteNaBuscaPorPlaca() throws Exception {
        mvc.perform(get("/api/veiculos?placa=ABC1D23").header(HttpHeaders.AUTHORIZATION, tokenAtendente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dono.documento").value("52998224725"));
    }
}
