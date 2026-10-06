# 🎓 Certifique-se — API Backend

> API REST responsável pela gestão, armazenamento e disponibilização de certificados acadêmicos e profissionais.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge\&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=for-the-badge\&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-green?style=for-the-badge\&logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge\&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Containers-blue?style=for-the-badge\&logo=docker)

---

## 📌 Sobre

O **Certifique-se** centraliza certificados acadêmicos e profissionais em um único ambiente, permitindo seu armazenamento, organização, consulta e compartilhamento através de um portfólio público.

## 🛠️ Tecnologias

* **Backend:** Java 21 + Spring Boot
* **Segurança:** Spring Security + JWT
* **Banco de Dados:** PostgreSQL
* **Armazenamento:** Cloud Storage para certificados PDF/JPG/PNG
* **Containerização:** Docker + Docker Compose
* **Frontend:** Angular — [Repositório Web](https://github.com/luanrichardsz/certifique-se-web)

## 🔐 API REST

A API utiliza autenticação **JWT stateless**, garantindo que operações relacionadas aos dados do usuário sejam realizadas mediante autenticação e autorização.

Principais recursos:

| Método   | Endpoint                 | Função                               |
| :------- | :----------------------- | :----------------------------------- |
| `POST`   | `/api/auth/login`        | Autenticação e emissão do token JWT  |
| `POST`   | `/api/usuarios`          | Cadastro de usuário                  |
| `GET`    | `/api/certificados`      | Consulta e filtragem de certificados |
| `POST`   | `/api/certificados`      | Cadastro e upload de certificado     |
| `DELETE` | `/api/certificados/{id}` | Exclusão de certificado              |

> **Autenticação e autorização são aplicadas aos recursos privados da aplicação, garantindo que cada usuário tenha acesso apenas aos seus próprios dados.**

## 💻 Executando localmente

```bash
# Clone o repositório
git clone https://github.com/luanrichardsz/Certifique-se.git

# Acesse o projeto
cd Certifique-se

# Suba o ambiente
docker-compose up -d
```
