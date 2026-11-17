package br.com.torque.auth;

import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByPerfilAndAtivoTrue(Perfil perfil);

    /** Usuário pelo id ou 404. */
    default Usuario buscarOuFalhar(Long id) {
        return findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }
}
