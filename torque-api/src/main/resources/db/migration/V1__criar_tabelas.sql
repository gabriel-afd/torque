-- Torque — schema inicial (Macro Entrega 2, Modelo de Dados)

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL UNIQUE,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL CHECK (perfil IN ('ADMINISTRADOR', 'ATENDENTE', 'MECANICO')),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE cliente (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    documento  VARCHAR(14)  NOT NULL UNIQUE,
    telefone   VARCHAR(20)  NOT NULL,
    email      VARCHAR(160),
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE veiculo (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT      NOT NULL REFERENCES cliente (id),
    placa       CHAR(7)     NOT NULL UNIQUE CHECK (placa ~ '^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$'),
    marca       VARCHAR(60) NOT NULL,
    modelo      VARCHAR(80) NOT NULL,
    ano         SMALLINT,
    cor         VARCHAR(30)
);

CREATE TABLE servico (
    id                 BIGSERIAL PRIMARY KEY,
    descricao          VARCHAR(150)  NOT NULL,
    preco_mao_de_obra  NUMERIC(10, 2) NOT NULL CHECK (preco_mao_de_obra >= 0),
    ativo              BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE peca (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(40)    NOT NULL UNIQUE,
    descricao    VARCHAR(150)   NOT NULL,
    preco_venda  NUMERIC(10, 2) NOT NULL CHECK (preco_venda >= 0),
    ativo        BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE SEQUENCE ordem_servico_numero_seq START 1000;

CREATE TABLE ordem_servico (
    id                 BIGSERIAL PRIMARY KEY,
    numero             BIGINT      NOT NULL UNIQUE DEFAULT nextval('ordem_servico_numero_seq'),
    veiculo_id         BIGINT      NOT NULL REFERENCES veiculo (id),
    mecanico_id        BIGINT      REFERENCES usuario (id),
    problema_relatado  TEXT        NOT NULL,
    quilometragem      INTEGER     CHECK (quilometragem >= 0),
    status             VARCHAR(30) NOT NULL CHECK (status IN ('ABERTA', 'AGUARDANDO_APROVACAO', 'APROVADA', 'EM_EXECUCAO',
                                                              'AGUARDANDO_PECA', 'CONCLUIDA', 'ENTREGUE', 'RECUSADA', 'CANCELADA')),
    data_abertura      TIMESTAMP   NOT NULL DEFAULT now(),
    data_entrega       TIMESTAMP,
    version            INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE orcamento (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT         NOT NULL REFERENCES ordem_servico (id),
    versao            SMALLINT       NOT NULL,
    desconto          NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (desconto >= 0),
    total             NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    situacao          VARCHAR(20)    NOT NULL CHECK (situacao IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'SUBSTITUIDO')),
    decidido_por_id   BIGINT         REFERENCES usuario (id),
    data_decisao      TIMESTAMP,
    UNIQUE (ordem_servico_id, versao)
);

CREATE TABLE orcamento_item_servico (
    id            BIGSERIAL PRIMARY KEY,
    orcamento_id  BIGINT         NOT NULL REFERENCES orcamento (id) ON DELETE CASCADE,
    servico_id    BIGINT         NOT NULL REFERENCES servico (id),
    descricao     VARCHAR(150)   NOT NULL,
    preco         NUMERIC(10, 2) NOT NULL CHECK (preco >= 0)
);

CREATE TABLE orcamento_item_peca (
    id              BIGSERIAL PRIMARY KEY,
    orcamento_id    BIGINT         NOT NULL REFERENCES orcamento (id) ON DELETE CASCADE,
    peca_id         BIGINT         NOT NULL REFERENCES peca (id),
    descricao       VARCHAR(150)   NOT NULL,
    quantidade      INTEGER        NOT NULL CHECK (quantidade > 0),
    preco_unitario  NUMERIC(10, 2) NOT NULL CHECK (preco_unitario >= 0)
);

CREATE TABLE historico_status (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT      NOT NULL REFERENCES ordem_servico (id),
    usuario_id        BIGINT      NOT NULL REFERENCES usuario (id),
    status_anterior   VARCHAR(30),
    status_novo       VARCHAR(30) NOT NULL,
    data_hora         TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ordem_servico_status ON ordem_servico (status);
CREATE INDEX idx_ordem_servico_data_abertura ON ordem_servico (data_abertura);
CREATE INDEX idx_ordem_servico_veiculo ON ordem_servico (veiculo_id);
CREATE INDEX idx_veiculo_cliente ON veiculo (cliente_id);
CREATE INDEX idx_historico_status_ordem ON historico_status (ordem_servico_id);
