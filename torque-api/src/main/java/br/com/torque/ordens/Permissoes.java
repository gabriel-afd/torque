package br.com.torque.ordens;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.shared.excecao.OperacaoNaoPermitidaException;
import java.util.Objects;

/** Verificações de perfil usadas pelas entidades do módulo de ordens. */
final class Permissoes {

    private Permissoes() {
    }

    /** ATENDENTE ou ADMINISTRADOR. */
    static void exigirBalcao(Usuario usuario, String acao) {
        Objects.requireNonNull(usuario, "Usuário obrigatório");
        if (usuario.getPerfil() != Perfil.ATENDENTE && usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new OperacaoNaoPermitidaException("Somente atendente ou administrador pode " + acao);
        }
    }
}
