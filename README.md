# Torque

Sistema web de ordens de serviço para oficinas mecânicas de pequeno porte: cadastro de clientes e veículos, abertura de OS, orçamento com aprovação do cliente, quadro de acompanhamento por status e histórico de manutenção por placa.

Projeto da disciplina **Projeto Integrador - EAD** (Análise e Desenvolvimento de Sistemas).
Autor: Gabriel Medeiros de Mendonça.

## Telas

Visual "Grafite e Lima" (protótipos em `docs/prototipos/grafite-e-lima/`). Prints tirados do sistema rodando com os dados fictícios do perfil `demo`.

**Quadro de OS:** resumo do dia e colunas por status. Os cartões podem ser arrastados entre colunas, e o sistema só aceita os movimentos permitidos pelas regras de negócio.

![Quadro de OS](docs/prints/quadro.png)

**Ordem de serviço:** orçamento com serviços e peças do catálogo, resumo com desconto, ações conforme o status e o perfil, mecânico responsável e histórico.

![Ordem de serviço](docs/prints/ordem-de-servico.png)

| Login | Clientes |
| --- | --- |
| ![Login](docs/prints/login.png) | ![Clientes](docs/prints/clientes.png) |
| **Novo cliente (diálogo)** | **Painel do mês** |
| ![Novo cliente](docs/prints/novo-cliente.png) | ![Painel do mês](docs/prints/painel.png) |

**No celular:** menu lateral vira gaveta, e o quadro rola de lado dentro da área das colunas.

| Quadro | Menu | Ordem de serviço |
| --- | --- | --- |
| ![Quadro no celular](docs/prints/celular-quadro.png) | ![Menu no celular](docs/prints/celular-menu.png) | ![OS no celular](docs/prints/celular-os.png) |

## Tecnologias

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 21, Spring Boot 3 (Web, Data JPA, Validation, Security), JWT |
| Banco de dados | PostgreSQL 16, migrações com Flyway |
| Frontend | Angular 18 |
| Testes | JUnit 5, Mockito, Testcontainers; Jasmine/Karma |
| Infraestrutura | Docker Compose, GitHub Actions |

## Estrutura

```
torque/
├── Dockerfile                 # imagem única para deploy: Angular + API
├── render.yaml                # blueprint do Render (deploy gratuito)
├── docker-compose.yml         # banco de dados e, opcionalmente, a aplicação
├── .github/workflows/ci.yml   # build e testes a cada pull request
├── docs/                      # entregas da disciplina, protótipos e prints das telas
├── torque-api/                # API REST — Spring Boot
└── torque-web/                # frontend — Angular
```

## Como executar (desenvolvimento)

Pré-requisitos: Java 21+, Node 20+, Docker. O Maven não precisa estar instalado: use o wrapper (`mvnw`).

```bash
# 1. Banco de dados
docker compose up -d db

# 2. API (http://localhost:8080, Swagger em /api/docs)
cd torque-api
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 3. Frontend (http://localhost:4200; /api é redirecionado para :8080 via proxy.conf.json)
cd torque-web
npm install
npm start
```

### Primeiro acesso

A API só sobe com `JWT_SECRET` (mínimo de 32 bytes). Com o banco vazio, ela cria o primeiro administrador a partir de `ADMIN_EMAIL` e `ADMIN_SENHA` (mínimo de 8 caracteres):

```bash
# Linux/macOS (Git Bash no Windows funciona igual)
export JWT_SECRET="troque-por-um-segredo-com-mais-de-32-bytes"
export ADMIN_EMAIL="admin@suaoficina.com.br"
export ADMIN_SENHA="uma-senha-forte"
cd torque-api && ./mvnw spring-boot:run
```

No log aparece `Administrador inicial criado: admin@...`. Abra http://localhost:4200, entre com esse e-mail e senha e cadastre os demais usuários em **Usuários**. Opcional: `OFICINA_NOME` define o nome impresso no PDF e na mensagem do WhatsApp.

### Dados de demonstração (perfil `demo`)

Para teste manual e apresentação, o perfil `demo` carrega dados fictícios **num banco sem clientes**: 1 atendente, 2 mecânicos, catálogo, 10 clientes, 15 veículos e OS em todos os status (com histórico e uma OS com orçamento na versão 2). Se não houver administrador, cria `admin.demo@oficina.teste`.

```bash
export SPRING_PROFILES_ACTIVE=demo   # além de JWT_SECRET e das variáveis do banco
cd torque-api && ./mvnw spring-boot:run
```

| Usuário | Perfil | Senha |
| --- | --- | --- |
| ana.demo@oficina.teste | Atendente | `demo12345` (ou `TORQUE_DEMO_SENHA`) |
| raimundo.demo@oficina.teste, tiago.demo@oficina.teste | Mecânico | idem |
| admin.demo@oficina.teste (só se não houver admin) | Administrador | idem |

Nunca ative o perfil `demo` numa oficina real. No deploy de demonstração ele é usado de propósito, com `TORQUE_DEMO_SENHA` própria (ver Deploy).

## Testes

```bash
# API: testes unitários e de integração (Testcontainers sobe um PostgreSQL 16; Docker precisa estar rodando)
cd torque-api && ./mvnw -B verify
# relatório de cobertura: torque-api/target/site/jacoco/index.html

# Sem Docker? Aponte os testes de integração para um PostgreSQL DESCARTÁVEL (os dados são apagados a cada teste):
#   TORQUE_TEST_DB_URL=jdbc:postgresql://localhost:55432/torque_teste TORQUE_TEST_DB_USER=torque ./mvnw -B verify

# Frontend
cd torque-web && npm run build && npm test -- --watch=false --browsers=ChromeHeadless
```

