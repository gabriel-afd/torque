package br.com.torque.clientes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, Long id);

    @Query("""
            select c from Cliente c
            where lower(c.nome) like :padraoNome escape '\\'
               or c.documento like :padraoDocumento
            """)
    Page<Cliente> buscar(@Param("padraoNome") String padraoNome,
                         @Param("padraoDocumento") String padraoDocumento,
                         Pageable pageable);
}
