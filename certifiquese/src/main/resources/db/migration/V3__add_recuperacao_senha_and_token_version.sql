ALTER TABLE tb_usuario
ADD COLUMN token_version INTEGER;

UPDATE tb_usuario
SET token_version = 0
WHERE token_version IS NULL;

ALTER TABLE tb_usuario
ALTER COLUMN token_version SET NOT NULL;

ALTER TABLE tb_usuario
ALTER COLUMN token_version SET DEFAULT 0;

CREATE TABLE tb_recuperacao_senha (
    id_recuperacao_senha BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    hash_token VARCHAR(64) NOT NULL UNIQUE,
    criado_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    utilizado_em TIMESTAMP NULL,
    CONSTRAINT fk_recuperacao_senha_usuario FOREIGN KEY (id_usuario) REFERENCES tb_usuario (id_usuario) ON DELETE CASCADE
);

CREATE INDEX idx_recuperacao_senha_usuario_utilizado_em
    ON tb_recuperacao_senha (id_usuario, utilizado_em);