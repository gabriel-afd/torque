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
| CT01 | HU01 | Login com credenciais válidas | POST /api/auth/login com e-mail e senha corretos | 200 com token JWT contendo o perfil | Alta | I | Aprovado | AuthIntegracaoTest#deveAutenticarComCredenciaisValidasEDevolverTokenComPerfil (2026-11-12) |
| CT02 | HU01 | Senha incorreta | POST /api/auth/login com senha errada | 401 com mensagem "E-mail ou senha inválidos" | Alta | I | Aprovado | AuthIntegracaoTest#deveRecusarSenhaIncorretaCom401 (2026-11-12) |
| CT03 | HU01 | E-mail inexistente | POST /api/auth/login com e-mail não cadastrado | 401 com a mesma mensagem do CT02 | Alta | I | Aprovado | AuthIntegracaoTest#deveRecusarEmailInexistenteComAMesmaMensagem (2026-11-12) |
| CT04 | HU01 | Usuário inativo | Login de usuário com ativo = false | 401 | Média | I | Aprovado | AuthIntegracaoTest#deveRecusarUsuarioInativoComAMesmaMensagem (2026-11-12) |
| CT05 | RNF04 | Requisição sem token | GET /api/clientes sem Authorization | 401 | Alta | I | Aprovado | AuthIntegracaoTest#deveExigirTokenNasRotasProtegidas (2026-11-12) |
| CT06 | RNF04 | Mecânico em rota de administrador | GET /api/usuarios com token de MECANICO | 403 | Alta | I | Aprovado | UsuarioIntegracaoTest#deveRecusarMecanicoNaRotaDeUsuariosCom403 (2026-11-12) |
| CT07 | RNF03 | Senha protegida | Cadastrar usuário e consultar banco e resposta | Senha gravada com hash BCrypt; nunca retornada pela API | Alta | I | Aprovado | UsuarioIntegracaoTest#deveGravarSenhaComBcryptENuncaDevolverASenha (2026-11-12) |

### 5.2 Clientes e veículos (HU02, HU03)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT08 | HU02, RN03 | Cadastro com CPF válido | Nome, telefone e CPF válido | 201; cliente encontrado na busca | Alta | I | Aprovado | ClienteIntegracaoTest#deveCadastrarClienteComCpfValidoEEncontrarNaBusca (2026-11-12) |
| CT09 | RN03 | CPF com dígito verificador inválido | CPF 111.111.111-12 | 400 com erro no campo documento | Alta | U, I | Aprovado | CpfCnpjValidatorTest#deveRecusarDocumentoInvalido, ClienteIntegracaoTest#deveRecusarCpfComDigitoInvalidoCom400NoCampoDocumento (2026-11-12) |
| CT10 | RN03 | CPF duplicado | Cadastrar duas vezes o mesmo CPF | 409; nenhum registro duplicado | Alta | I | Aprovado | ClienteIntegracaoTest#deveRecusarCpfDuplicadoCom409SemGravarDuplicado, ClienteServiceTest#deveConverterViolacaoDeUniqueDoBancoEmConflito (2026-11-12) |
| CT11 | RN03 | CNPJ válido | Cliente com CNPJ válido | 201 | Média | U | Aprovado | CpfCnpjValidatorTest#deveAceitarCnpjValidoComOuSemMascara (2026-11-12) |
| CT12 | HU03, RN02 | Placa no formato Mercosul | ABC1D23 | Aceita | Alta | U, I | Aprovado | PlacaValidatorTest#deveAceitarPlacaMercosul, VeiculoIntegracaoTest#deveCadastrarPlacaMercosulNormalizada (2026-11-12) |
| CT13 | RN02 | Placa no formato antigo | ABC1234 | Aceita | Alta | U | Aprovado | PlacaValidatorTest#deveAceitarPlacaNoFormatoAntigo (2026-11-12) |
| CT14 | RN02 | Placa inválida | AB-1234, ABCD123 | 400 com erro no campo placa | Alta | U, I | Aprovado | PlacaValidatorTest#deveRecusarPlacaInvalida, VeiculoIntegracaoTest#deveRecusarPlacaInvalidaCom400NoCampoPlaca (2026-11-12) |
| CT15 | RN02 | Normalização da placa | abc-1d23 | Gravada como ABC1D23 | Média | U | Aprovado | PlacaValidatorTest#deveNormalizarParaMaiusculasSemHifenEEspacos (2026-11-12) |
| CT16 | RN02 | Placa duplicada | Cadastrar a mesma placa duas vezes | 409 | Alta | I | Aprovado | VeiculoIntegracaoTest#deveRecusarPlacaDuplicadaCom409, ClienteServiceTest#deveConverterViolacaoDeUniqueDaPlacaEmConflito (2026-11-12) |
| CT17 | HU03 | Busca por placa | GET /api/veiculos?placa=ABC1D23 | 200 com veículo e dono; placa inexistente → 404 | Alta | I | Aprovado | VeiculoIntegracaoTest#deveBuscarVeiculoPorPlacaComDadosDoDono, #deveDevolver404ParaPlacaInexistente (2026-11-12) |
| CT18 | RN01 | Veículo sem cliente | POST de veículo para cliente inexistente | 404; nada gravado | Média | I | Aprovado | VeiculoIntegracaoTest#deveDevolver404AoCadastrarVeiculoParaClienteInexistenteSemGravarNada (2026-11-12) |

