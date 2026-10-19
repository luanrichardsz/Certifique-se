-- Adicionar coluna de foto de perfil para o usuário
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS foto VARCHAR(500);
