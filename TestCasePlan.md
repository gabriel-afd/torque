# Plano e Casos de Teste — Torque

Sistema de ordens de serviço para oficinas mecânicas de pequeno porte.
Projeto Integrador - EAD · Gabriel Medeiros de Mendonça · RA 2515050024

## 1. Objetivo

Verificar se o Torque atende às histórias de usuário (HU01–HU09), às regras de negócio (RN01–RN10) e aos requisitos não funcionais definidos na Macro Entrega 1, e registrar a execução dos testes, os defeitos encontrados e suas correções.

## 2. Escopo

| Dentro do escopo | Fora do escopo |
| --- | --- |
| Regras de domínio (status da OS, orçamento, desconto) | Emissão de nota fiscal e estoque (fora do MVP) |
| API REST: autenticação, autorização, validações, códigos HTTP | Integração oficial com WhatsApp (o MVP gera link) |
| Persistência no PostgreSQL e migrações Flyway | Testes de carga em escala de produção |
| Frontend: autenticação, rotas protegidas, cálculo do orçamento | Compatibilidade com navegadores antigos |
| Fluxo completo da OS (teste manual ponta a ponta) | |

## 3. Estratégia

| Nível | O que cobre | Ferramenta | Onde |
| --- | --- | --- | --- |
| Unitário de domínio | Máquina de estados, cálculo do orçamento, validadores | JUnit 5 | `torque-api/src/test/.../(modulo)` |
| Unitário de serviço | Casos de uso com repositórios simulados | JUnit 5 + Mockito | idem |
| Integração | Endpoints, segurança e banco real | Spring Boot Test + Testcontainers (PostgreSQL 16) | idem |
| Frontend | Guard, interceptor, componentes e serviços | Jasmine + Karma | `torque-web/src/app/**/*.spec.ts` |
| Manual (ponta a ponta) | Fluxo completo, responsividade e desempenho | Navegador + Swagger UI | roteiro na seção 5 |

Os testes automatizados rodam localmente (`mvn -B verify`, `npm test`) e no GitHub Actions a cada push e pull request.

## 4. Critérios

**Entrada:** funcionalidade implementada na branch, código compilando, migrações aplicadas sem erro.

**Aprovação de um caso:** o resultado obtido é igual ao esperado, sem erro não tratado no log.

**Saída (pronto para entrega):**
- 100% dos casos de prioridade **Alta** executados e aprovados.
- Nenhum defeito de severidade **Alta** em aberto.
- Pipeline de CI verde na `main`.
- Cobertura de linhas do pacote `ordens` ≥ 80% (relatório JaCoCo).

## 5. Casos de teste

Legenda de nível: **U** unitário · **I** integração · **F** frontend · **M** manual.
Status: Não executado · Aprovado · Reprovado · Bloqueado.

### 5.1 Autenticação e autorização (HU01)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT01 | HU01 | Login com credenciais válidas | POST /api/auth/login com e-mail e senha corretos | 200 com token JWT contendo o perfil | Alta | I | Não executado | |
| CT02 | HU01 | Senha incorreta | POST /api/auth/login com senha errada | 401 com mensagem "E-mail ou senha inválidos" | Alta | I | Não executado | |
| CT03 | HU01 | E-mail inexistente | POST /api/auth/login com e-mail não cadastrado | 401 com a mesma mensagem do CT02 | Alta | I | Não executado | |
| CT04 | HU01 | Usuário inativo | Login de usuário com ativo = false | 401 | Média | I | Não executado | |
| CT05 | RNF04 | Requisição sem token | GET /api/clientes sem Authorization | 401 | Alta | I | Não executado | |
| CT06 | RNF04 | Mecânico em rota de administrador | GET /api/usuarios com token de MECANICO | 403 | Alta | I | Não executado | |
| CT07 | RNF03 | Senha protegida | Cadastrar usuário e consultar banco e resposta | Senha gravada com hash BCrypt; nunca retornada pela API | Alta | I | Não executado | |

