package br.com.torque.catalogo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PecaRepository extends JpaRepository<Peca, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /** Busca por descrição ou código. {@code padrao} já vem em minúsculas e com curingas escapados. */
    @Query("""
            select p from Peca p
            where (lower(p.descricao) like :padrao escape '\\' or lower(p.codigo) like :padrao escape '\\')
              and (p.ativo = true or :incluirInativos = true)
            """)
    Page<Peca> buscar(String padrao, boolean incluirInativos, Pageable pageable);
}
