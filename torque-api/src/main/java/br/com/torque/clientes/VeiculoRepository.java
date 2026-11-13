package br.com.torque.clientes;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    boolean existsByPlacaAndIdNot(String placa, Long id);

    /** Veículo com o dono carregado na mesma consulta. */
    @Query("select v from Veiculo v join fetch v.cliente where v.placa = :placa")
    Optional<Veiculo> buscarPorPlacaComDono(String placa);

    @Query("select v from Veiculo v join fetch v.cliente where v.id = :id")
    Optional<Veiculo> buscarComDono(Long id);

    List<Veiculo> findByClienteIdOrderByPlaca(Long clienteId);
}
