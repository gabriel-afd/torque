package br.com.torque.shared.demo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.torque.shared.IntegracaoTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Sem o perfil demo, o carregador nem existe: dados fictícios nunca vão para outro ambiente. */
class DadosDemoForaDoPerfilIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void naoDeveExistirCarregadorDeDadosDemoForaDoPerfilDemo() {
        assertThat(contexto.getBeansOfType(DadosDemo.class)).isEmpty();
    }
}
