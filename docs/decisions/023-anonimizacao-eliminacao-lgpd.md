# ADR 023 — Anonimização em vez de borrado para o direito de eliminação LGPD

**Estado**: Aceptada
**Fecha**: 2026-08-29
**Sprint**: 7 (tarea 7.14)

---

## Contexto

`frontend/app/(public)/privacidade/page.tsx` ya afirmaba, antes de este sprint, que o titular
pode *"solicitar a exclusão dos seus dados a qualquer momento, entrando em contato pelo
WhatsApp"* — mas não existia nenhum mecanismo, no admin ou no backend, para atender esse
pedido. A tarea 7.14 do ROADMAP pede o endpoint que cumpre essa promessa.

Não há auth de clientes finais em MVP1 (CLAUDE.md §6) — o canal é WhatsApp, e é o **admin**
quem executa a eliminação a pedido do titular, não um formulário self-service.

O modelo de dados (ADR 013 §4) não tem tabela `customer`: um "titular" são N linhas de
`orders` e N de `appointments`, unidas só por `cliente_telefone_norm` — coluna que o próprio
ADR 013 §11 decidiu **não indexar**, porque a única query que precisaria era "Fase 2". Esta
tarea é essa query.

---

## Decisión

### Anonimização in-place, não `DELETE`

Um `DELETE` levaria junto os snapshots de preço e itens (`order_items`,
`appointment_addons`) — dado contábil, não pessoal — e reduziria os conteos do
mini-dashboard operativo que se constrói no mesmo sprint (7.1/7.3). A anonimização preserva o
histórico comercial e elimina só o dado pessoal, que é a leitura padrão da LGPD art. 16
(conservação para cumprimento de obrigação legal/fiscal).

Campos limpos, com marcadores em vez de `NULL` onde a coluna é `NOT NULL`:

| Tabela | Campos anonimizados | Preservado |
|---|---|---|
| `orders` | `cliente_nome` → `"Titular removido"`, `cliente_telefone` → `"—"`, `cliente_telefone_norm` → `NULL`, `endereco_entrega` → `"—"`, `horario_entrega` → `NULL` | `status`, `subtotal_snapshot`, `order_items` completos |
| `appointments` | idem cliente, mais `pet_nome` → `"—"`, `pet_raca` → `NULL`, `observacoes` → `NULL` | `status`, snapshots de preço/duração, `start_at`/`end_at`, addons |

### Por titular (telefone), não por pedido/turno individual

O direito de eliminação é da pessoa, não de um pedido pontual — se o mesmo cliente fez 3
pedidos, a solicitação cobre os 3, sem que o admin precise achá-los um a um. O endpoint busca
por `cliente_telefone_norm` em `orders` **e** `appointments` simultaneamente.

Isso reverte ADR 013 §11: agora existe a query que precisava do índice. `V12` adiciona um
índice **parcial** (`WHERE cliente_telefone_norm IS NOT NULL`) em ambas as tabelas — as linhas
já anonimizadas (`NULL`) nunca voltam a ser buscadas por telefone, então não faz sentido
indexá-las.

### Preview antes de executar

A anonimização é irreversível — apaga exatamente o dado (telefone) que identifica o titular,
então não dá pra "desfazer e conferir" depois. `GET /api/v1/admin/privacy/preview` lista o que
seria afetado; `POST /api/v1/admin/privacy/anonymize` executa. Rodar `anonymize` de novo com o
mesmo telefone devolve 404 — idempotente, porque a primeira chamada já deixou
`cliente_telefone_norm = NULL`.

### `privacy_erasure_log` guarda um hash, não o telefone

Não existia nenhuma auditoria de "quem fez o quê" no repo (`common.Auditable` só dá
`createdAt`/`updatedAt`) — uma operação destructiva e irreversível sem registro é um risco que
não vale correr. O log guarda `tenant_id`, `admin_user_id`, `orders_anonymized`,
`appointments_anonymized`, `created_at` e um **SHA-256 do telefone normalizado**, nunca o
telefone em si: reter o identificador que acabou de ser removido do titular contradiria a
própria eliminação.

`admin_user_id` não tem FK para `admin_users`: nenhuma tabela do repo referencia essa tabela
hoje, porque os testes constroem `AdminUser` em memória sem persistir (mesmo padrão de
`AdminOrderControllerTest`). Uma FK quebraria todo teste deste endpoint.

### `PrivacyService` orquestra `OrderService` e `AppointmentService`, nunca seus repositórios

CLAUDE.md §5: "otros módulos consumen esa interfaz, nunca el repositorio". `orders` e
`booking` ganharam `findPublicIdsByPhone`/`anonymizeByPhone` nas suas próprias interfaces de
serviço; o módulo novo `privacy` não importa `OrderRepository` nem `AppointmentRepository`.

---

## Alternativas consideradas

### ❌ `DELETE` físico

Mais simples e mais contundente frente ao titular, mas apaga histórico contábil e desalinha
os conteos do dashboard operativo do mesmo sprint. Rejeitado.

### ❌ Um endpoint por `publicId` (um pedido/turno de cada vez)

Trivial de implementar, mas não corresponde à semântica real do direito de eliminação (é da
pessoa, não do pedido) e obriga o admin a caçar cada registro do titular manualmente.

### ❌ Deixar o admin escolher entre anonimizar ou excluir em cada solicitação

Mais flexível, mas duplica os caminhos a testar e transfere pro operador uma decisão jurídica
que não é dele. A anonimização é a única opção compatível com a retenção contábil que o
próprio negócio precisa — não é uma escolha caso a caso.

---

## Consecuencias

### Positivas
- Cumpre a promessa já publicada em `/privacidade` sem quebrar o histórico comercial nem os
  conteos do dashboard.
- `V12` corrige duas decisões que já estavam datadas para expirar (ADR 013 §11 sobre o
  índice, e a ausência de qualquer auditoria de operações destrutivas).
- O padrão (`preview` → confirmação → `anonymize`) generaliza para qualquer futura operação
  destrutiva do admin.

### Negativas
- `cliente_telefone_norm` passa a ser nullable em duas tabelas que antes garantiam
  `NOT NULL` — qualquer código futuro que assuma esse campo sempre presente precisa checar
  `null` explicitamente.
- O log de auditoria não tem UI própria nesta entrega (fora do escopo da tarea 7.14); hoje só
  é consultável direto na base.
