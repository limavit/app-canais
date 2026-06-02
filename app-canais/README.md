# IPTV Manager

Sistema web para gerenciamento e reproducao de listas IPTV legitimas, proprias ou autorizadas.

O projeto segue desenvolvimento atomico por funcionalidade. O arquivo `CONTEXT.md` e usado como contexto local de desenvolvimento e esta ignorado pelo Git.

## Stack

- Backend: Java 21, Spring Boot 3, Spring Security, JWT, Spring Data JPA, PostgreSQL, Flyway e Maven.
- Frontend: Angular, TypeScript, Angular Router, Reactive Forms e HLS.js.
- Infraestrutura: Docker, Docker Compose, PostgreSQL e Nginx para servir o frontend.

## Estrutura

```txt
.
|-- backend/
|-- frontend/
|-- docs/
|-- scripts/
|-- docker-compose.yml
|-- .env.example
`-- README.md
```

## Funcionalidades

- Cadastro, login, logout, refresh token e rota `me`.
- Perfis `ADMIN` e `USER`.
- Administrador inicial configuravel por variaveis de ambiente.
- Cadastro de listas IPTV por URL ou upload `.m3u/.m3u8`.
- Upload com limite configurado para arquivos grandes.
- Importacao e refresh de listas em background.
- Parser M3U/M3U8 com categoria, logo, tvg-id, tvg-name e URL do stream.
- Listagem paginada de canais com busca, categoria, lista de origem e favoritos.
- Remocao de listas e canais.
- Player HLS integrado.
- Teste individual de canal.
- Teste em lote dos canais filtrados, com fila e limite de concorrencia.
- Persistencia do ultimo status de teste do canal.
- Filtro por status de teste: nao testado, online, offline, invalido e indefinido.
- Dashboard e area administrativa de usuarios.

## Variaveis de ambiente

Copie o exemplo antes de subir os containers:

```bash
cp .env.example .env
```

Edite principalmente:

```txt
POSTGRES_PASSWORD
JWT_SECRET
ADMIN_EMAIL
ADMIN_PASSWORD
```

Variaveis uteis para arquivos grandes e concorrencia:

```txt
MAX_FILE_SIZE=100MB
MAX_REQUEST_SIZE=100MB
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
TOMCAT_MAX_THREADS=200
TOMCAT_MIN_SPARE_THREADS=20
```

Importante: `.env`, `CONTEXT.md`, chaves, certificados e arquivos locais de producao estao ignorados no Git. Nao versionar segredos reais.

## Containers

Subir tudo com build:

```bash
./scripts/start-containers.sh
```

Recriar os containers depois de alteracoes:

```bash
./scripts/restart-containers.sh
```

Acompanhar logs:

```bash
./scripts/logs-containers.sh
```

Parar:

```bash
./scripts/stop-containers.sh
```

URLs:

```txt
Frontend: http://localhost:4200
Backend:  http://localhost:8080
Health:   http://localhost:8080/api/health
```

Health check:

```bash
curl http://localhost:8080/api/health
```

## Acesso inicial

Por padrao do `.env.example`:

```txt
ADMIN_EMAIL=admin@admin.com
ADMIN_PASSWORD=admin123
```

Troque esses valores no `.env` antes de usar fora do ambiente local.

Login via API:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@admin.com","password":"admin123"}'
```

## API principal

Autenticacao:

```txt
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
```

Usuarios:

```txt
GET    /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
DELETE /api/users/{id}
```

Listas IPTV:

```txt
POST   /api/iptv-lists/url
POST   /api/iptv-lists/upload
GET    /api/iptv-lists
GET    /api/iptv-lists/{id}
PUT    /api/iptv-lists/{id}
DELETE /api/iptv-lists/{id}
POST   /api/iptv-lists/{id}/import
POST   /api/iptv-lists/{id}/refresh
GET    /api/iptv-lists/{id}/channels
```

Canais:

```txt
GET    /api/channels
GET    /api/channels/{id}
PUT    /api/channels/{id}
DELETE /api/channels/{id}
GET    /api/channels/search?term=
GET    /api/channels/groups
GET    /api/channels/group/{groupName}
POST   /api/channels/{id}/favorite
DELETE /api/channels/{id}/favorite
GET    /api/channels/favorites
POST   /api/channels/{id}/test
POST   /api/channels/test-batch
```

Dashboard:

```txt
GET /api/dashboard
```

Exemplo de cadastro de lista por URL:

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

Exemplo de teste em lote dos canais offline de uma lista:

```bash
curl -X POST "http://localhost:8080/api/channels/test-batch?listId=1&testStatus=OFFLINE" \
  -H "Authorization: Bearer SEU_TOKEN"
```

## Como usar no frontend

1. Acesse `http://localhost:4200`.
2. Entre com o usuario admin ou crie uma conta.
3. Cadastre uma lista por URL ou upload.
4. Abra a lista e clique em `Importar`.
5. Enquanto estiver `PROCESSING`, a tela acompanha o status automaticamente.
6. Abra `Canais` para buscar, filtrar por categoria/lista/status, favoritar, testar ou remover canais.
7. Use `Testar filtrados` para validar varios canais com fila controlada.

## Desenvolvimento local

Backend:

```bash
cd backend
mvn spring-boot:run
```

Testes do backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm install
npm start
```

Build do frontend:

```bash
cd frontend
npm run build
```

Arquivo HTTP para testes manuais:

```txt
docs/api.http
```

## Banco de dados

O schema e versionado por Flyway em:

```txt
backend/src/main/resources/db/migration/
```

Ao subir o backend, as migrations sao aplicadas automaticamente. A migration `V5` adiciona os campos do ultimo teste dos canais.

## Observacoes de concorrencia

- Importacao e refresh de listas rodam em background.
- O teste em lote usa fila controlada e testa ate 20 canais em paralelo por lote.
- O teste de canal e feito do ponto de vista do backend/container.
- Alguns provedores podem bloquear checagens automaticas; nesses casos, um stream pode aparecer offline mesmo funcionando no player.

## Seguranca

- Nao versionar `.env`, chaves, certificados, tokens ou arquivos de producao.
- Trocar `JWT_SECRET`, `POSTGRES_PASSWORD` e `ADMIN_PASSWORD` antes de qualquer uso fora do local.
- Usar apenas listas IPTV proprias, legitimas ou autorizadas.

## Observacoes legais

Este sistema e apenas um gerenciador/reprodutor de listas IPTV. Ele nao fornece canais, conteudo, streams ou listas.

O usuario e responsavel por adicionar apenas listas e conteudos que tenha direito de acessar. O sistema nao deve ser usado para burlar autenticacao, DRM, paywall ou qualquer mecanismo de protecao.
