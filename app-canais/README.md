# IPTV Manager

Sistema web para gerenciamento e reproducao de listas IPTV legitimas, proprias ou autorizadas.

Este repositorio sera desenvolvido de forma atomica por funcionalidade, seguindo as instrucoes do arquivo `CONTEXT.md`.

## Stack prevista

- Backend: Java 21, Spring Boot 3, Spring Security, JWT, Spring Data JPA, PostgreSQL, Flyway e Maven.
- Frontend: Angular 18+, TypeScript, Angular Router, Reactive Forms e HLS.js.
- Infraestrutura: Docker, Docker Compose, PostgreSQL e Nginx opcional para producao.

## Estrutura

```txt
.
|-- backend/
|-- frontend/
|-- docker/
|-- docs/
|-- scripts/
|-- CONTEXT.md
`-- README.md
```

## Desenvolvimento por modulos

O desenvolvimento sera entregue em etapas pequenas e verificaveis:

1. Estrutura base do monorepo.
2. Backend base com Spring Boot, PostgreSQL, Flyway e Docker.
3. Autenticacao JWT e modulo de usuarios.
4. Modulo de listas IPTV.
5. Parser M3U/M3U8.
6. Importacao de canais.
7. Modulo de canais, busca, filtros e favoritos.
8. Dashboard.
9. Frontend base com Angular, layout e autenticacao.
10. Telas de listas, canais e favoritos.
11. Player HLS.
12. Area administrativa.
13. Docker Compose completo, documentacao, exemplos e testes finais.

## Backend local

O backend base fica em `backend/`.

Para rodar os testes:

```bash
cd backend
mvn test
```

Para executar a aplicacao localmente, configure um PostgreSQL acessivel pelas variaveis de `.env.example` e rode:

```bash
cd backend
mvn spring-boot:run
```

## Containers

Para subir PostgreSQL e backend via Docker Compose:

```bash
./scripts/start-containers.sh
```

O frontend fica disponivel em:

```txt
http://localhost:4200
```

O backend tambem fica exposto em:

```txt
http://localhost:8080
```

Para acompanhar logs:

```bash
./scripts/logs-containers.sh
```

Para parar os containers:

```bash
./scripts/stop-containers.sh
```

Health check do backend:

```bash
curl http://localhost:8080/api/health
```

## Autenticacao

Com os containers, o administrador inicial e criado pelas variaveis:

```txt
ADMIN_EMAIL=admin@admin.com
ADMIN_PASSWORD=admin123
```

Endpoints ja disponiveis:

```txt
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me

GET    /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
DELETE /api/users/{id}
```

Exemplo de login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@admin.com","password":"admin123"}'
```

## Listas IPTV

Endpoints ja disponiveis:

```txt
POST   /api/iptv-lists/url
POST   /api/iptv-lists/upload
GET    /api/iptv-lists
GET    /api/iptv-lists/{id}
PUT    /api/iptv-lists/{id}
DELETE /api/iptv-lists/{id}
```

Exemplo de cadastro por URL:

```bash
curl -X POST http://localhost:8080/api/iptv-lists/url \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN" \
  -d '{"name":"Minha lista","sourceUrl":"https://example.com/list.m3u8"}'
```

Exemplo de upload:

```bash
curl -X POST http://localhost:8080/api/iptv-lists/upload \
  -H "Authorization: Bearer SEU_TOKEN" \
  -F "name=Minha lista local" \
  -F "file=@docs/examples/sample.m3u"
```

## Parser M3U/M3U8

O backend ja possui `M3uParserService`, preparado para extrair canais de listas `.m3u` e `.m3u8`. A importacao para o banco entra no proximo modulo atomico.

## Canais e importacao

Endpoints disponiveis:

```txt
POST /api/iptv-lists/{id}/import
POST /api/iptv-lists/{id}/refresh

GET    /api/channels
GET    /api/channels/{id}
GET    /api/iptv-lists/{listId}/channels
PUT    /api/channels/{id}
DELETE /api/channels/{id}

GET    /api/channels/search?term=
GET    /api/channels/groups
GET    /api/channels/group/{groupName}

POST   /api/channels/{id}/favorite
DELETE /api/channels/{id}/favorite
GET    /api/channels/favorites
```

## Dashboard

```txt
GET /api/dashboard
```

## Frontend

O frontend Angular possui:

- Login e cadastro.
- Dashboard.
- Listagem e cadastro de listas por URL ou upload.
- Detalhes da lista e acionamento de importacao/refresh.
- Listagem de canais, busca e favoritos.
- Player HLS.
- Area administrativa de usuarios.

Para rodar localmente:

```bash
cd frontend
npm install
npm start
```

Para build:

```bash
cd frontend
npm run build
```

## Testes e validacao

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm run build
```

Arquivo HTTP para testes manuais:

```txt
docs/api.http
```

## Observacoes legais e de seguranca

Este sistema e apenas um gerenciador/reprodutor de listas IPTV. Ele nao fornece canais, conteudo, streams ou listas. O usuario e responsavel por adicionar apenas listas e conteudos que tenha direito de acessar.

O sistema nao deve burlar autenticacao, DRM, paywall ou qualquer mecanismo de protecao.
