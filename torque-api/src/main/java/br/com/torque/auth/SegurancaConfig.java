package br.com.torque.auth;

import br.com.torque.auth.jwt.JwtAuthenticationFilter;
import br.com.torque.auth.jwt.JwtProperties;
import br.com.torque.auth.jwt.JwtService;
import br.com.torque.shared.web.ErroResponse;
import br.com.torque.shared.web.TratadorDeErros;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Segurança stateless com JWT. Autorização fina por {@code @PreAuthorize} em cada endpoint. */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SegurancaConfig {

    private static final String[] ROTAS_PUBLICAS_GET = {
            "/api/docs", "/api/docs/**", "/api/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
            "/swagger-ui/**", "/swagger-ui.html"
    };

    private final ObjectMapper objectMapper;

    public SegurancaConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JwtService jwtService, UsuarioRepository usuarios) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, ROTAS_PUBLICAS_GET).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, resp, ex) ->
                                escrever(resp, HttpStatus.UNAUTHORIZED, TratadorDeErros.MENSAGEM_NAO_AUTENTICADO))
                        .accessDeniedHandler((req, resp, ex) ->
                                escrever(resp, HttpStatus.FORBIDDEN, TratadorDeErros.MENSAGEM_SEM_PERMISSAO)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarios),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void escrever(HttpServletResponse resposta, HttpStatus status, String mensagem) throws IOException {
        ErroResponse corpo = TratadorDeErros.corpo(status, mensagem, Map.of());
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resposta.getOutputStream(), corpo);
    }
}
