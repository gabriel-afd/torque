-- CT56 (RNF05): gera 10 mil OS fictícias no banco LOCAL para medir a listagem GET /api/ordens.
-- Pré-requisito: banco com veículos e usuários (ex.: API rodada uma vez com SPRING_PROFILES_ACTIVE=demo).
-- Uso: psql -h localhost -p 5432 -U torque -d torque -f gerar-10mil-os.sql
-- NUNCA rode em produção: as OS ficam com problema relatado "Carga CT56" e podem ser apagadas com limpar-ct56.sql.

BEGIN;

WITH veiculos AS (
    SELECT id, row_number() OVER (ORDER BY id) - 1 AS n, count(*) OVER () AS total FROM veiculo
), mecanicos AS (
    SELECT id, row_number() OVER (ORDER BY id) - 1 AS n, count(*) OVER () AS total FROM usuario WHERE perfil = 'MECANICO'
), status AS (
    SELECT s, row_number() OVER () - 1 AS n
    FROM unnest(ARRAY['ABERTA', 'AGUARDANDO_APROVACAO', 'APROVADA', 'EM_EXECUCAO', 'AGUARDANDO_PECA', 'CONCLUIDA',
                      'ENTREGUE', 'RECUSADA', 'CANCELADA']) AS s
), novas AS (
    INSERT INTO ordem_servico (veiculo_id, mecanico_id, problema_relatado, quilometragem, status, data_abertura, data_entrega)
    SELECT v.id,
           CASE WHEN st.s IN ('ABERTA', 'AGUARDANDO_APROVACAO', 'RECUSADA', 'CANCELADA') THEN NULL ELSE m.id END,
           'Carga CT56 #' || g,
           10000 + (g * 7) % 150000,
           st.s,
           now() - make_interval(mins => g * 13),
           CASE WHEN st.s = 'ENTREGUE' THEN now() - make_interval(mins => g * 13 - 60) END
    FROM generate_series(1, 10000) AS g
    JOIN veiculos v ON v.n = g % v.total
    JOIN status st ON st.n = g % 9
    LEFT JOIN mecanicos m ON m.n = g % greatest(m.total, 1)
    RETURNING id, status, data_abertura
), orcamentos AS (
    INSERT INTO orcamento (ordem_servico_id, versao, desconto, total, situacao)
    SELECT id, 1, 0, 100 + (id % 50) * 10,
           CASE WHEN status IN ('ABERTA', 'AGUARDANDO_APROVACAO', 'CANCELADA') THEN 'PENDENTE'
                WHEN status = 'RECUSADA' THEN 'RECUSADO' ELSE 'APROVADO' END
    FROM novas
    RETURNING ordem_servico_id
)
INSERT INTO historico_status (ordem_servico_id, usuario_id, status_anterior, status_novo, data_hora)
SELECT n.id, (SELECT min(id) FROM usuario), NULL, 'ABERTA', n.data_abertura FROM novas n;

COMMIT;

ANALYZE ordem_servico;
ANALYZE orcamento;
ANALYZE historico_status;

SELECT count(*) AS total_os FROM ordem_servico;
