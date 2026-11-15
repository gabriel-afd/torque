# Auditoria de segurança e qualidade — torque-api

Data: 2026-11-15 · Branch: `feature/08-auditoria-backend` · Base: código até o prompt 07.

## Escopo e método

- Leitura de todos os controllers, services, DTOs, entidades, configuração de segurança/JWT e tratador de erros.
- Conferência de cada endpoint contra a tabela de perfis do `CLAUDE.md` (36 rotas; 35 com `@PreAuthorize`, só `POST /api/auth/login` é público).
- Busca por regra de negócio fora do domínio, validação faltando, dados sensíveis em respostas e logs, N+1, paginação, transações e tratamento de erro.
- Dependências: `mvn dependency:list` (runtime) comparado com os avisos de segurança publicados.
- Cada problema confirmado ganhou um teste que falhava antes da correção (ver seção **Defeitos** do `TestCasePlan.md`).

## O que está correto

- Todos os endpoints têm `@PreAuthorize` com o perfil da tabela; MECANICO restrito no quadro e nas transições pelo domínio (RN09).
- JWT: chave mínima de 32 bytes vinda de `JWT_SECRET`, expiração, perfil lido do banco a cada requisição e usuário inativo barrado.
- Login com mensagem única e tempo equalizado (hash fictício quando o e-mail não existe).
- Senhas só em BCrypt; `toString` dos requests mascara a senha; entidades nunca saem pela API.
- Regras RN04–RN10 no domínio; services só orquestram; `@Version` com `OPTIMISTIC_FORCE_INCREMENT`.
- Listagens paginadas com `max-page-size: 100`; quadro e histórico sem N+1 (conferido no prompt 06).
- Erros sempre no formato `{status, erro, mensagem, campos}`, 500 sem stack trace.

## Achados

| ID | Arquivo / linha | Problema | Severidade | Correção proposta | Situação |
| --- | --- | --- | --- | --- | --- |
| A01 | `pom.xml:10,37` | Spring Boot 3.3.5 fora do suporte; dependências com CVEs publicadas: Tomcat 10.1.31 (CVE-2024-50379, CVE-2025-24813, CVE-2025-48988), Spring Security 6.3.4 (CVE-2025-22228), Spring Framework 6.1.14 (CVE-2024-38820), logback 1.5.11 (CVE-2024-12798), commons-lang3 3.14.0 (CVE-2025-48924) | Alta | Spring Boot 3.5.7 (Tomcat 10.1.48, Security 6.5.6, Framework 6.2.12, logback 1.5.20), springdoc 2.8.13 e commons-lang3 3.18.0 | Corrigido (D04) |
| A02 | `ordens/dto/OrdemServicoResponse.java:43`, `clientes/dto/VeiculoComDonoResponse.java:14` | MECANICO recebe CPF/CNPJ, telefone e e-mail do cliente no detalhe da OS e na busca por placa (dados pessoais além do necessário, LGPD) | Média | Omitir esses campos quando o usuário é MECANICO (continua vendo o nome) | Corrigido (D05) |
| A03 | `shared/web/TratadorDeErros.java:162` | Violação de integridade grava no log a mensagem do PostgreSQL, que contém os valores (ex.: `Chave (documento)=(…)`) | Média | Registrar só o SQLState e o nome da restrição | Corrigido (D06) |
| A04 | `ordens/dto/OrcamentoRequest.java:32`, `ordens/Orcamento.java:281` | Quantidade de peça sem limite superior; total acima de `NUMERIC(10,2)` estoura no banco e a API responde 409 "conflito" | Média | `@Max(9999)` na quantidade (400) e regra no domínio para total acima de R$ 99.999.999,99 (422) | Corrigido (D07) |
| A05 | `auth/dto/UsuarioRequest.java:13`, `auth/AuthService.java:35` | `@Size(max = 72)` conta caracteres, mas o BCrypt usa só 72 **bytes**: senha com acentos é truncada e o login aceita outra senha com o mesmo prefixo; após a atualização do Spring Security o cadastro passaria a dar 500 | Média | Validar o tamanho em bytes UTF-8 no cadastro/edição (400) e recusar no login (401) | Corrigido (D08) |
| A06 | `ClienteController`, `OrdemServicoController`, `PecaController`, `ServicoController` (parâmetro `busca`) | Termo de busca sem tamanho máximo (LIKE com textos arbitrariamente grandes) | Baixa | `@Size(max = 100)` no parâmetro | Corrigido (D09) |
| A07 | `auth/AuthController.java:25` | Login sem limite de tentativas (força bruta) | Média | Limitar por IP/e-mail no proxy reverso de produção (ex.: nginx `limit_req`) ou com bucket em memória | Não corrigido: depende da infraestrutura de implantação, fora do escopo desta entrega |
| A08 | `auth/SegurancaConfig.java` | Sem configuração de CORS | Baixa | No desenvolvimento o Angular usa proxy (`/api` → `:8080`); em produção, servir web e API no mesmo domínio | Não corrigido na API (tratado no frontend, prompt 09) |
| A09 | `auth/SegurancaConfig.java:30` | Swagger público em qualquer ambiente | Baixa | Desligar `springdoc` por variável em produção | Não corrigido: documentação pública é útil na avaliação |
| A10 | `ordens/OrdemServicoController.java:69` | MECANICO consegue ver o detalhe de OS não atribuída a ele | Baixa | Restringir como no quadro | Não corrigido: o `CLAUDE.md` define `GET /api/ordens/{id}` para todos; dados pessoais já protegidos por A02 |
| A11 | `ordens/OrdemServicoService.java` (`montarAlteracao`) | Checagem de item repetido no request fica no service | Baixa | Mover para `Orcamento.Alteracao` | Não corrigido: é validação de entrada, não regra de negócio; candidato ao prompt 14 (refatoração) |
