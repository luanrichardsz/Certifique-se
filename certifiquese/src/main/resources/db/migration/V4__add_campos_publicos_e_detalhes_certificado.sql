-- 1. Novos campos em tb_usuario
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS username VARCHAR(50);
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS headline VARCHAR(150);
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS perfil_publico BOOLEAN NOT NULL DEFAULT TRUE;

-- Preencher username para usuários existentes a partir do email (caso haja registros)
UPDATE tb_usuario 
SET username = LOWER(SPLIT_PART(email, '@', 1))
WHERE username IS NULL;

-- Garantir unicidade e obrigatoriedade do username
ALTER TABLE tb_usuario ALTER COLUMN username SET NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_usuario_username') THEN
        ALTER TABLE tb_usuario ADD CONSTRAINT uk_usuario_username UNIQUE (username);
    END IF;
END $$;

-- 2. Novos campos em tb_certificado
ALTER TABLE tb_certificado ADD COLUMN IF NOT EXISTS carga_horaria INTEGER;
ALTER TABLE tb_certificado ADD COLUMN IF NOT EXISTS descricao VARCHAR(2000);
ALTER TABLE tb_certificado ADD COLUMN IF NOT EXISTS link_validacao VARCHAR(500);
ALTER TABLE tb_certificado ADD COLUMN IF NOT EXISTS publico BOOLEAN NOT NULL DEFAULT TRUE;

-- Índice para consultas públicas de certificados por usuário
CREATE INDEX IF NOT EXISTS idx_certificado_usuario_publico ON tb_certificado (id_usuario, publico);