### 5.3 Catálogo e orçamento (HU05)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT19 | RN04 | Cálculo do total | 1 serviço de R$ 120,00 + 2 peças de R$ 35,00 | Total R$ 190,00 | Alta | U | Aprovado | OrcamentoTest#deveCalcularTotalComoServicosMaisQuantidadeVezesPrecoDasPecas (2026-11-12) |
| CT20 | RN04 | Total com desconto | Itens somando R$ 390,00, desconto R$ 19,50 | Total R$ 370,50 | Alta | U | Aprovado | OrcamentoTest#deveSubtrairDescontoDoTotal (2026-11-12) |
| CT21 | RN05 | Desconto acima de 10% por atendente | Desconto de 15% com perfil ATENDENTE | 422; orçamento inalterado | Alta | U, I | Aprovado | OrcamentoTest#deveRecusarDescontoAcimaDeDezPorCentoParaAtendenteSemAlterarOrcamento, OrdemServicoIntegracaoTest#deveRecusarDescontoAcimaDeDezPorCentoParaAtendenteCom422SemAlterarOrcamento (2026-11-12) |
| CT22 | RN05 | Desconto de exatamente 10% por atendente | Desconto de 10% | Aceito (valor de borda) | Alta | U | Aprovado | OrcamentoTest#deveAceitarDescontoDeExatamenteDezPorCentoParaAtendente (2026-11-12) |
| CT23 | RN05 | Desconto acima de 10% por administrador | Desconto de 15% com perfil ADMINISTRADOR | Aceito | Média | U | Aprovado | OrcamentoTest#deveAceitarDescontoAcimaDeDezPorCentoParaAdministrador (2026-11-12) |
| CT24 | RN10 | Preço congelado no item | Alterar preço no catálogo depois de incluir o item | Item e total da OS mantêm o valor original | Alta | I | Aprovado | OrdemServicoIntegracaoTest#deveManterPrecoDoItemAoAlterarCatalogoDepois (2026-11-12) |
| CT25 | — | Quantidade inválida de peça | Quantidade 0 ou negativa | 400 | Média | I | Aprovado | OrdemServicoIntegracaoTest#deveRecusarQuantidadeDePecaZeroOuNegativaCom400 (2026-11-12) |
| CT26 | — | Item inativo no catálogo | Incluir serviço ou peça inativa | 422 | Baixa | U | Aprovado | OrcamentoTest#deveRecusarServicoInativoDoCatalogo, #deveRecusarPecaInativaDoCatalogo (2026-11-12) |

### 5.4 Ordem de serviço e fluxo de status (HU04, HU06, HU07)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT27 | HU04 | Abrir OS | Veículo existente, problema e quilometragem | 201; status ABERTA; número sequencial; histórico gravado | Alta | I | Aprovado | OrdemServicoIntegracaoTest#deveAbrirOsComNumeroSequencialStatusAbertaEHistorico (2026-11-12) |
| CT28 | HU04 | OS para veículo inexistente | veiculoId inválido | 404 | Média | I | Aprovado | OrdemServicoIntegracaoTest#deveDevolver404AoAbrirOsParaVeiculoInexistente (2026-11-12) |
| CT29 | HU06 | Aprovar orçamento pendente | POST .../aprovacao | OS APROVADA; orçamento APROVADO com data e usuário | Alta | U, I | Aprovado | OrdemServicoTest#deveAprovarOrcamentoPendenteRegistrandoDataEUsuario, OrdemServicoIntegracaoTest#deveAprovarOrcamentoRegistrandoDataEUsuario (2026-11-12) |
| CT30 | RN07 | Aprovar orçamento já aprovado | Aprovar duas vezes | 422 na segunda chamada | Alta | U | Aprovado | OrdemServicoTest#deveRecusarSegundaAprovacaoDoMesmoOrcamento (2026-11-12) |
| CT31 | HU06 | Recusar orçamento | POST .../recusa | OS RECUSADA | Alta | U, I | Aprovado | OrdemServicoTest#deveRecusarOrcamentoEFinalizarOsComoRecusada, OrdemServicoIntegracaoTest#deveRecusarOrcamentoEFinalizarOs (2026-11-12) |
| CT32 | RN07 | Alterar orçamento aprovado | Incluir item em OS APROVADA | Nova versão PENDENTE; anterior SUBSTITUIDO; OS volta a AGUARDANDO_APROVACAO | Alta | U, I | Aprovado | OrdemServicoTest#deveCriarNovaVersaoAoAlterarOrcamentoAprovado, OrdemServicoIntegracaoTest#deveCriarNovaVersaoAoAlterarOrcamentoAprovado (2026-11-12) |
| CT33 | RN06 | Iniciar execução sem aprovação | ABERTA → EM_EXECUCAO | 422 | Alta | U | Aprovado | OrdemServicoTest#deveRecusarInicioDeExecucaoSemOrcamentoAprovado, TransicoesStatusTest#deveRecusarTransicaoForaDaTabelaComRegraDeNegocio[ABERTA -> EM_EXECUCAO] (2026-11-12) |
| CT34 | Seção 2.5 | Todas as transições permitidas | Teste parametrizado com cada linha da tabela de transições | Todas aceitas | Alta | U | Aprovado | TransicoesStatusTest#deveAceitarTodaTransicaoDaTabela (16 casos gerados da tabela) (2026-11-12) |
| CT35 | Seção 2.5 | Transições não permitidas | Teste parametrizado com pares fora da tabela | Todas recusadas com 422 | Alta | U | Aprovado | TransicoesStatusTest#deveRecusarTransicaoForaDaTabelaComRegraDeNegocio (65 pares fora da tabela) (2026-11-12) |
| CT36 | RN08 | Mudar status de OS final | ENTREGUE, RECUSADA ou CANCELADA → qualquer status | 422 | Alta | U | Aprovado | OrdemServicoTest#deveRecusarQualquerMudancaDeOsEntregue, #deveRecusarQualquerMudancaDeOsRecusadaOuCancelada (2026-11-12) |
| CT37 | HU07 | Peça em falta | EM_EXECUCAO → AGUARDANDO_PECA → EM_EXECUCAO | Ambas aceitas | Média | U | Aprovado | OrdemServicoTest#deveIrEVoltarDeAguardandoPeca (2026-11-12) |
| CT38 | RN09 | Mecânico atribuído inicia serviço | Token do mecânico atribuído | 200; OS EM_EXECUCAO | Alta | I | Aprovado | OrdemServicoIntegracaoTest#deveDeixarMecanicoAtribuidoIniciarServico (2026-11-12) |
| CT39 | RN09 | Mecânico não atribuído | Token de outro mecânico | 403; status inalterado | Alta | I | Aprovado | OrdemServicoIntegracaoTest#deveRecusarMecanicoNaoAtribuidoCom403SemAlterarStatus (2026-11-12) |
| CT40 | — | Histórico de status | Executar 3 transições | 3 registros em historico_status com usuário, data, de/para | Alta | I | Aprovado | OrdemServicoIntegracaoTest#deveGravarHistoricoDeCadaTransicao (2026-11-12) |
| CT41 | — | Edição concorrente | Duas atualizações com a mesma versão da OS | A segunda recebe 409 | Média | I | Aprovado | OrdemServicoIntegracaoTest#deveRecusarSegundaAtualizacaoComAMesmaVersaoDaOsCom409, TratadorDeErrosTest#deveDevolver409ParaConflitoDeVersaoPedindoRecarga (2026-11-12) |

