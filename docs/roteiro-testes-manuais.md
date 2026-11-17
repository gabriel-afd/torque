# Roteiro dos testes manuais (CT53 a CT57)

Execute na ordem e anote o resultado em cada linha **Resultado** (Aprovado / Reprovado + observação). Depois copie os resultados para o `TestCasePlan.md` (ou me passe para eu preencher).

## Preparação (uma vez)

1. Banco **vazio** (ou um banco novo só para o teste): crie o banco `torque_demo` no PostgreSQL local.
2. Suba a API com o perfil demo (dados fictícios: atendente, 2 mecânicos, 10 clientes, 15 veículos, OS em todos os status):
   - No IntelliJ, em *Run Configuration → Environment variables*, acrescente `SPRING_PROFILES_ACTIVE=demo` e aponte `DB_URL` para `jdbc:postgresql://localhost:5432/torque_demo`. Mantenha `JWT_SECRET`, `ADMIN_EMAIL` e `ADMIN_SENHA`.
   - No log deve aparecer: `Perfil demo: ... carregados.`
3. Suba o frontend: `cd torque-web && npm start` e abra http://localhost:4200.
4. Usuários (senha `demo12345`): `ana.demo@oficina.teste` (atendente), `raimundo.demo@oficina.teste` e `tiago.demo@oficina.teste` (mecânicos). Administrador: o seu `ADMIN_EMAIL`.

---

## CT53 — Fluxo completo (todas as HUs)

| # | Passo | Esperado | Resultado |
| --- | --- | --- | --- |
| 1 | Entrar como **Ana (atendente)** | Abre o Quadro de OS com cartões em todas as colunas | |
| 2 | Clientes → **Novo cliente**: nome fictício, CPF válido (ex.: 529.982.247-25), telefone | Cliente salvo e detalhe aberto | |
| 3 | No detalhe do cliente → **Novo veículo**: placa nova (ex.: TST1A23), marca, modelo | Veículo aparece na lista do cliente | |
| 4 | Quadro → **+ Nova OS** → buscar a placa TST1A23 → problema "Teste manual" e km → **Abrir OS** | Abre a tela da OS, status *Aberta* | |
| 5 | Adicionar 1 serviço e 1 peça (qtd 2) pela busca do catálogo; dar desconto de 5% | Subtotais e total mudam na hora; **Salvar orçamento** | |
| 6 | Tentar desconto de 15% (atendente) e salvar | Aviso do limite de 10% e a API recusa com mensagem (RN05) | |
| 7 | **Enviar para aprovação** | Status *Aguardando aprovação*; histórico com a mudança | |
| 8 | **Compartilhar (PDF / WhatsApp)** | Baixa `orcamento-os-....pdf` e abre o WhatsApp Web com o resumo e o total | |
| 9 | **Registrar aprovação** | Status *Aprovada* | |
| 10 | Sair e entrar como **administrador** → abrir a OS → atribuir **Raimundo** | Mecânico aparece no cabeçalho | |
| 11 | Sair e entrar como **Raimundo** → abrir a OS pelo quadro → **Iniciar serviço** | Status *Em execução* | |
| 12 | **Aguardando peça** e depois **Retomar serviço** | Status vai para *Aguardando peça* e volta a *Em execução* | |
| 13 | **Concluir** | Status *Concluída* | |
| 14 | Sair e entrar como **Ana** → abrir a OS → **Entregar** | Status *Entregue*; nenhum botão de ação | |
| 15 | Clicar na placa no cabeçalho da OS (histórico do veículo) | A OS aparece no topo da lista, com data, km, serviços e valor | |

## CT54 — Uso no celular (360 px)

No Chrome: F12 → ícone de dispositivo (Ctrl+Shift+M) → largura **360**. Em cada tela, conferir que **não há rolagem horizontal da página** e que dá para ler e tocar nos botões.

| Tela | O que conferir | Resultado |
| --- | --- | --- |
| Login | Campos e botão ocupam a largura, sem cortar | |
| Quadro de OS | Colunas rolam de lado **dentro** do quadro; a página não rola de lado; menu vira ☰ | |
| Tela da OS | Resumo fica abaixo dos itens; tabelas rolam dentro do cartão; botões cabem | |
| Histórico do veículo (Veículos → buscar placa → Histórico) | Cartões legíveis; botão "+ Nova OS" flutuante não cobre o conteúdo final | |

## CT55 — Abrir OS rapidamente (no máximo 3 telas)

| # | Passo | Resultado |
| --- | --- | --- |
| 1 | Entrar como Ana, no Quadro clicar **+ Nova OS** (tela 1: quadro; diálogo) | |
| 2 | Digitar a placa de um veículo da demo (ex.: a do cartão de qualquer OS) → Buscar → problema → **Abrir OS** | |
| 3 | Contar as telas até ver a OS aberta (esperado: quadro → diálogo → OS = 3) | Telas: ___ |

## CT56 — Desempenho da listagem (já executado por script)

Executado em 2026-11-16 com `scripts/ct56/gerar-10mil-os.sql` e `scripts/ct56/medir-listagem.sh` (10.017 OS): médias de 0,022 a 0,024 s, máximo de 0,031 s, meta de 2 s. Para repetir no seu banco local: rode o SQL com `psql`, depois `EMAIL=... SENHA=... ./medir-listagem.sh http://localhost:8080`, e no fim `limpar-ct56.sql`.

| Resultado (opcional, repetição) | |
| --- | --- |

## CT57 — Dados pessoais

| # | Passo | Esperado | Resultado |
| --- | --- | --- | --- |
| 1 | DevTools → aba **Network**: fazer login e abrir Usuários (admin) | Nenhuma resposta traz `senha` ou hash | |
| 2 | Como **mecânico**, abrir uma OS e ver a resposta de `/api/ordens/{id}` | Cliente sem CPF/CNPJ, telefone e e-mail | |
| 3 | Ler o console/log da API depois do fluxo do CT53 | Nenhuma senha, token (`eyJ...`) ou CPF no log | |
| 4 | Erro proposital: cadastrar cliente com CPF já existente | Mensagem 409 sem o CPF no log nem stack trace na resposta | |
