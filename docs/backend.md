# Backend de salas, reservas e chaves

Java 21, Spring Boot 4.1.1, Maven, Spring Data JPA e PostgreSQL. Reutiliza os
usuários UUID, perfis e JWT existentes. Não contém interface web.

## Executar

Com JDK 21 completo e PostgreSQL disponível:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ifsystem
export DB_USERNAME=postgres
export DB_PASSWORD='senha-do-banco'
export JWT_SECRET='configure-um-segredo-com-pelo-menos-32-bytes'
./mvnw spring-boot:run
```

O banco deve existir. Hibernate mantém a estratégia preexistente `ddl-auto=update`.
São criadas as tabelas `reservations` e `key_movements`, com UUIDs, FKs para
usuários/reservas e índices. Salas não são copiadas para uma tabela JPA.

Para desenvolvimento sem PostgreSQL:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev` usa H2 em memória e lê a planilha real; porta padrão 8081,
configurável por DEV_PORT. O perfil `demo` usa H2 e três salas fictícias
explicitamente identificadas em `demo/rooms.csv`. Ao encerrar o processo, dados
desses perfis são apagados. Não há usuários pré-cadastrados.

## Fonte de salas

`SpreadsheetRoomProvider` implementa `RoomDataProvider` e lê CSV HTTPS ou um
Resource Spring, com limite de 5 MB, timeout, cache e tratamento de erros (503).
A configuração padrão aponta para a exportação CSV da planilha fornecida,
aba Horários. Repetições idênticas de salas são consolidadas, nomes vazios são
ignorados e identificadores com nomes conflitantes são rejeitados.

Variáveis: ROOMS_LOCATION, ROOMS_NAME_COLUMN (padrão Sala / Local),
ROOMS_ID_COLUMN (opcional), ROOMS_CACHE_TTL (PT5M) e ROOMS_ZONE
(America/Sao_Paulo). A fonte precisa permitir exportação pública ou ser
substituída por CSV local/provider autenticado. Nenhuma escrita é feita na planilha.

IDs são derivados dos nomes normalizados se não houver uma coluna de IDs.
Renomear salas exige preservar seus identificadores por uma coluna imutável.
Capacidade, bloco, tipo e descrição ficam null quando ausentes na fonte.

**A disponibilidade considera apenas reservas aprovadas. As aulas semanais da
planilha não bloqueiam automaticamente datas.** Essa integração depende de
calendário letivo, feriados e exceções.

## API

Autenticar em `POST /auth/login`, com `{"cpf":"...","senha":"..."}` e enviar
`Authorization: Bearer <token>`. As novas APIs exigem usuário ativo. Operador =
ADMINISTRADOR ou SERVIDOR. As permissões das rotas antigas de usuários foram
preservadas e precisam de revisão antes de implantação real, pois permitem
criar perfis privilegiados sem autenticação.

| Método | URL | Acesso / finalidade |
|---|---|---|
| GET | /api/rooms | Autenticado; lista salas |
| GET | /api/rooms/{id} | Autenticado; detalhe da sala |
| GET | /api/rooms/{id}/availability?date=YYYY-MM-DD | Autenticado; intervalos livres/ocupados |
| POST | /api/reservations | Autenticado; cria solicitação pendente |
| GET | /api/reservations | Próprias reservas; operadores consultam todas |
| GET | /api/reservations/{id} | Solicitante ou operador |
| POST | /api/reservations/{id}/approve | Operador |
| POST | /api/reservations/{id}/reject | Operador; exige motivo |
| POST | /api/reservations/{id}/cancel | Solicitante ou operador |
| POST | /api/keys/checkout | Operador; retirada no período aprovado |
| POST | /api/keys/checkin | Operador; devolução |
| GET | /api/keys/status | Operador; situação por sala |
| GET | /api/keys/history | Operador; histórico paginado |

Criação de reserva:

```json
{"roomId":"sala-114","startTime":"2026-11-13T10:00:00-03:00","endTime":"2026-11-13T11:00:00-03:00"}
```

Rejeição: `{"reason":"Manutenção"}`. Retirada: `{"reservationId":"UUID"}`.
Devolução: `{"movementId":"UUID"}`. Aprovação e cancelamento não exigem corpo.
A API usa DTOs que não expõem entidades, senhas ou CPFs dos solicitantes.
Datas são instantes UTC; disponibilidade diária usa ROOMS_ZONE.

Consultas de reservas aceitam roomId, requesterId, status, start, end, page e size.
Histórico aceita roomId, responsibleUserId, start, end, page e size.
Página começa em zero, tamanho padrão 20, máximo 100. Respostas paginadas usam
Page do Spring Data. Períodos filtram sobreposição, inclusive movimentos ativos.
Status de reserva: PENDING, APPROVED, REJECTED, CANCELLED. Status de chave:
AVAILABLE/CHECKED_OUT na situação e CHECKED_OUT/RETURNED nos movimentos.

Erros de negócio retornam ProblemDetail com detail: 400 dados inválidos,
401 autenticação, 403 autorização, 404 recurso ausente, 409 conflito/transição,
503 fonte de salas inválida/indisponível. Exemplo:
`{"status":409,"detail":"An approved reservation overlaps this period"}`.

## Regras e concorrência

Solicitações começam pendentes. Professor tem prioridade sobre solicitação
conflitante pendente de aluno; reservas já aprovadas não são deslocadas. Revisão
registra operador, momento e motivo de rejeição. Intervalos são [início,fim),
permitindo reservas adjacentes. Chaves só saem durante a reserva aprovada, com
solicitante ativo; devoluções atrasadas são permitidas. O responsável é o
solicitante, e os operadores de retirada/devolução são registrados separadamente.
Não é permitido cancelar com chave ativa nem devolver duas vezes.

No PostgreSQL, mutações compartilham uma trava transacional por sala, obtida antes
da consulta de conflitos. Entidades são atualizadas após esperar pela trava;
@Version complementa a proteção. Usar isolamento READ_COMMITTED. Escritas externas
precisam respeitar esse protocolo. H2 local usa uma linha de bloqueio compartilhada,
serializando operações de todas as salas nesses perfis. Histórico nunca é apagado
pela devolução. Disponibilidade física de chave é separada da disponibilidade de
horários de reserva.

## Testes e rastreabilidade

```bash
./mvnw clean verify
```

Testes de regras, autorização JWT, REST e concorrência usam PostgreSQL real
isolado iniciado por embedded-postgres (somente teste), sem Docker ou banco da
aplicação. JUnit também cobre CSV inválido/duplicado, estabilidade de IDs,
disponibilidade, perfil dev JPA/H2 e ciclo de chaves no H2. Os testes não consultam
o Google nem usam credenciais reais. O PostgreSQL temporário exige usuário comum,
execução de binários temporários e conexões locais.

Dependências novas: commons-csv 1.14.1 e embedded-postgres 2.2.0 (teste).
Chave Jira confirmada: IFSISTEM. Épico IFSISTEM-13; salas IFSISTEM-6 / 19–24;
reservas IFSISTEM-17 / 25–31; chaves IFSISTEM-18 / 32–38.
