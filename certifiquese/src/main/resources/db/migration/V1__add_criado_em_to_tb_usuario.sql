ALTER TABLE tb_usuario ADD COLUMN criado_em timestamp;

UPDATE tb_usuario
SET criado_em = NOW()
WHERE criado_em IS NULL;

ALTER TABLE tb_usuario ALTER COLUMN criado_em SET NOT NULL;