### 5.5 Consultas, compartilhamento e painel (HU06, HU08, HU09)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT42 | HU08 | Quadro por status | GET /api/ordens?status=EM_EXECUCAO | Só OS no status pedido, paginadas | Alta | I | Aprovado | OrdemServicoConsultaIntegracaoTest#deveFiltrarQuadroPorStatusComPaginacao (2026-11-12) |
| CT43 | HU08 | Filtro por mecânico | GET /api/ordens?mecanico={id} | Só OS do mecânico | Média | I | Aprovado | OrdemServicoConsultaIntegracaoTest#deveFiltrarQuadroPorMecanico, #deveMostrarAoMecanicoSoAsPropriasOsMesmoPedindoOutro (2026-11-12) |
| CT44 | HU09 | Histórico do veículo | Veículo com 3 OS | Lista da mais recente para a mais antiga, com data, km, serviços e valor | Alta | I | Aprovado | OrdemServicoConsultaIntegracaoTest#deveListarHistoricoDoVeiculoDaMaisRecenteParaAMaisAntiga (2026-11-12) |
| CT45 | HU06 | PDF do orçamento | GET .../pdf | 200, application/pdf, com número da OS, placa e total | Média | I | Aprovado | OrcamentoDocumentosIntegracaoTest#deveGerarPdfDoOrcamentoComNumeroDaOsPlacaETotal, #deveNegarPdfAoMecanico, #deveResponder404ParaVersaoOuOsInexistente; OrcamentoPdfTest (2026-11-15) |
| CT46 | HU06 | Link do WhatsApp | Gerar link de compartilhamento | URL wa.me com telefone do cliente e total | Média | U, F | Aprovado | CompartilhamentoWhatsAppTest#deveGerarLinkWaMeComDdiDoBrasilETelefoneDoCliente, #deveIncluirResumoComNumeroDaOsVeiculoItensETotal, #deveCodificarTextoParaUrlSemEspacosNemCaracteresEspeciaisCrus; OrcamentoDocumentosIntegracaoTest#deveGerarLinkDoWhatsAppComTelefoneDoClienteETotal (2026-11-15); F: ordem-detalhe.component.spec.ts "deve baixar o PDF do orçamento e abrir o link do WhatsApp com telefone e total", "deve avisar quando o navegador bloquear a janela do WhatsApp" (2026-11-16) |
| CT47 | RF16 | Painel do mês | OS abertas, concluídas e entregues no mês | Contagens corretas; faturamento = soma das OS entregues | Baixa | I | Aprovado | PainelMesIntegracaoTest#deveContarOsDoMesESomarFaturamentoDasEntregues, #deveConsiderarSoOMesPedido, #deveNegarPainelAAtendenteEMecanico (2026-11-15) |

### 5.6 Frontend

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT48 | HU01 | Rota protegida sem login | Acessar /quadro sem token | Redireciona para /login | Alta | F | Aprovado | auth.guard.spec.ts "deve redirecionar para /login quem acessa /quadro sem token" (2026-11-15) |
| CT49 | RNF04 | Interceptor de autenticação | Requisição HTTP com usuário logado | Cabeçalho Authorization: Bearer <token> | Alta | F | Aprovado | auth.interceptor.spec.ts "deve enviar Authorization: Bearer <token> nas chamadas à API com usuário logado" (2026-11-15) |
| CT50 | HU01 | Token expirado | API responde 401 | Usuário volta ao login com aviso | Média | F | Aprovado | auth.interceptor.spec.ts "deve voltar ao login com aviso quando a API responder 401 (token expirado)"; auth.service.spec.ts "deve descartar sessão salva com token expirado" (2026-11-15) |
| CT51 | HU05 | Resumo do orçamento na tela | Incluir e remover itens | Subtotais e total atualizados na hora | Média | F | Aprovado | ordem-detalhe.component.spec.ts "deve atualizar subtotais e total na hora ao remover e incluir itens", "deve recalcular ao mudar a quantidade de uma peça"; orcamento.spec.ts "deve recalcular ao incluir, remover e mudar quantidade (CT51)" (2026-11-16) |
| CT52 | HU08 | Menu por perfil | Logar como atendente | Itens Usuários, Catálogo e Painel não aparecem | Média | F | Aprovado | layout.component.spec.ts "não deve mostrar Usuários, Catálogo e Painel para ATENDENTE" (2026-11-15) |

