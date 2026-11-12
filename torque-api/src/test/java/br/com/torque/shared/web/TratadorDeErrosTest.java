package br.com.torque.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import br.com.torque.shared.validacao.CpfCnpj;
import br.com.torque.shared.validacao.Placa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class TratadorDeErrosTest {

    record ClienteFicticioRequest(@NotBlank String nome, @NotBlank @CpfCnpj String documento, @Placa String placa) {
    }

    @RestController
    static class ControllerDeTeste {

        @PostMapping("/teste/validacao")
        void validacao(@Valid @RequestBody ClienteFicticioRequest request) {
        }

        @GetMapping("/teste/{tipo}")
        void lancar(@PathVariable String tipo) {
            switch (tipo) {
                case "nao-autenticado" -> throw new BadCredentialsException("credencial");
                case "sem-permissao" -> throw new AccessDeniedException("negado");
                case "nao-encontrado" -> throw new RecursoNaoEncontradoException("Cliente não encontrado: 42");
                case "conflito" -> throw new ConflitoException("Documento já cadastrado");
                case "integridade" -> throw new DataIntegrityViolationException("uk_cliente_documento");
                case "regra" -> throw new RegraDeNegocioException("Desconto acima do permitido");
                default -> throw new IllegalStateException("detalhe interno: senha=segredo123 em TabelaSecreta");
            }
        }

        @GetMapping("/teste/numero/{id}")
        void numero(@PathVariable Long id) {
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    void deveDevolver400ComErrosPorCampo() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "documento": "111.111.111-12", "placa": "AB-1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Bad Request"))
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.MENSAGEM_VALIDACAO))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.documento").value("CPF ou CNPJ inválido"))
                .andExpect(jsonPath("$.campos.placa").value("Placa inválida (use ABC1234 ou ABC1D23)"));
    }

    @Test
    void deveDevolver400ParaJsonMalFormado() throws Exception {
        mvc.perform(post("/teste/validacao").contentType(MediaType.APPLICATION_JSON).content("{ quebrado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isMap());
    }

    @Test
    void deveDevolver400ParaParametroDeTipoErrado() throws Exception {
        mvc.perform(get("/teste/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.id").value("Valor inválido"));
    }

    @Test
    void deveDevolver401() throws Exception {
        mvc.perform(get("/teste/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.MENSAGEM_NAO_AUTENTICADO));
    }

    @Test
    void deveDevolver403() throws Exception {
        mvc.perform(get("/teste/sem-permissao"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Forbidden"));
    }

    @Test
    void deveDevolver404ComMensagemDaExcecao() throws Exception {
        mvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado: 42"))
                .andExpect(jsonPath("$.campos").isEmpty());
    }

    @Test
    void deveDevolver409ParaConflitoEViolacaoDeIntegridade() throws Exception {
        mvc.perform(get("/teste/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Documento já cadastrado"));
        mvc.perform(get("/teste/integridade"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(not(containsString("uk_cliente_documento"))));
    }

    @Test
    void deveDevolver422ParaRegraDeNegocio() throws Exception {
        mvc.perform(get("/teste/regra"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.mensagem").value("Desconto acima do permitido"));
    }

    @Test
    void deveDevolver500SemVazarDetalhesInternos() throws Exception {
        mvc.perform(get("/teste/qualquer-outro"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.MENSAGEM_ERRO_INTERNO))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("segredo123"))))
                .andExpect(content().string(not(containsString("TabelaSecreta"))))
                .andExpect(content().string(not(containsString("at br.com"))))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
