CREATE TABLE tb_usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    nome_usuario VARCHAR(255) NOT NULL,
    biografia VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE tb_certificado (
    id_certificado BIGSERIAL PRIMARY KEY,
    hash_certificado VARCHAR(255) NOT NULL,
    foto VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    empresa VARCHAR(255) NOT NULL,
    data_conclusao DATE NOT NULL,
    id_usuario BIGINT NOT NULL,
    CONSTRAINT uk_certificado_hash UNIQUE (hash_certificado),
    CONSTRAINT fk_certificado_usuario
        FOREIGN KEY (id_usuario) REFERENCES tb_usuario (id_usuario)
);

CREATE TABLE tb_certificado_tag (
    id_certificado BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    CONSTRAINT fk_certificado_tag_certificado
        FOREIGN KEY (id_certificado) REFERENCES tb_certificado (id_certificado)
);
