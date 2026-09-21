# Integração com Cloudflare R2

Este documento descreve a integração do backend do Certifique-se com o Cloudflare R2 para armazenar as imagens dos certificados.

## O que foi implementado

- Inclusão do AWS SDK for Java v2, módulo S3, no `pom.xml`.
- Criação do `R2Config`, responsável por montar o cliente S3 compatível com o Cloudflare R2.
- Criação de um serviço de armazenamento para enviar, consultar e remover imagens.
- Endpoint autenticado para upload de imagens.
- Endpoint público para visualizar imagens de buckets privados.
- Remoção automática da imagem quando:
  - a foto de um certificado é substituída;
  - um certificado é excluído;
  - a conta de um usuário é excluída.
- Validação de formato e tamanho do arquivo.
- Tratamento de falhas de comunicação com o R2.
- Testes unitários e um teste de integração real com o bucket.
- Remoção da integração antiga com o Supabase Storage.

## Variáveis de ambiente

As seguintes variáveis são utilizadas:

```properties
R2_ACCOUNT_ID=identificador_da_conta_cloudflare
R2_ACCESS_KEY_ID=access_key_do_token_r2
R2_SECRET_ACCESS_KEY=secret_key_do_token_r2
R2_BUCKET_NAME=nome_do_bucket
R2_ENDPOINT=https://SEU_ACCOUNT_ID.r2.cloudflarestorage.com
R2_REGION=auto
```

O `R2_ACCOUNT_ID` normalmente é um identificador hexadecimal de 32 caracteres. O endpoint não deve conter o nome do bucket.

O token do R2 precisa ter, no bucket configurado, permissões para ler e gravar objetos. A exclusão de objetos também utiliza a permissão de escrita.

### URL pública opcional

Por padrão, o bucket pode continuar privado. Nesse caso, o backend devolve URLs no formato:

```text
/certificados/imagens/certificado-UUID.png
```

Essa rota busca o arquivo no R2 sem expor as credenciais.

Se o bucket estiver publicado por um domínio personalizado ou por uma URL de desenvolvimento `r2.dev`, adicione:

```properties
R2_PUBLIC_URL=https://arquivos.seudominio.com
```

Não coloque uma barra no final. Quando essa variável estiver configurada, o upload devolverá diretamente a URL pública do objeto. Para produção, recomenda-se um domínio personalizado; a URL `r2.dev` é voltada a desenvolvimento e possui limitação de tráfego.

## Upload de uma imagem

O upload exige autenticação JWT.

```http
POST /certificados/imagens
Authorization: Bearer TOKEN
Content-Type: multipart/form-data
```

O formulário precisa ter um campo chamado `arquivo`:

```bash
curl -X POST http://localhost:8080/certificados/imagens \
  -H "Authorization: Bearer SEU_TOKEN" \
  -F "arquivo=@/caminho/certificado.png"
```

Resposta:

```json
{
  "chave": "certificado-550e8400-e29b-41d4-a716-446655440000.png",
  "url": "/certificados/imagens/certificado-550e8400-e29b-41d4-a716-446655440000.png"
}
```

O frontend deve enviar o valor de `url` no campo `foto` ao cadastrar ou atualizar o certificado.

## Formatos e limites

São aceitos:

- JPEG (`image/jpeg`)
- PNG (`image/png`)
- WebP (`image/webp`)

O limite é de 5 MB por imagem. Cada objeto recebe um nome UUID para impedir colisões e evitar confiar no nome enviado pelo usuário.

## Leitura da imagem

Quando `R2_PUBLIC_URL` não está configurada, a leitura é feita por:

```http
GET /certificados/imagens/{chave}
```

Essa rota é pública porque os certificados podem aparecer em perfis públicos. Ela devolve o `Content-Type` original e cabeçalhos de cache por um ano, pois o nome UUID muda sempre que uma nova imagem é enviada.

## Remoção automática

A remoção do R2 acontece somente depois que a transação do banco é confirmada. Dessa forma, uma falha ou rollback no PostgreSQL não remove prematuramente uma imagem ainda referenciada.

Referências de imagens que não pertencem ao padrão gerado pela integração são ignoradas durante a limpeza. Isso evita excluir acidentalmente arquivos externos ou imagens antigas de outro provedor.

## Configuração técnica do cliente

O cliente usa:

- endpoint S3 da conta Cloudflare;
- região `auto`;
- credenciais estáticas do token R2;
- URLs em estilo path;
- `chunkedEncodingEnabled(false)`.

O chunked encoding foi desativado porque o Cloudflare R2 exige essa configuração no AWS SDK for Java v2 para evitar incompatibilidade de assinatura em operações `PutObject`.

## Testes

Executar a suíte normal:

```bash
./mvnw test
```

Executar manualmente o teste real do R2:

```bash
./mvnw -q -Dtest=R2LiveSmokeIT test
```

O teste real lê as credenciais do `.env`, envia uma imagem PNG mínima, lê o objeto e o remove em seguida. O nome `R2LiveSmokeIT` impede que ele seja executado automaticamente pela suíte normal.