### 5.7 Testes manuais e não funcionais

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT53 | Todas as HUs | Fluxo completo | Login → cadastrar cliente e veículo → abrir OS → orçar → compartilhar → aprovar → atribuir → executar → aguardar peça → concluir → entregar | Todas as etapas sem erro; OS aparece no histórico do veículo | Alta | M | Aprovado | scripts/testes-manuais/ct53-ct57-api.mjs: 15 passos do roteiro pela API (via proxy do frontend), OS nº 1017 de ABERTA a ENTREGUE com 8 registros de histórico e no topo do histórico do veículo; extras: RN05 (422), RN08 (422), RN09 (403); saída em docs/evidencias/ct53-ct57-saida.txt; tela final em docs/evidencias/ct54-ct55/ct54-3-ordem.png (2026-11-25) |
| CT54 | RNF01 | Uso no celular | Abrir login, quadro e histórico com largura de 360 px | Telas legíveis e utilizáveis, sem rolagem horizontal | Média | M | Aprovado | scripts/testes-manuais/ct54-ct55-navegador.mjs (Chrome headless, 360 × 780, modo móvel): login, quadro, tela da OS e histórico com scrollWidth = 360 (sem rolagem horizontal); colunas do quadro rolam dentro do contêiner; menu ☰; resumo abaixo dos itens; botão "+ Nova OS" fixo sem cobrir o último cartão; prints em docs/evidencias/ct54-ct55/ (2026-11-25) |
| CT55 | RNF02 | Abrir OS rapidamente | Contar telas até abrir OS de veículo cadastrado | No máximo 3 telas | Média | M | Aprovado | scripts/testes-manuais/ct54-ct55-navegador.mjs: quadro → diálogo Nova OS (placa + problema) → /ordens/19 com status Aberta = 3 telas; prints ct55-1 a ct55-3 em docs/evidencias/ct54-ct55/ (2026-11-25) |
| CT56 | RNF05 | Desempenho da listagem | Banco com 10 mil OS; GET /api/ordens | Resposta em até 2 segundos | Média | M | Aprovado | scripts/ct56/gerar-10mil-os.sql + medir-listagem.sh, 10.017 OS, 10 chamadas por consulta: padrão 0,023 s (máx. 0,031 s), quadro com 6 status/100 por página 0,024 s, busca 0,022 s, página 150 0,022 s (2026-11-16; API jar + PostgreSQL 17 local) |
| CT57 | RNF06 | Dados pessoais | Revisar respostas e logs da API | Sem senha, token ou dado desnecessário exposto | Alta | M | Aprovado | scripts/testes-manuais/ct53-ct57-api.mjs: login e GET /api/usuarios sem senha ou hash; MECANICO recebe o cliente só com id e nome; CPF duplicado 409 no formato padrão, sem stack trace; sem token 401. Log da API após o fluxo (47 linhas) sem senha, token `eyJ`, hash BCrypt nem o CPF usado (2026-11-25) |

### 5.8 Implantação (Macro Entrega 4)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT58 | Implantação | Imagem única serve frontend e API | GET `/`, `/index.html` e rotas do Angular sem token; GET de arquivo `.js` inexistente; GET `/api/...` sem e com token; POST fora de `/api` sem token | `index.html` nas rotas do SPA (forward na raiz); 404 JSON para arquivo e rota `/api` inexistentes; 401 na API sem token; escrita fora de `/api` continua exigindo login | Alta | I | Aprovado | FrontendIntegracaoTest#deveServirIndexHtmlSemLogin, #deveServirIndexHtmlNaRaiz, #deveEncaminharRotaDoAngularParaIndexHtml, #deveResponder404ParaArquivoEstaticoInexistente, #deveManterApiProtegidaSemToken, #naoDeveEncaminharRotaApiInexistenteParaIndexHtml, #naoDeveLiberarEscritaForaDaApiSemToken (2026-11-26) |
| CT59 | Implantação | Jar com o build do Angular em 512 MB | Reproduzir os estágios do Dockerfile sem Docker (`ng build` → `static/` → `mvn package`); subir o jar com as flags de memória da imagem (`-Xmx307m`, SerialGC, porta via `PORT`) contra PostgreSQL local | Sobe sem erro; `/`, `/quadro`, `/ordens/17` e o `main-*.js` com 200; login 200 e `/api/ordens` 200 com token; memória abaixo de 512 MB | Alta | M | Aprovado | Subida em 7,4 s; rotas do SPA 200 `text/html`; `main-6XPHJAN3.js` 200; `/main-inexistente.js` 404 JSON; `/api/ordens` 401 sem token e 200 com token; Swagger 200; RSS do processo 329 MB (2026-11-26; Docker indisponível na máquina) |
| CT60 | Implantação | Deploy no Render + Neon | Blueprint `render.yaml`, banco no Neon; acessar a URL pública, logar como admin e abrir o quadro | Build da imagem ok, Flyway aplica as migrações, carga demo no banco vazio, login e quadro funcionam por HTTPS | Alta | M | Não executado | Depende das contas do Render e do Neon (feito pelo autor) |
| CT61 | Implantação | Carga inicial com senha própria | Jar com perfil `demo` e `TORQUE_DEMO_SENHA` num banco vazio, com `ADMIN_EMAIL` definido | Dados fictícios carregados; usuário demo entra só com a senha configurada (a padrão é recusada); sem `admin.demo` extra; senha fora do log | Alta | M | Aprovado | 4 usuários (admin do ADMIN_EMAIL + ana, raimundo, tiago), 17 OS em 9 status; `ana.demo` com `demo12345` 401 e com a senha configurada 200; `admin.demo` inexistente (401); log sem a senha nem token (2026-11-26) |

### 5.9 Ajustes de uso (Macro Entrega 4)