### 5.2 Clientes e veículos (HU02, HU03)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT08 | HU02, RN03 | Cadastro com CPF válido | Nome, telefone e CPF válido | 201; cliente encontrado na busca | Alta | I | Não executado | |
| CT09 | RN03 | CPF com dígito verificador inválido | CPF 111.111.111-12 | 400 com erro no campo documento | Alta | U, I | Não executado | |
| CT10 | RN03 | CPF duplicado | Cadastrar duas vezes o mesmo CPF | 409; nenhum registro duplicado | Alta | I | Não executado | |
| CT11 | RN03 | CNPJ válido | Cliente com CNPJ válido | 201 | Média | U | Não executado | |
| CT12 | HU03, RN02 | Placa no formato Mercosul | ABC1D23 | Aceita | Alta | U, I | Não executado | |
| CT13 | RN02 | Placa no formato antigo | ABC1234 | Aceita | Alta | U | Não executado | |
| CT14 | RN02 | Placa inválida | AB-1234, ABCD123 | 400 com erro no campo placa | Alta | U, I | Não executado | |
| CT15 | RN02 | Normalização da placa | abc-1d23 | Gravada como ABC1D23 | Média | U | Não executado | |
| CT16 | RN02 | Placa duplicada | Cadastrar a mesma placa duas vezes | 409 | Alta | I | Não executado | |
| CT17 | HU03 | Busca por placa | GET /api/veiculos?placa=ABC1D23 | 200 com veículo e dono; placa inexistente → 404 | Alta | I | Não executado | |
| CT18 | RN01 | Veículo sem cliente | POST de veículo para cliente inexistente | 404; nada gravado | Média | I | Não executado | |

### 5.3 Catálogo e orçamento (HU05)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT19 | RN04 | Cálculo do total | 1 serviço de R$ 120,00 + 2 peças de R$ 35,00 | Total R$ 190,00 | Alta | U | Não executado | |
| CT20 | RN04 | Total com desconto | Itens somando R$ 390,00, desconto R$ 19,50 | Total R$ 370,50 | Alta | U | Não executado | |
| CT21 | RN05 | Desconto acima de 10% por atendente | Desconto de 15% com perfil ATENDENTE | 422; orçamento inalterado | Alta | U, I | Não executado | |
| CT22 | RN05 | Desconto de exatamente 10% por atendente | Desconto de 10% | Aceito (valor de borda) | Alta | U | Não executado | |
| CT23 | RN05 | Desconto acima de 10% por administrador | Desconto de 15% com perfil ADMINISTRADOR | Aceito | Média | U | Não executado | |
| CT24 | RN10 | Preço congelado no item | Alterar preço no catálogo depois de incluir o item | Item e total da OS mantêm o valor original | Alta | I | Não executado | |
| CT25 | — | Quantidade inválida de peça | Quantidade 0 ou negativa | 400 | Média | I | Não executado | |
| CT26 | — | Item inativo no catálogo | Incluir serviço ou peça inativa | 422 | Baixa | U | Não executado | |

### 5.4 Ordem de serviço e fluxo de status (HU04, HU06, HU07)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT27 | HU04 | Abrir OS | Veículo existente, problema e quilometragem | 201; status ABERTA; número sequencial; histórico gravado | Alta | I | Não executado | |
| CT28 | HU04 | OS para veículo inexistente | veiculoId inválido | 404 | Média | I | Não executado | |
| CT29 | HU06 | Aprovar orçamento pendente | POST .../aprovacao | OS APROVADA; orçamento APROVADO com data e usuário | Alta | U, I | Não executado | |
| CT30 | RN07 | Aprovar orçamento já aprovado | Aprovar duas vezes | 422 na segunda chamada | Alta | U | Não executado | |
| CT31 | HU06 | Recusar orçamento | POST .../recusa | OS RECUSADA | Alta | U, I | Não executado | |
| CT32 | RN07 | Alterar orçamento aprovado | Incluir item em OS APROVADA | Nova versão PENDENTE; anterior SUBSTITUIDO; OS volta a AGUARDANDO_APROVACAO | Alta | U, I | Não executado | |
| CT33 | RN06 | Iniciar execução sem aprovação | ABERTA → EM_EXECUCAO | 422 | Alta | U | Não executado | |
| CT34 | Seção 2.5 | Todas as transições permitidas | Teste parametrizado com cada linha da tabela de transições | Todas aceitas | Alta | U | Não executado | |
| CT35 | Seção 2.5 | Transições não permitidas | Teste parametrizado com pares fora da tabela | Todas recusadas com 422 | Alta | U | Não executado | |
| CT36 | RN08 | Mudar status de OS final | ENTREGUE, RECUSADA ou CANCELADA → qualquer status | 422 | Alta | U | Não executado | |
| CT37 | HU07 | Peça em falta | EM_EXECUCAO → AGUARDANDO_PECA → EM_EXECUCAO | Ambas aceitas | Média | U | Não executado | |
| CT38 | RN09 | Mecânico atribuído inicia serviço | Token do mecânico atribuído | 200; OS EM_EXECUCAO | Alta | I | Não executado | |
| CT39 | RN09 | Mecânico não atribuído | Token de outro mecânico | 403; status inalterado | Alta | I | Não executado | |
| CT40 | — | Histórico de status | Executar 3 transições | 3 registros em historico_status com usuário, data, de/para | Alta | I | Não executado | |
| CT41 | — | Edição concorrente | Duas atualizações com a mesma versão da OS | A segunda recebe 409 | Média | I | Não executado | |