## Variáveis de ambiente da API

| Variável | Uso | Padrão em desenvolvimento |
| --- | --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/torque` |
| `DB_USER` | usuário do banco | `torque` |
| `DB_PASSWORD` | senha do banco | `torque` |
| `JWT_SECRET` | chave de assinatura do JWT — **obrigatória**, mínimo 32 bytes; a API não sobe sem ela | — |
| `JWT_EXPIRACAO_HORAS` | validade do token | `8` |
| `ADMIN_EMAIL` | e-mail do primeiro administrador (criado só se não houver nenhum usuário) | — |
| `ADMIN_SENHA` | senha do primeiro administrador (mínimo 8 caracteres) | — |
| `OFICINA_NOME` | nome impresso no PDF e na mensagem do WhatsApp | `Oficina Torque` |
| `PORT` | porta HTTP (informada pela plataforma de deploy) | `8080` |

Primeira subida em desenvolvimento (bash):

```bash
export JWT_SECRET="uma-chave-local-com-pelo-menos-32-bytes"
export ADMIN_EMAIL="admin@oficina.local" ADMIN_SENHA="troque-esta-senha"
cd torque-api && ./mvnw spring-boot:run
```

Depois que o administrador existir, `ADMIN_EMAIL`/`ADMIN_SENHA` podem ser removidas (nada é recriado se já houver usuários).

## Deploy (gratuito: Render + Neon)

Imagem única: o `Dockerfile` da raiz compila o Angular, copia o build para `static/` da API e gera um só
`.jar`. O Spring Boot serve o frontend e a API na mesma origem (`/api`), sem CORS. Rotas do Angular
(`/quadro`, `/ordens/17`) recebem o `index.html` (`shared.web.FrontendConfig`). A API continua exigindo JWT.

**1. Banco no [Neon](https://neon.tech)** (plano Free, não expira)

1. Crie o projeto na região **AWS US East (N. Virginia)**.
2. Em *Connect*, copie a connection string, por exemplo
   `postgresql://usuario:senha@ep-xxxx.us-east-1.aws.neon.tech/neondb?sslmode=require`.
3. Separe as partes para a API:
   - `DB_URL` = `jdbc:postgresql://ep-xxxx.us-east-1.aws.neon.tech/neondb?sslmode=require`
   - `DB_USER` = `usuario`
   - `DB_PASSWORD` = `senha`

O Flyway cria as tabelas na primeira subida.

**2. Aplicação no [Render](https://render.com)** (Web Service Free)

1. *New → Blueprint* e escolha este repositório. O Render lê o `render.yaml`.
2. Preencha `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_SENHA` e `TORQUE_DEMO_SENHA`. O `JWT_SECRET` é gerado pelo Render.
3. *Apply*. O build leva alguns minutos. A aplicação fica em `https://torque-xxxx.onrender.com`
   (Swagger em `/api/docs`).
4. Cada merge na `main` gera um novo deploy (`autoDeploy`).

**Carga inicial:** o blueprint ativa o perfil `demo` (`SPRING_PROFILES_ACTIVE=demo`). Assim, a primeira subida
já preenche o banco com dados fictícios: atendente, 2 mecânicos, catálogo, 10 clientes, 15 veículos e 17 OS em
todos os status, com histórico. A carga só roda com o banco sem clientes, então reinícios e novos deploys não
duplicam nada. Os usuários demo (`ana.demo@oficina.teste`, `raimundo.demo@…`, `tiago.demo@…`) usam a senha de
`TORQUE_DEMO_SENHA`. Como a URL é pública, não use a padrão `demo12345`. O administrador é o de `ADMIN_EMAIL`.
Para começar com o banco vazio, remova `SPRING_PROFILES_ACTIVE` antes do primeiro deploy.

Limites do plano gratuito:
- O serviço dorme após cerca de 15 min sem acesso, e o primeiro acesso depois disso leva de 30 a 60 s.
- 512 MB de RAM. A imagem já limita o heap (`JAVA_OPTS` no `Dockerfile`). Medido localmente: cerca de 330 MB após a subida.

**Rodar a imagem localmente** (com Docker):

```bash
JWT_SECRET="uma-chave-local-com-pelo-menos-32-bytes" ADMIN_EMAIL="admin@oficina.local" ADMIN_SENHA="troque-esta-senha" \
  docker compose up -d --build      # http://localhost:8080
```

## Entregas

| Macro Entrega | Conteúdo | Pasta |
| --- | --- | --- |
| 1 — Concepção, Planejamento e Engenharia de Requisitos | Visão, requisitos, histórias, backlog, plano de IA | `docs/entrega-1` |
| 2 — Arquitetura, Modelagem e Prototipação da Solução | Arquitetura, diagramas, modelo de dados, protótipos | `docs/entrega-2` |
| 3 — Desenvolvimento, Integração e Garantia da Qualidade | Código, testes e registro de IA | em andamento |
| 4 — Deployment, Documentação e Apresentação Final | Aplicação implantada e manuais | em andamento (deploy: ver seção Deploy) |

## Padrões

- Branch `main` protegida; uma branch por item do backlog (`feature/07-abertura-os`), merge por pull request com CI verde.
- Commits no padrão Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`).