| ID | Origem | Cenário | Entrada / passos | Resultado esperado | Prioridade | Nível | Status | Evidência |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CT62 | RNF01 | Campos de formulário sem sobreposição | Abrir o login (e demais formulários) com os padrões reais do app; enviar vazio | Cada campo reserva a linha da mensagem de erro; o contorno de um campo não encosta no seguinte | Alta | F | Aprovado | app.config.spec.ts "deve reservar a linha da mensagem de erro em cada campo, sem um campo encostar no outro"; conferido no Chrome headless em 1366 px e 360 px: contorno do e-mail termina 22 px antes do campo senha (2026-11-26) |
| CT63 | HU08, RN06–RN09 | Arrastar cartões no quadro | Soltar cartões em outras colunas com cada perfil | Movimentos da tabela de transições executam a ação na API (aprovação pela versão vigente do orçamento); os demais mostram mensagem amigável sem chamar a API; recusa da API devolve o cartão e mostra a mensagem dela; o clique ao soltar não abre a OS | Alta | F | Aprovado | quadro.spec.ts "arrastar cartões no quadro (tabela de transições)" (9 testes: todos os pares de colunas, perfis e RN09); quadro.component.spec.ts "arrastar de Aberta para Aguardando aprovação…", "…para Aprovada…", "…direto para Concluída…", "MECANICO arrastando a OS de outro mecânico…", "MECANICO atribuído…", "quando a API recusar o movimento…", "soltar o cartão na própria coluna…", "não deve abrir a OS com o clique…"; arraste real com o mouse (DevTools) contra a API: Aberta→Concluída recusado com "Só OS em execução podem ser concluídas.", Aberta→Aguardando aprovação executado (2026-11-26) |
| CT64 | RNF01 | Tema escuro e marca | Abrir login, quadro, OS, novo cliente, clientes, painel e diálogo Nova OS em 1366 px e 360 px | Fundo grafite legível, azul da navegação e laranja da ação principal mantidos; engrenagem laranja no lugar da bolinha e na aba; sem rolagem horizontal no celular | Média | M | Aprovado | login.component.spec.ts "deve mostrar a engrenagem da marca ao lado do nome"; prints conferidos visualmente (Chrome headless, API com dados demo); 360 px: scrollWidth = 360 (2026-11-26) |
| CT65 | HU02 | Novo cliente em diálogo | Clientes → "+ Novo cliente"; salvar; cancelar; abrir /clientes/novo | Cadastro abre sobre a lista; salvo, vai para o cliente; cancelado, continua na lista; 409 de CPF no campo com o diálogo aberto; /clientes/novo abre o diálogo e volta para /clientes ao cancelar | Média | F | Aprovado | novo-cliente.component.spec.ts "deve cadastrar e fechar devolvendo o cliente", "não deve chamar a API com o formulário inválido", "deve mostrar o 409 de CPF duplicado no campo e manter o diálogo aberto", "\"Novo cliente\" deve abrir o cadastro em diálogo e ir para o cliente cadastrado", "não deve sair da lista quando o diálogo for cancelado", "/clientes/novo: deve abrir o diálogo sobre a lista e voltar para /clientes ao cancelar" (2026-11-26) |
| CT66 | HU03, RN01 | Novo veículo sem buscar a placa | Veículos → "+ Novo veículo" | Cadastro rápido com placa em branco (dono existente ou novo); cadastrado, abre o histórico do veículo; MECANICO não vê o botão | Média | F | Aprovado | veiculos-pagina.component.spec.ts (3 testes); cadastro-rapido.component.spec.ts "deve vir com a placa buscada já preenchida" (título com a placa) (2026-11-26) |
| CT67 | RNF01 | Visual Grafite e Lima | Abrir login, quadro, OS, clientes, novo cliente, veículos, histórico, painel, catálogo e usuários em 1440 px e 390 px | Telas conforme docs/prototipos/grafite-e-lima; menu lateral com ícones por perfil e trilha "Oficina / …"; resumo do quadro; busca e paginador legíveis; celular sem rolagem horizontal | Média | M | Aprovado | layout.component.spec.ts "deve mostrar o ícone de cada item do menu", "deve montar a trilha…"; quadro.spec.ts "cartão e resumo do quadro (visual Grafite e Lima)" (3 testes); quadro.component.spec.ts "deve mostrar o resumo do topo…", "deve mostrar o texto da busca inteiro…"; login.component.spec.ts "deve mostrar e ocultar a senha…"; paginador-intl.spec.ts; 15 prints conferidos (Chrome headless, API com dados demo), 390 px: scrollWidth = 390 no quadro, na OS e no login; prints em docs/prints (2026-11-26) |
| CT68 | — | Crédito do autor | Abrir o login (1440 px e 390 px) e o menu lateral logado | "Desenvolvido por Gabriel Medeiros de Mendonça" no rodapé do painel grafite do login (também no celular) e no rodapé do menu lateral | Baixa | F | Aprovado | login.component.spec.ts "deve mostrar o crédito do autor na tela inicial"; layout.component.spec.ts "deve mostrar o crédito do autor no menu lateral"; prints conferidos (Chrome headless) (2026-11-26) |

## 6. Execuções

Cada execução registrada com data, comando, resultado e commit testado.

