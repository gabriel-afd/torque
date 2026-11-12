# Torque

Sistema web de ordens de serviço para oficinas mecânicas de pequeno porte: cadastro de clientes e veículos, abertura de OS, orçamento com aprovação do cliente, quadro de acompanhamento por status e histórico de manutenção por placa.

Projeto da disciplina **Projeto Integrador - EAD** (Análise e Desenvolvimento de Sistemas).
Autor: Gabriel Medeiros de Mendonça.

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
├── docker-compose.yml         # banco de dados (api e web entram na Sprint 4)
├── .github/workflows/ci.yml   # build e testes a cada pull request
├── docs/                      # entregas da disciplina
├── torque-api/                # API REST — Spring Boot
└── torque-web/                # frontend — Angular
```

## Como executar (desenvolvimento)

```bash
# 1. Banco de dados
docker compose up -d db

# 2. API (http://localhost:8080, Swagger em /api/docs)
cd torque-api && mvn spring-boot:run

# 3. Frontend (http://localhost:4200)
cd torque-web && npm install && npm start
```

Variáveis de ambiente da API: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`.

## Entregas

| Macro Entrega | Conteúdo | Pasta |
| --- | --- | --- |
| 1 — Concepção, Planejamento e Engenharia de Requisitos | Visão, requisitos, histórias, backlog, plano de IA | `docs/entrega-1` |
| 2 — Arquitetura, Modelagem e Prototipação da Solução | Arquitetura, diagramas, modelo de dados, protótipos | `docs/entrega-2` |
| 3 — Desenvolvimento, Integração e Garantia da Qualidade | Código, testes e registro de IA | em andamento |
| 4 — Deployment, Documentação e Apresentação Final | Aplicação implantada e manuais | a fazer |

## Padrões

- Branch `main` protegida; uma branch por item do backlog (`feature/07-abertura-os`), merge por pull request com CI verde.
- Commits no padrão Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`).
