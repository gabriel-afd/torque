package br.com.torque.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.jwt.JwtService;
import br.com.torque.shared.IntegracaoTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarios;

    private ResultActions login(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}"));
    }

    @Test
    void deveAutenticarComCredenciaisValidasEDevolverTokenComPerfil() throws Exception {
        criarUsuario("Ana Atendente", "ana@oficina.teste", Perfil.ATENDENTE);

        String corpo = login("Ana@Oficina.teste", SENHA_PADRAO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Atendente"))
                .andExpect(jsonPath("$.perfil").value("ATENDENTE"))
                .andReturn().getResponse().getContentAsString();

        String token = json.readTree(corpo).get("token").asText();
        Claims claims = jwtService.validar(token).orElseThrow();
        assertThat(claims.get(JwtService.CLAIM_PERFIL)).isEqualTo("ATENDENTE");
        assertThat(claims.getExpiration()).isInTheFuture();

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarSenhaIncorretaCom401() throws Exception {
        criarUsuario("Ana Atendente", "ana@oficina.teste", Perfil.ATENDENTE);

        login("ana@oficina.teste", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void deveRecusarEmailInexistenteComAMesmaMensagem() throws Exception {
        login("ninguem@oficina.teste", SENHA_PADRAO)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void deveRecusarUsuarioInativoComAMesmaMensagem() throws Exception {
        Usuario usuario = criarUsuario("Mário Mecânico", "mario@oficina.teste", Perfil.MECANICO);
        usuario.inativar();
        usuarios.saveAndFlush(usuario);

        login("mario@oficina.teste", SENHA_PADRAO)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void deveRecusarTokenDeUsuarioInativadoDepoisDoLogin() throws Exception {
        Usuario usuario = criarUsuario(Perfil.ADMINISTRADOR);
        String token = bearer(usuario);
        usuario.inativar();
        usuarios.saveAndFlush(usuario);

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExigirTokenNasRotasProtegidas() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Unauthorized"))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void deveRecusarTokenComPerfilAdulteradoNoPayload() throws Exception {
        String[] partes = bearer(Perfil.MECANICO).substring("Bearer ".length()).split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(partes[1]), StandardCharsets.UTF_8)
                .replace("MECANICO", "ADMINISTRADOR");
        String forjado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveDeixarDocumentacaoDaApiPublica() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/api/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void deveValidarCorpoDoLogin() throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        JsonNode campos = json.readTree(corpo).get("campos");
        assertThat(campos.has("email")).isTrue();
        assertThat(campos.has("senha")).isTrue();
    }
}