| Data | Branch / commit | Comando | Testes | Falhas | Observação |
| --- | --- | --- | --- | --- | --- |
| 2026-11-12 | feature/00-ambiente | `./mvnw -B verify` (api) | 1 | 0 | Contexto sobe e Flyway aplica V1. Docker indisponível na máquina (WSL2 sem "Plataforma de Máquina Virtual"); executado com `TORQUE_TEST_DB_URL` em PostgreSQL 17 local descartável. CI bloqueado no GitHub (cobrança da conta). |
| 2026-11-12 | feature/00-ambiente | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 3 | 0 | Build ok; Karma em ChromeHeadless. |
| 2026-11-12 | feature/01-base-compartilhada | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 70 | 0 | Validadores CPF/CNPJ e placa, handler de erros (500 sem stack trace). |
| 2026-11-12 | feature/02-autenticacao | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 101 | 0 | 1 falha intermediária por teste mal escrito (token com caractere extra é aceito pelo decodificador base64; trocado por payload adulterado). Subida manual: sem JWT_SECRET a API recusa subir; admin inicial criado; login 200/401. |
| 2026-11-12 | feature/03-clientes-veiculos | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 126 | 0 | Clientes e veículos; ddl-auto validate ok com CHAR(7) e SMALLINT. |
| 2026-11-12 | feature/04-catalogo | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 141 | 0 | Catálogo: CRUD, 403 para atendente, código duplicado 409, preço negativo 400, inativos só para admin. Sem CT específico no plano (item 6 do backlog). |
| 2026-11-12 | feature/05-dominio-os | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 295 | 0 | Domínio da OS: 152 testes unitários puros + 1 de mapeamento JPA. JaCoCo pacote ordens: 94,8% linhas, 81,0% ramos. |
| 2026-11-12 | feature/06-api-os | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 337 | 1 → 0 | 1 falha por defeito de código (D03, corrigido em 266bc7a); 4 falhas intermediárias por testes mal escritos (leitura da resposta em ISO-8859-1 e termo de busca errado). N+1 conferido no log SQL e por teste com Hibernate Statistics. JaCoCo ordens: 97,0% linhas / 84,7% ramos. |
| 2026-11-15 | feature/07-pdf-whatsapp-painel | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 362 | 0 | PDF (CT45), link do WhatsApp (CT46) e painel do mês (CT47). 1 falha intermediária por teste mal escrito (a extração de texto do PDF junta células vizinhas da tabela sem espaço). Nenhum defeito de código. JaCoCo ordens: 97,5% linhas / 86,7% ramos; relatorios: 100%. |
| 2026-11-15 | feature/08-auditoria-backend | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 375 | 8 → 0 | Auditoria (docs/auditoria-backend.md): 8 testes novos falharam antes das correções, reproduzindo D05–D09; D04 evidenciado por `mvn dependency:list`. Após Spring Boot 3.5.7 nenhum teste antigo quebrou. 2 falhas intermediárias por testes mal escritos (senha de 73 caracteres barrada pelo @Size antes de chegar ao BCrypt; asserção com acento dependente do charset do console). 1 teste antigo ajustado por mudança intencional (MECANICO não recebe mais o telefone do dono). JaCoCo total: 96,0% linhas / 85,5% ramos. |
| 2026-11-15 | feature/09-web-nucleo | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 33 | 0 | Núcleo do frontend: CT48, CT49, CT50, CT52. Build ok (budget inicial ajustado para 800 kB por causa do Angular Material). Teste manual: API (jar) + `ng serve`; login do admin pelo proxy 200, senha errada 401 com "E-mail ou senha inválidos", rota sem token 401, Swagger 200. |
| 2026-11-15 | feature/10-web-clientes | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 59 | 4 → 0 | Clientes e veículos: validadores CPF/CNPJ e placa (validadores.spec.ts), erros 400/409 no campo (cliente-form, cadastro-rapido), fluxo "placa não encontrada → oferecer cadastro" (busca-placa.component.spec.ts). 4 falhas por defeito de código (D10). Complementa no frontend CT09, CT14, CT16 e CT17. |
| 2026-11-15 | feature/11-web-quadro | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 78 | 0 | Quadro de OS (HU08): agrupamento por status com contadores, alertas (orçamento > 1 dia, aguardando peça), filtros de busca e mecânico, MECANICO sem filtro/sem nova OS, clique abre /ordens/:id, abertura de OS pela placa. Complementa no frontend CT42 e CT43. Layout em 360 px não conferido visualmente (fica para CT54, teste manual). |
| 2026-11-16 | feature/12-web-orcamento | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 129 | 3 → 0 | Tela da OS e orçamento (HU04–HU07): CT46 (parte F) e CT51; botões por status e perfil (orcamento.spec.ts e ordem-detalhe.component.spec.ts), aviso de RN05 e de nova versão (RN07), mensagem 422 da API, confirmação de cancelar/recusar, atribuição de mecânico. 3 falhas intermediárias por teste mal escrito (TestBed sem LOCALE_ID pt-BR: valores saíam como "R$320.50"). Nenhum defeito de código. |
| 2026-11-16 | feature/13-web-historico-admin | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 157 | 6 → 0 | Histórico do veículo (HU09, complementa CT44 no frontend: ordem da API, data, km, serviços, valor, total de OS, última km, "+ Nova OS" com o veículo já escolhido), CRUD do catálogo (lista, busca, inativos, 409 de código, 400 de preço), CRUD de usuários (perfil, senha opcional na edição, 409 de e-mail, 422 do último admin) e painel do mês (complementa CT47: números e seletor de mês). 6 falhas intermediárias por teste mal escrito no spec do catálogo (`TestBed.inject` antes de `overrideProvider`). Nenhum defeito de código. |
| 2026-11-16 | feature/14-refatoracao | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 377 | 0 | Refatoração sem mudança de comportamento (B1 `Orcamento.alterar` dividido em validação/aplicação, B2 itens mantidos extraídos de `montarAlteracao`, B3 `UsuarioRepository.buscarOuFalhar`). Antes e depois: 377 testes, 0 falhas. JaCoCo total 96,0% linhas / 85,5% ramos antes e depois; ordens 97,5% → 97,6% linhas, 87,0% ramos. |
| 2026-11-16 | feature/14-refatoracao | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 157 | 0 | Refatoração do frontend (F1 `OrcamentoItensComponent`, F2 operador `comCarregamento`, F3 `ListaPaginada`, F4 ações por status em tabela, F5 cadastro rápido, F6 helpers de teste). 157 testes verdes antes e depois de cada item. Cobertura (karma-coverage) 88,3% → 89,5% linhas, 82,7% → 82,7% ramos. |
| 2026-11-16 | feature/15-testes-finais | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 383 | 0 | Suíte completa da API. Novos: perfil demo (DadosDemoTest, DadosDemoIntegracaoTest, DadosDemoForaDoPerfilIntegracaoTest). Conferido por script que toda evidência citada no plano (classe#método e nome de spec) existe. JaCoCo total 96,4% linhas / 86,2% ramos; ordens 97,6% / 87,0%. |
| 2026-11-16 | feature/15-testes-finais | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 157 | 0 | Suíte completa do frontend; cobertura 89,5% linhas / 82,7% ramos. |
| 2026-11-16 | feature/15-testes-finais | `scripts/ct56/medir-listagem.sh` (API jar, perfil demo, 10.017 OS) | 4 consultas × 10 | 0 | CT56: todas as médias entre 0,022 s e 0,024 s (meta 2 s). Subida com perfil demo carregou 4 usuários, 10 clientes, 15 veículos e 17 OS no banco real. Log da subida sem senha ou token (apoio ao CT57). |
| 2026-11-25 | feature/16-testes-manuais | `node scripts/testes-manuais/ct53-ct57-api.mjs` (API jar, perfil demo, frontend `ng serve`) | 27 verificações | 0 | CT53 e CT57 pela API através do proxy do frontend. Antes: suíte completa já verde na main (API 383, web 157). |
| 2026-11-25 | feature/16-testes-manuais | `node scripts/testes-manuais/ct54-ct55-navegador.mjs` (Chrome headless, 360 px) | 5 verificações | 0 | CT54 (4 telas sem rolagem horizontal) e CT55 (3 telas até abrir a OS). Prints conferidos visualmente. |
| 2026-11-26 | feature/18-deploy | `./mvnw -B verify` (TORQUE_TEST_DB_URL local) | 388 | 4 → 0 | CT58 (FrontendIntegracaoTest, 7 testes). 3 falhas esperadas antes da implementação (teste escrito primeiro: 401 nas rotas do frontend). 1 falha por teste mal escrito (`/` é welcome page com forward para index.html, que o MockMvc não segue; asserção trocada por `forwardedUrl`). Nenhum defeito de código. |
| 2026-11-26 | feature/18-deploy | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 157 | 0 | Sem mudança no frontend; o build de produção é o que entra na imagem. |
| 2026-11-26 | feature/18-deploy | Estágios do Dockerfile à mão + `java -Xmx307m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -jar` (PORT=8089) + curl | 12 verificações | 0 | CT59. Docker indisponível na máquina: a imagem em si só vai ser construída no primeiro deploy do Render (CT60). |
| 2026-11-26 | feature/18-deploy | Jar com `SPRING_PROFILES_ACTIVE=demo` e `TORQUE_DEMO_SENHA` em banco vazio + curl | 6 verificações | 0 | CT61. Observado: o Tomcat atende antes de a carga demo terminar, e um login feito nos primeiros segundos dá 401 até os usuários existirem. Não é defeito (a carga roda uma vez, na primeira subida). |
| 2026-11-26 | feature/19-ajustes-visuais-quadro | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 176 | 1 → 0 | CT62 a CT64. 1 falha por defeito de código (D11: o teste novo falhou com o padrão antigo e passou após a correção). 3 erros de compilação no spec por tipagem (`permitido: false` sem `as const`), teste mal escrito. Build sem avisos (estilo do quadro ajustado para o orçamento de 2 kB por componente). |
| 2026-11-26 | feature/19-ajustes-visuais-quadro | Chrome headless via DevTools contra `ng serve` + API (jar, perfil demo) | 6 verificações + 13 prints | 0 | CT62, CT63 (arraste real com o mouse) e CT64 em 1366 px e 360 px. |
| 2026-11-26 | feature/20-visual-grafite-lima | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 194 | 0 | CT65 a CT67, D12 e D13. Build sem avisos após ajustar os orçamentos (estilo por componente 4 kB; pacote inicial 900 kB por causa do menu lateral do Material). Avisos intermediários corrigidos: ícone do botão "Entrar" fora do slot (NG8011) e estilo do login acima do orçamento. Nenhuma falha de teste. |
| 2026-11-26 | feature/20-visual-grafite-lima | Chrome headless via DevTools contra `ng serve` + API (jar, perfil demo, banco zerado) | 15 prints | 0 | CT67 em 1440 px e 390 px. Primeira rodada mostrou campos de filtro altos com texto cortado e a 6ª coluna fora da tela em 1440 px; corrigidos (d40ed1d) e prints refeitos. |
| 2026-11-26 | feature/21-credito-autor | `npm run build && npm test -- --watch=false --browsers=ChromeHeadless` (web) | 196 | 2 → 0 | CT68. 2 falhas por teste mal escrito (regex de espaços gravada sem a barra invertida). Nenhum defeito de código. |

## 7. Defeitos

Defeitos encontrados durante o desenvolvimento e os testes, com causa e correção.

| ID | Data | Caso | Descrição do defeito | Severidade | Causa | Correção (commit) | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| D01 | 2026-11-12 | CT57 | Swagger UI inacessível: /api/docs redireciona para /api/swagger-ui/index.html, que respondia 401 | Baixa | Rota /api/swagger-ui/** fora da lista pública do SecurityFilterChain (encontrado no teste manual de subida) | Rota liberada + teste AuthIntegracaoTest#deveDeixarDocumentacaoDaApiPublica (7598265) | Corrigido |
| D02 | 2026-11-12 | CT57 | Senha de usuário padrão gerada pelo Spring impressa no log na subida | Média | UserDetailsServiceAutoConfiguration ativa por não haver UserDetailsService próprio (encontrado no teste manual de subida) | Autoconfiguração excluída no application.yml (7598265) | Corrigido |
| D03 | 2026-11-12 | CT32 | Orcamento.alterar lança NullPointerException (500) quando há item sem id e os conjuntos de itens mantidos são imutáveis (Set.of/Map.of) | Média | Consulta contains/containsKey com a chave nula do item; coleções imutáveis do Java recusam null (teste OrdemServicoTest#naoDeveCriarNovaVersaoQuandoAlteracaoDoOrcamentoAprovadoEhInvalida) | Tratamento da chave nula com `mantido`/`mantida` em Orcamento + teste de regressão (266bc7a) | Corrigido |
| D04 | 2026-11-15 | Auditoria A01 | Dependências com vulnerabilidades publicadas (Tomcat 10.1.31, Spring Security 6.3.4, Spring Framework 6.1.14, logback 1.5.11, commons-lang3 3.14.0) e Spring Boot 3.3 fora do suporte | Alta | Versões do projeto inicial nunca atualizadas (`mvn dependency:list`) | Spring Boot 3.5.7, springdoc 2.8.13, commons-lang3 3.18.0 (e5486d2) | Corrigido |
| D05 | 2026-11-15 | Auditoria A02 | MECANICO recebe CPF/CNPJ, telefone e e-mail do cliente em GET /api/ordens/{id} e GET /api/veiculos?placa= | Média | Respostas montadas sem considerar o perfil (DadosPessoaisMecanicoIntegracaoTest falhava) | Campos omitidos para MECANICO (790204a) | Corrigido |
| D06 | 2026-11-15 | Auditoria A03 | Log de violação de integridade contém valores do registro (ex.: CPF duplicado) | Média | Handler registrava `getMostSpecificCause().getMessage()` (TratadorDeErrosTest#naoDeveRegistrarNoLogOsValoresDaViolacaoDeIntegridade falhava) | Log só com SQLState e restrição (1c58d3d) | Corrigido |
| D07 | 2026-11-15 | Auditoria A04 | Quantidade enorme de peça ou total acima de NUMERIC(10,2) responde 409 "conflito" | Média | Sem limite na quantidade nem no total; o estouro chega ao banco (LimitesOrcamentoIntegracaoTest falhava com 409) | `@Max(9999)` (400) e limite do total no domínio (422) (edfb8ce) | Corrigido |
| D08 | 2026-11-15 | Auditoria A05 | Senha com acentos acima de 72 bytes: login aceita outra senha com o mesmo prefixo (200) e, no Spring Security 6.5, o cadastro dá 500 | Média | `@Size` conta caracteres; o BCrypt usa 72 bytes (AuthIntegracaoTest e UsuarioIntegracaoTest falhavam) | Validação em bytes UTF-8 no cadastro/edição e no login (f08679b) | Corrigido |
| D09 | 2026-11-15 | Auditoria A06 | Parâmetro `busca` sem tamanho máximo | Baixa | Sem validação no `@RequestParam` (LimitesDeBuscaIntegracaoTest falhava com 200) | `@Size(max = 100)` nas quatro listagens (a437b75) | Corrigido |
| D10 | 2026-11-15 | Frontend HU03 | Busca rápida por placa não buscava: botão "Buscar" e Enter não faziam nada | Alta | `<form (ngSubmit)>` sem `[formGroup]`: com só ReactiveFormsModule não há diretiva de formulário, então `ngSubmit` nunca é emitido (busca-placa.component.spec.ts falhava: nenhuma requisição a /api/veiculos) | Formulário ligado a um `FormGroup` (11d50af; encontrado pelo teste antes do primeiro commit do componente) | Corrigido |
| D11 | 2026-11-26 | CT62 | Campos de formulário encostando um no outro em todas as telas (o contorno do campo seguinte sobrepõe o de cima; visto no deploy) | Média | `MAT_FORM_FIELD_DEFAULT_OPTIONS` com `subscriptSizing: 'dynamic'` no app.config: a linha da mensagem de erro/dica fica com 0 px (app.config.spec.ts falhava: subscript dinâmico e 16 px a menos entre os campos) | Padrão `fixed`; `dynamic` só nas barras de busca/filtro de uma linha (7e97c7c) | Corrigido |
| D12 | 2026-11-26 | CT67 | Texto da busca do quadro ("Buscar placa ou cli…") cortado até clicar no campo; o mesmo nas buscas de clientes, catálogo e orçamento | Baixa | Rótulo flutuante (mat-label) ao lado do ícone de lupa em campo estreito: só aparece inteiro quando sobe com o foco (quadro.component.spec.ts falharia: havia rótulo e nenhum placeholder) | Placeholder com aria-label nos campos de busca (77394c9) | Corrigido |
| D13 | 2026-11-26 | CT67 | Paginador em inglês ("Items per page", "1 – 10 of 10") nas listas | Baixa | MatPaginatorIntl padrão do Material sem tradução | PaginadorIntl em português registrado no app.config + paginador-intl.spec.ts (cab64f1) | Corrigido |

## 8. Resumo final

Preenchido ao fim da Macro Entrega 3. Os casos da Macro Entrega 4 (CT58–CT68, seções 5.8 e 5.9) ficam fora deste resumo.

| Indicador | Valor |
| --- | --- |
| Casos planejados | 57 |
| Casos executados | 57 (52 automatizados + CT56 + CT53, CT54, CT55 e CT57 pelo roteiro em docs/roteiro-testes-manuais.md, 2026-11-25) |
| Aprovados | 57 |
| Reprovados / bloqueados | 0 / 0 |
| Defeitos encontrados / corrigidos | 10 / 10 (D01–D10) |
| Testes automatizados | API 383 (unitários + integração com PostgreSQL real), frontend 157 (Jasmine/Karma), 0 falhas (2026-11-16) |
| Cobertura do pacote `ordens` (JaCoCo) | 97,6% linhas / 87,0% ramos (total da API 96,4% / 86,2%; frontend 89,5% linhas), feature/15, 2026-11-16 |