### 5.5 Consultas, compartilhamento e painel (HU06, HU08, HU09)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT42 | HU08 | Quadro por status | GET /api/ordens?status=EM_EXECUCAO | Só OS no status pedido, paginadas | Alta | I | Não executado | |
| CT43 | HU08 | Filtro por mecânico | GET /api/ordens?mecanico={id} | Só OS do mecânico | Média | I | Não executado | |
| CT44 | HU09 | Histórico do veículo | Veículo com 3 OS | Lista da mais recente para a mais antiga, com data, km, serviços e valor | Alta | I | Não executado | |
| CT45 | HU06 | PDF do orçamento | GET .../pdf | 200, application/pdf, com número da OS, placa e total | Média | I | Não executado | |
| CT46 | HU06 | Link do WhatsApp | Gerar link de compartilhamento | URL wa.me com telefone do cliente e total | Média | U, F | Não executado | |
| CT47 | RF16 | Painel do mês | OS abertas, concluídas e entregues no mês | Contagens corretas; faturamento = soma das OS entregues | Baixa | I | Não executado | |

### 5.6 Frontend

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT48 | HU01 | Rota protegida sem login | Acessar /quadro sem token | Redireciona para /login | Alta | F | Não executado | |
| CT49 | RNF04 | Interceptor de autenticação | Requisição HTTP com usuário logado | Cabeçalho Authorization: Bearer <token> | Alta | F | Não executado | |
| CT50 | HU01 | Token expirado | API responde 401 | Usuário volta ao login com aviso | Média | F | Não executado | |
| CT51 | HU05 | Resumo do orçamento na tela | Incluir e remover itens | Subtotais e total atualizados na hora | Média | F | Não executado | |
| CT52 | HU08 | Menu por perfil | Logar como atendente | Itens Usuários, Catálogo e Painel não aparecem | Média | F | Não executado | |

### 5.7 Testes manuais e não funcionais

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT53 | Todas as HUs | Fluxo completo | Login → cadastrar cliente e veículo → abrir OS → orçar → compartilhar → aprovar → atribuir → executar → aguardar peça → concluir → entregar | Todas as etapas sem erro; OS aparece no histórico do veículo | Alta | M | Não executado | |
| CT54 | RNF01 | Uso no celular | Abrir login, quadro e histórico com largura de 360 px | Telas legíveis e utilizáveis, sem rolagem horizontal | Média | M | Não executado | |
| CT55 | RNF02 | Abrir OS rapidamente | Contar telas até abrir OS de veículo cadastrado | No máximo 3 telas | Média | M | Não executado | |
| CT56 | RNF05 | Desempenho da listagem | Banco com 10 mil OS; GET /api/ordens | Resposta em até 2 segundos | Média | M | Não executado | |
| CT57 | RNF06 | Dados pessoais | Revisar respostas e logs da API | Sem senha, token ou dado desnecessário exposto | Alta | M | Não executado | |

## 6. Execuções

Cada execução registrada com data, comando, resultado e commit testado.

| Data | Branch / commit | Comando | Testes | Falhas | Observação |
| --- | --- | --- | --- | --- | --- |
| 2026-11-12 | feature/00-ambiente | `./mvnw -B verify` (api) | 1 | 0 | Contexto sobe e Flyway aplica V1. Docker indisponível na máquina (WSL2 sem "Plataforma de Máquina Virtual"); executado com `TORQUE_TEST_DB_URL` em PostgreSQL 17 local descartável. CI bloqueado no GitHub (cobrança da conta). |
| 2026-11-12 | feature/00-ambiente | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 3 | 0 | Build ok; Karma em ChromeHeadless. |

## 7. Defeitos

Defeitos encontrados durante o desenvolvimento e os testes, com causa e correção.

| ID | Data | Caso | Descrição do defeito | Severidade | Causa | Correção (commit) | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | | | |

## 8. Resumo final

Preenchido ao fim da Macro Entrega 3.

| Indicador | Valor |
| --- | --- |
| Casos planejados | 57 |
| Casos executados | |
| Aprovados | |
| Reprovados / bloqueados | |
| Defeitos encontrados / corrigidos | |
| Cobertura do pacote `ordens` (JaCoCo) | |
