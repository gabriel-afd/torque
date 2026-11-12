package br.com.torque.auth;

import br.com.torque.auth.dto.UsuarioAtualizacaoRequest;
import br.com.torque.auth.dto.UsuarioRequest;
import br.com.torque.auth.dto.UsuarioResponse;
import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    static final String MENSAGEM_EMAIL_DUPLICADO = "Já existe um usuário com este e-mail";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarios.findAll(pageable).map(UsuarioResponse::de);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(carregar(id));
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioRequest request) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarios.existsByEmail(email)) {
            throw new ConflitoException(MENSAGEM_EMAIL_DUPLICADO);
        }
        Usuario usuario = new Usuario(request.nome().trim(), email,
                passwordEncoder.encode(request.senha()), request.perfil());
        return UsuarioResponse.de(usuarios.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
        Usuario usuario = carregar(id);
        String email = Usuario.normalizarEmail(request.email());
        if (usuarios.existsByEmailAndIdNot(email, id)) {
            throw new ConflitoException(MENSAGEM_EMAIL_DUPLICADO);
        }
        if (usuario.isAdministrador() && usuario.isAtivo() && request.perfil() != Perfil.ADMINISTRADOR) {
            garantirOutroAdministradorAtivo();
        }
        usuario.atualizar(request.nome().trim(), email, request.perfil());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.alterarSenha(passwordEncoder.encode(request.senha()));
        }
        return UsuarioResponse.de(usuarios.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioResponse inativar(Long id) {
        Usuario usuario = carregar(id);
        if (usuario.isAdministrador() && usuario.isAtivo()) {
            garantirOutroAdministradorAtivo();
        }
        usuario.inativar();
        return UsuarioResponse.de(usuario);
    }

    private void garantirOutroAdministradorAtivo() {
        if (usuarios.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR) <= 1) {
            throw new RegraDeNegocioException("O sistema precisa de pelo menos um administrador ativo");
        }
    }

    private Usuario carregar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }
}
