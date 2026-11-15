package br.com.torque.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CatalogoDominioTest {

    @Test
    void deveArredondarPrecoParaDuasCasasComHalfEven() {
        assertThat(new Servico("Revisão", new BigDecimal("10.125")).getPrecoMaoDeObra()).isEqualByComparingTo("10.12");
        assertThat(new Servico("Revisão", new BigDecimal("10.135")).getPrecoMaoDeObra()).isEqualByComparingTo("10.14");
        assertThat(new Peca("x", "Filtro", new BigDecimal("7")).getPrecoVenda()).hasToString("7.00");
    }

    @Test
    void deveRecusarPrecoNegativoNoDominio() {
        assertThatThrownBy(() -> new Servico("Revisão", new BigDecimal("-0.01")))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> new Peca("F1", "Filtro", new BigDecimal("-1")))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> new Peca("F1", "Filtro", null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveIncluirInativosSoParaAdministradorQuePedir() {
        var admin = new UsuarioAutenticado(1L, "A", "a@t", Perfil.ADMINISTRADOR);
        var atendente = new UsuarioAutenticado(2L, "B", "b@t", Perfil.ATENDENTE);

        assertThat(CatalogoService.deveIncluirInativos(true, admin)).isTrue();
        assertThat(CatalogoService.deveIncluirInativos(false, admin)).isFalse();
        assertThat(CatalogoService.deveIncluirInativos(true, atendente)).isFalse();
        assertThat(CatalogoService.deveIncluirInativos(true, null)).isFalse();
    }
}
