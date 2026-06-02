# Plano de desenvolvimento atomico

Este arquivo registra a ordem de construcao do projeto. Cada modulo deve sair com escopo claro, verificacao proporcional e sem misturar funcionalidades de modulos futuros.

## Modulo 1 - Estrutura base do monorepo

Objetivo: criar a organizacao inicial do repositorio, documentacao basica e arquivos de apoio.

Status: concluido.

Entregas:

- Diretorios `backend/`, `frontend/`, `docker/`, `docs/` e `scripts/`.
- `.gitignore` e `.editorconfig`.
- `README.md` inicial.
- Exemplo de lista M3U em `docs/examples/sample.m3u`.

## Modulo 2 - Backend base

Objetivo: criar aplicacao Spring Boot 3 com Java 21, Maven, configuracao de banco PostgreSQL e migrations Flyway iniciais.

Status: concluido.

Entregas:

- Projeto Maven em `backend/`.
- Pacotes base em `com.example.iptvmanager`.
- Configuracoes por profile.
- Flyway conectado ao PostgreSQL.
- Dockerfile do backend.
- Docker Compose e scripts para subir/parar containers.
- Teste de contexto da aplicacao.

## Modulo 3 - Autenticacao e usuarios

Objetivo: implementar cadastro, login, JWT, usuario atual e controle de perfis `ADMIN` e `USER`.

Status: concluido.

Entregas:

- Entidade `User`, enum `UserRole` e repositorio JPA.
- Entidade `RefreshToken` e fluxo de refresh/logout.
- Cadastro, login, refresh e `/api/auth/me`.
- Endpoints administrativos de usuarios em `/api/users`.
- Senhas com BCrypt.
- JWT com filtro de autenticacao.
- Protecao de rotas por perfil `ADMIN`.
- Seed opcional de administrador via `ADMIN_EMAIL` e `ADMIN_PASSWORD`.
- Tratamento global de erros.
- Testes de auth, refresh, BCrypt, `/me` e protecao admin.

## Modulo 4 - Listas IPTV

Objetivo: cadastro de listas por upload e URL, ownership, status e endpoints principais.

Status: concluido.

Entregas:

- Entidade `IptvList`, enums de origem/status e repositorio JPA.
- Cadastro de lista por URL.
- Upload de arquivo `.m3u` e `.m3u8` com armazenamento local configuravel.
- Listagem, detalhe, atualizacao e exclusao de listas.
- Regras de ownership: `USER` acessa apenas as proprias listas.
- Regra de administrador: `ADMIN` acessa todas as listas.
- Validacoes de nome, URL e extensao de arquivo.
- Testes de URL, upload, isolamento por usuario e acesso admin.

## Modulo 5 - Parser M3U/M3U8

Objetivo: implementar `M3uParserService` e `ParsedChannelDTO`.

Status: concluido.

Entregas:

- DTO `ParsedChannelDTO`.
- Servico `M3uParserService`.
- Extracao de nome, URL, categoria, logo, `tvg-id`, `tvg-name`, duracao e linha bruta.
- Fallbacks para categoria e nome.
- Validacao de URL `http://` ou `https://`.
- Remocao de duplicidade por `streamUrl`.
- Testes do parser para casos validos, fallback, URL invalida, duplicidade e comentarios intermediarios.

## Modulo 6 - Importacao de canais

Objetivo: importar canais de arquivos ou URLs, atualizar status e salvar canais em lote.

Status: concluido.

Entregas:

- Servico `IptvImportService`.
- Endpoints `POST /api/iptv-lists/{id}/import` e `POST /api/iptv-lists/{id}/refresh`.
- Importacao de listas por arquivo e URL.
- Atualizacao de status `PROCESSING`, `IMPORTED` e `ERROR`.
- Preservacao de favoritos no refresh por `streamUrl`.
- Salvamento em lote via `saveAll`.

## Modulo 7 - Canais, busca e favoritos

Objetivo: endpoints paginados, filtros, categorias e favoritos.

Status: concluido.

Entregas:

- Entidade `Channel`, DTOs, mapper, repositorio, servico e controller.
- Listagem paginada e ordenada.
- Filtros por nome, lista, categoria, favorito e ativo.
- Busca, grupos e favoritos.
- Regras de acesso por dono e administrador.

## Modulo 8 - Dashboard

Objetivo: estatisticas e ultimos registros para o dashboard.

Status: concluido.

Entregas:

- Endpoint `GET /api/dashboard`.
- Contagens de listas, canais, categorias e favoritos.
- Listas e canais recentes.

## Modulo 9 - Frontend base

Objetivo: criar app Angular, layout, auth core, guards e interceptors.

Status: concluido.

Entregas:

- App Angular standalone.
- Layout com menu lateral e topo.
- Login, cadastro, guarda de rotas e interceptor JWT.

## Modulo 10 - Telas de listas e canais

Objetivo: CRUD visual de listas, upload, URL, detalhes, busca e filtros de canais.

Status: concluido.

Entregas:

- Telas de listas, nova lista, detalhes e importacao.
- Tela de canais com busca.
- Tela de favoritos.

## Modulo 11 - Player HLS

Objetivo: tela de reproducao com HLS.js, troca de canal e tratamento de erro.

Status: concluido.

Entregas:

- Tela `/channels/:id/play`.
- Player HTML5 com fallback HLS.js.
- Tratamento de erro de carregamento.

## Modulo 12 - Area administrativa

Objetivo: gerenciamento de usuarios para `ADMIN`.

Status: concluido.

Entregas:

- Tela `/admin/users` protegida por perfil `ADMIN`.
- Listagem de usuarios.

## Modulo 13 - Empacotamento e documentacao final

Objetivo: Docker Compose completo, README final, exemplos, collection HTTP/Postman e validacao geral.

Status: concluido.

Entregas:

- Docker Compose com PostgreSQL, backend e frontend.
- Nginx para frontend com proxy `/api`.
- Arquivo HTTP em `docs/api.http`.
- README atualizado.
