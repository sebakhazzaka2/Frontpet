---
description: Ejecutar el DoD (CLAUDE.md §10) como checklist verificable, con salida PASS/FAIL por fase.
---

# /verify

Corré el Definition of Done real del proyecto (CLAUDE.md §10), fase por fase, y devolvé
un reporte con el mismo formato siempre — para que sea comparable entre corridas, no una
narración distinta cada vez.

No implementes nada acá. Si una fase falla, reportalo; arreglar es una tarea aparte
(a menos que el usuario pida arreglarlo en el momento).

## Cuándo correrlo

- Antes de pedirle al usuario que haga commit o abra PR
- Después de terminar una feature o de un refactor no trivial
- Cuando el usuario pida "verificá" o "¿está listo esto?"

## Fases

Ejecutá todas. No saltees ninguna por asumir que "seguro pasa".

### 1. Backend build + tests

```bash
cd backend && mvn test 2>&1 | tail -40
```

`mvn test` alcanza: no hay `maven-failsafe-plugin` configurado, así que los
`*IntegrationTest.java` (Testcontainers) ya corren con Surefire por nombre de archivo.
No uses `mvn verify` pensando que corre más tests — hoy no corre ninguno extra.

Reportá: compila sí/no · tests totales · pasados/fallados · si algún `*IntegrationTest`
específico falló, nombralo.

### 2. Frontend build + lint + test

Desde WSL (ADR 015 — el dev server no es suficiente, esto tiene que correr con el
toolchain real):

```bash
cd frontend && pnpm build 2>&1 | tail -40
pnpm lint 2>&1 | tail -30
pnpm test 2>&1 | tail -40
```

Si no estás corriendo dentro de WSL, decilo explícitamente en el reporte — no falsees
un PASS que corrió en el entorno equivocado.

### 3. Warnings de compilación

Revisá la salida de `mvn test` y `pnpm build` en busca de warnings, no solo errores.
CLAUDE.md §10 exige "sin warnings", no solo "sin errores".

### 4. Validación visual (solo si hubo cambios de UI)

Si el diff toca `frontend/app/**` o `frontend/components/**`:
- Confirmá que se validó a 320/768/1024px (1440 si hay layout desktop propio)
- Confirmá que hay screenshot para la issue/PR
- Si no se hizo, marcalo como pendiente — no lo des por hecho

### 5. Schema DB (solo si hubo migración nueva)

Si el diff agrega algo en `backend/src/main/resources/db/migration/`:
- `docs/db-model.png` debe estar regenerado — si no lo está, marcarlo como bloqueante
- Confirmá que no hay drift entre migraciones (el caso `tempo_extra` de esta semana:
  columna agregada por V11 que ya existía en V3 de otro entorno)

### 6. ADR pendiente

Si el diff refleja una decisión técnica no trivial sin ADR correspondiente en
`docs/decisions/`, marcalo. No crees el ADR vos — señalalo para que el usuario decida.

### 7. Diff review

```bash
git status
git diff --stat
```

Revisá que no haya archivos inesperados (secrets, `.env`, artefactos de build).

## Salida

Devolvé siempre este formato, sin secciones extra:

```
VERIFY REPORT
=============

Backend (mvn test):     [PASS/FAIL] — X tests, Y fallados
Frontend build:          [PASS/FAIL]
Frontend lint:           [PASS/FAIL] — X warnings
Frontend test (vitest):  [PASS/FAIL] — X/Y passed
Warnings de compilación: [NINGUNO/LISTA]
Validación visual:       [OK/PENDIENTE/N-A]
db-model.png:            [OK/DESACTUALIZADO/N-A]
ADR pendiente:           [NO/SÍ — cuál decisión]
Diff:                    [X archivos, ninguno sospechoso / revisar: <archivo>]

Entorno: [WSL confirmado / NO confirmado — resultados de frontend no garantizados]

Overall: [READY / NOT READY para PR]

Si NOT READY, en orden de bloqueo:
1. ...
2. ...
```
