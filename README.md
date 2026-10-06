# 🎓 Certifique-se — API Backend (Spring Boot & Cloud)

> Plataforma para centralização, gestão e disponibilização de portfólio público de certificados acadêmicos e profissionais.

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-green?style=for-the-badge&logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Containers-blue?style=for-the-badge&logo=docker)

---

## 📌 Sobre o Projeto
Muitos estudantes e profissionais perdem comprovantes de cursos em pastas do computador, e-mails antigos ou drives em nuvem sem organização. O **Certifique-se** resolve essa dor ao oferecer um painel centralizado para envio, categorização por tags, busca por emissor e geração de links de verificação.

## 🛠️ Arquitetura & Tecnologias
- **Linguagem & Framework:** Java 17 + Spring Boot
- **Segurança:** Spring Security com autenticação Stateless via Tokens JWT
- **Banco de Dados:** PostgreSQL (modelagem relacional com migrations e queries otimizadas)
- **Armazenamento de Arquivos:** Integração com Storage Cloud para upload e gestão dos certificados (PDF/JPG/PNG)
- **Containerização:** Docker e Docker Compose para padronização de ambiente
- **Frontend Consumidor:** Angular CLI ([Acesse o repositório Web](https://github.com/luanrichardsz/certifique-se-web))

## 🚀 Endpoints Principais (API REST)

| Verbo | Endpoint | Descrição | Protegido |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/auth/login` | Autenticação de usuário e retorno de JWT | ❌ |
| `POST` | `/api/usuarios` | Cadastro de novo usuário | ❌ |
| `GET` | `/api/certificados` | Listagem paginada de certificados com filtros (nome, tag, emissor) | 🔐 |
| `POST` | `/api/certificados` | Upload e cadastro de novo certificado | 🔐 |
| `DELETE` | `/api/certificados/{id}` | Remoção segura de certificado do usuário | 🔐 |

## 💻 Como Rodar a Aplicação Localmente

```bash
# 1. Clone o repositório
git clone [https://github.com/luanrichardsz/Certifique-se.git](https://github.com/luanrichardsz/Certifique-se.git)

# 2. Acesse a pasta
cd Certifique-se

# 3. Suba o ambiente via Docker Compose (PostgreSQL + App)
docker-compose up -d
