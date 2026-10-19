-- ====================================================================
-- CERTIFIQUE-SE - SCHEMA COMPLETO DO BANCO DE DADOS (SUPABASE / POSTGRESQL)
-- ====================================================================
-- Este script cria todas as tabelas, relacionamentos e índices de todas as
-- entidades do projeto Certifique-se de forma 100% compatível com o Hibernate/Flyway.
-- Pode ser executado diretamente no SQL Editor do Supabase.

-- 1. TABELA tb_usuario (UsuarioEntity)
CREATE TABLE IF NOT EXISTS tb_usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    nome_usuario VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    headline VARCHAR(150),
    biografia VARCHAR(255),
    foto VARCHAR(500),
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    perfil_publico BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    token_version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_username UNIQUE (username)
);

-- 2. TABELA tb_certificado (CertificadoEntity)
CREATE TABLE IF NOT EXISTS tb_certificado (
    id_certificado BIGSERIAL PRIMARY KEY,
    hash_certificado VARCHAR(255) NOT NULL,
    foto VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    empresa VARCHAR(255) NOT NULL,
    data_conclusao DATE NOT NULL,
    carga_horaria INTEGER,
    descricao VARCHAR(2000),
    link_validacao VARCHAR(500),
    publico BOOLEAN NOT NULL DEFAULT TRUE,
    id_usuario BIGINT NOT NULL,
    CONSTRAINT uk_certificado_hash UNIQUE (hash_certificado),
    CONSTRAINT fk_certificado_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuario (id_usuario) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_certificado_usuario_publico 
    ON tb_certificado (id_usuario, publico);

-- 3. TABELA tb_certificado_tag (ElementCollection em CertificadoEntity)
CREATE TABLE IF NOT EXISTS tb_certificado_tag (
    id_certificado BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    CONSTRAINT fk_certificado_tag_certificado FOREIGN KEY (id_certificado) REFERENCES tb_certificado (id_certificado) ON DELETE CASCADE
);

-- 4. TABELA tb_recuperacao_senha (RecuperacaoSenhaEntity)
CREATE TABLE IF NOT EXISTS tb_recuperacao_senha (
    id_recuperacao_senha BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    hash_token VARCHAR(64) NOT NULL UNIQUE,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    expira_em TIMESTAMP NOT NULL,
    utilizado_em TIMESTAMP NULL,
    CONSTRAINT fk_recuperacao_senha_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuario (id_usuario) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recuperacao_senha_usuario_utilizado_em
    ON tb_recuperacao_senha (id_usuario, utilizado_em);

-- 5. REGISTRO NO HISTÓRICO DO FLYWAY (flyway_schema_history)
-- Garante que o Flyway reconheça todas as versões (V0 a V5) como aplicadas
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx ON flyway_schema_history (success);
