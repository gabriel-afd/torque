package br.com.torque.ordens;

import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import br.com.torque.shared.Busca;
import br.com.torque.shared.validacao.PlacaUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filtros do quadro de OS: status, mecânico e busca por placa ou nome do cliente. */
final class FiltrosOrdem {

    private FiltrosOrdem() {
    }

    static Specification<OrdemServico> de(Collection<StatusOS> status, Long mecanicoId, String busca) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (status != null && !status.isEmpty()) {
                filtros.add(root.get("status").in(status));
            }
            if (mecanicoId != null) {
                filtros.add(cb.equal(root.get("mecanico").get("id"), mecanicoId));
            }
            if (!Busca.vazia(busca)) {
                Join<OrdemServico, Veiculo> veiculo = root.join("veiculo");
                Join<Veiculo, Cliente> cliente = veiculo.join("cliente");
                String padraoPlaca = Busca.contem(PlacaUtil.normalizar(busca)).toUpperCase(Locale.ROOT);
                filtros.add(cb.or(
                        cb.like(veiculo.get("placa"), padraoPlaca, '\\'),
                        cb.like(cb.lower(cliente.get("nome")), Busca.contem(busca), '\\')));
            }
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }
}
