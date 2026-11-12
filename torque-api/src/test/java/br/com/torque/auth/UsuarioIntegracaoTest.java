package br.com.torque.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.shared.IntegracaoTestBase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario admin;
    private String tokenAdmin;

    @BeforeEach
    void criarAdministrador() {
        admin = criarUsuario("Admin Fictício", "admin@oficina.teste", Perfil.ADMINISTRADOR);
        tokenAdmin = bearer(admin);
    }

    private String novoUsuarioJson(String email) throws Exception {
        return paraJson(Map.of("nome", "Carla Atendente", "email", email,
                "senha", "senhaSegura123", "perfil", "ATENDENTE"));
    }

    @Test
    void deveRecusarMecanicoNaRotaDeUsuariosCom403() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deveRecusarAtendenteAoCadastrarUsuarioCom403() throws Exception {
        mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.ATENDENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(novoUsuarioJson("carla@oficina.teste")))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = 'carla@oficina.teste'",
                Integer.class)).isZero();
    }

    @Test
    void deveGravarSenhaComBcryptENuncaDevolverASenha() throws Exception {
        String resposta = mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(novoUsuarioJson("Carla@Oficina.teste")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/usuarios/")))
                .andExpect(jsonPath("$.email").value("carla@oficina.teste"))
                .andExpect(jsonPath("$.perfil").value("ATENDENTE"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(content().string(not(containsString("senhaSegura123"))))
                .andReturn().getResponse().getContentAsString();

        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE email = 'carla@oficina.teste'",
                String.class);
        assertThat(hash).startsWith("$2").isNotEqualTo("senhaSegura123");
        assertThat(passwordEncoder.matches("senhaSegura123", hash)).isTrue();
        assertThat(resposta).doesNotContain(hash);

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("$2"))))
                .andExpect(jsonPath("$.content[0].senhaHash").doesNotExist());
    }

    @Test
    void deveListarUsuariosPaginados() throws Exception {
        criarUsuario(Perfil.ATENDENTE);
        criarUsuario(Perfil.MECANICO);

        mvc.perform(get("/api/usuarios?size=2&page=0").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void deveRecusarEmailDuplicadoCom409() throws Exception {
        criarUsuario("Carla", "carla@oficina.teste", Perfil.ATENDENTE);

        mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(novoUsuarioJson("CARLA@oficina.teste")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deveValidarCadastroCom400() throws Exception {
        mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paraJson(Map.of("nome", "", "email", "invalido", "senha", "curta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists())
                .andExpect(jsonPath("$.campos.perfil").exists());
    }

    @Test
    void deveEditarUsuarioMantendoSenhaQuandoNaoInformada() throws Exception {
        Usuario carla = criarUsuario("Carla", "carla@oficina.teste", Perfil.ATENDENTE);
        String hashAntes = jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE id = ?", String.class, carla.getId());

        mvc.perform(put("/api/usuarios/" + carla.getId()).header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paraJson(Map.of("nome", "Carla Souza", "email", "carla@oficina.teste",
                                "perfil", "MECANICO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carla Souza"))
                .andExpect(jsonPath("$.perfil").value("MECANICO"));

        assertThat(jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE id = ?", String.class, carla.getId()))
                .isEqualTo(hashAntes);
    }

    @Test
    void deveInativarUsuario() throws Exception {
        Usuario carla = criarUsuario("Carla", "carla@oficina.teste", Perfil.ATENDENTE);

        mvc.perform(patch("/api/usuarios/" + carla.getId() + "/inativar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        assertThat(jdbc.queryForObject("SELECT ativo FROM usuario WHERE id = ?", Boolean.class, carla.getId()))
                .isFalse();
    }

    @Test
    void deveImpedirInativarOUltimoAdministradorAtivo() throws Exception {
        mvc.perform(patch("/api/usuarios/" + admin.getId() + "/inativar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveDevolver404ParaUsuarioInexistente() throws Exception {
        mvc.perform(get("/api/usuarios/999999").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isNotFound());
    }
}
