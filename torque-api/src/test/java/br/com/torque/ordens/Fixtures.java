package br.com.torque.ordens;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.catalogo.Peca;
import br.com.torque.catalogo.Servico;
import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/** Objetos de domínio fictícios para testes unitários (sem Spring, sem banco). */
final class Fixtures {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1000);

    private Fixtures() {
    }

    static Usuario usuario(Perfil perfil) {
        long id = SEQUENCIA.incrementAndGet();
        Usuario usuario = new Usuario(perfil.name() + " " + id, "u" + id + "@oficina.teste", "hash-ficticio", perfil);
        return comId(usuario, id);
    }

    static Usuario atendente() {
        return usuario(Perfil.ATENDENTE);
    }

    static Usuario administrador() {
        return usuario(Perfil.ADMINISTRADOR);
    }

    static Usuario mecanico() {
        return usuario(Perfil.MECANICO);
    }

    static Veiculo veiculo() {
        Cliente cliente = new Cliente("Cliente Fictício", "52998224725", "83999990001", null);
        return cliente.novoVeiculo("ABC1D23", "Fiat", "Uno", 2015, "Branco");
    }

    static Servico servico(String descricao, String preco) {
        return comId(new Servico(descricao, new BigDecimal(preco)), SEQUENCIA.incrementAndGet());
    }

    static Peca peca(String descricao, String preco) {
        long id = SEQUENCIA.incrementAndGet();
        return comId(new Peca("P-" + id, descricao, new BigDecimal(preco)), id);
    }

    static OrdemServico osAberta() {
        return OrdemServico.abrir(veiculo(), "Barulho na suspensão", 85_000, atendente());
    }

    /** Simula o id que o banco atribuiria. */
    static <T> T comId(T entidade, Long id) {
        try {
            Field campo = entidade.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidade, id);
            return entidade;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Atribui ids aos itens do orçamento, como se tivessem sido gravados. */
    static void gravarItens(Orcamento orcamento) {
        orcamento.getItensServico().forEach(item -> {
            if (item.getId() == null) {
                comId(item, SEQUENCIA.incrementAndGet());
            }
        });
        orcamento.getItensPeca().forEach(item -> {
            if (item.getId() == null) {
                comId(item, SEQUENCIA.incrementAndGet());
            }
        });
    }
}
