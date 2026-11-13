package br.com.torque.shared.web;

import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.OperacaoNaoPermitidaException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte exceções no formato padrão {@link ErroResponse}. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    public static final String MENSAGEM_VALIDACAO = "Dados inválidos. Verifique os campos informados.";
    public static final String MENSAGEM_NAO_AUTENTICADO = "Autenticação necessária.";
    public static final String MENSAGEM_SEM_PERMISSAO = "Você não tem permissão para esta operação.";
    public static final String MENSAGEM_ERRO_INTERNO = "Erro interno. Tente novamente mais tarde.";

    public static ErroResponse corpo(HttpStatus status, String mensagem, Map<String, String> campos) {
        return new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, campos);
    }

    private static ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status).body(corpo(status, mensagem, campos));
    }

    private static ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
        return resposta(status, mensagem, Map.of());
    }

    // 400 ---------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_VALIDACAO, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErroResponse> validacaoParametros(HandlerMethodValidationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(resultado -> {
            String nome = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors()
                    .forEach(erro -> campos.putIfAbsent(nome, erro.getDefaultMessage()));
        });
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_VALIDACAO, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErroResponse> validacaoRestricoes(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violacao -> {
            String caminho = violacao.getPropertyPath().toString();
            String campo = caminho.substring(caminho.lastIndexOf('.') + 1);
            campos.putIfAbsent(campo, violacao.getMessage());
        });
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_VALIDACAO, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_VALIDACAO, Map.of(ex.getName(), "Valor inválido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException ex) {
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_VALIDACAO,
                Map.of(ex.getParameterName(), "Parâmetro obrigatório"));
    }

    // 401 / 403 ---------------------------------------------------------

    /** Falha de login: a mensagem é controlada pelo serviço de autenticação (genérica por segurança). */
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErroResponse> credenciaisInvalidas(BadCredentialsException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, MENSAGEM_NAO_AUTENTICADO);
    }

    /** Também cobre {@code AuthorizationDeniedException} (subclasse) lançada pelo {@code @PreAuthorize}. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErroResponse> semPermissao(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, MENSAGEM_SEM_PERMISSAO);
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    ResponseEntity<ErroResponse> operacaoNaoPermitida(OperacaoNaoPermitidaException ex) {
        return resposta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 404 / 405 ---------------------------------------------------------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado nesta rota.");
    }

    // 409 ---------------------------------------------------------------

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResponse> conflito(ConflitoException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Operação conflita com dados já cadastrados.");
    }

    // 422 ---------------------------------------------------------------

    @ExceptionHandler(RegraDeNegocioException.class)
    ResponseEntity<ErroResponse> regraDeNegocio(RegraDeNegocioException ex) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // 500 ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> erroInterno(Exception ex) {
        log.error("Erro não tratado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_ERRO_INTERNO);
    }
}
