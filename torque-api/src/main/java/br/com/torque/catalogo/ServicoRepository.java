package br.com.torque.catalogo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    /** {@code padrao} já vem em minúsculas e com curingas escapados. */
    @Query("""
            select s from Servico s
            where lower(s.descricao) like :padrao escape '\\'
              and (s.ativo = true or :incluirInativos = true)
            """)
    Page<Servico> buscar(String padrao, boolean incluirInativos, Pageable pageable);
}
