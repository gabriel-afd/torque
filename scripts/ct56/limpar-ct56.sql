-- Remove as OS geradas por gerar-10mil-os.sql.
BEGIN;
DELETE FROM historico_status WHERE ordem_servico_id IN (SELECT id FROM ordem_servico WHERE problema_relatado LIKE 'Carga CT56 #%');
DELETE FROM orcamento WHERE ordem_servico_id IN (SELECT id FROM ordem_servico WHERE problema_relatado LIKE 'Carga CT56 #%');
DELETE FROM ordem_servico WHERE problema_relatado LIKE 'Carga CT56 #%';
COMMIT;
