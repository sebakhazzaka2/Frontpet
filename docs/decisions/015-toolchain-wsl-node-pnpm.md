# ADR 015 — Toolchain de desarrollo: WSL, Node y pnpm

**Estado**: Aceptada
**Fecha**: 2026-07-16
**Sprint**: 2 (previo al arranque)
**Relacionada**: [ADR 014](./014-tailwind-v4-css-first.md) (Tailwind v4 CSS-first)

---

## Contexto

Al intentar verificar por primera vez que el frontend compilaba después de la migración a
Tailwind v4 (ADR 014), **el build falló**. El diagnóstico destapó cuatro problemas
independientes de toolchain que estaban latentes y no los cubría ningún documento.

Ninguno era evidente porque **nunca se había corrido `pnpm build`** desde la migración.

### Problema 1 — El repo vive en WSL, pero las herramientas corren en Windows

El proyecto está en `/home/sebakhazzaka/proyectos/Frontpet` (filesystem de WSL2/Ubuntu).
Se accede desde Windows vía el path UNC `\\wsl.localhost\Ubuntu\...`.

`node_modules` de pnpm es un árbol de **symlinks** hacia el store `.pnpm/`. Leer esos
symlinks desde Windows por UNC devuelve `Input/output error`. Cualquier herramienta que
corra del lado Windows (Git Bash, un node instalado con nvm4w, un agente de IA) **no puede
resolver las dependencias**, aunque el `ls` del directorio funcione.

Peor: si se corre `pnpm install` desde Windows sobre el path UNC, pnpm resuelve las
**optionalDependencies de la plataforma equivocada**.

### Problema 2 — `@tailwindcss/oxide` sin binario nativo

Consecuencia directa del anterior. El build moría con:

```
Error: Cannot find module './tailwindcss-oxide.linux-x64-gnu.node'
```

`@tailwindcss/oxide` es el motor Rust de Tailwind v4 y se distribuye como binarios nativos
por plataforma, vía optionalDependencies. El `pnpm-lock.yaml` **sí** listaba
`@tailwindcss/oxide-linux-x64-gnu@4.3.0`, pero **no había ningún binario de oxide instalado
en disco** — ni el de Linux ni el de Windows.

Curiosamente, `@next/swc-linux-x64-gnu` y `lightningcss-linux-x64-gnu` **sí** estaban
presentes. O sea: la instalación estaba a medias, no era un simple caso de plataforma
cruzada. Al reinstalar desde WSL, pnpm avisó
`ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY` — su forma de decir que el `node_modules`
existente lo había creado **otro package manager**.

### Problema 3 — El `node` del sistema en WSL es v18

```
$ which node → /usr/bin/node
$ node -v    → v18.19.1        ← Next 16 requiere >= 20.9
$ nvm ls     → v24.15.0 (default, lts/*)
```

WSL tiene **dos** Node: el del sistema (apt, v18.19.1) y el de nvm (v24.15.0). En una
terminal interactiva, `.bashrc` carga nvm y queda v24 — por eso el `dev` funcionaba. Pero
**en cualquier shell no interactivo** (un script, un hook, CI, un agente) nvm no se carga y
queda el v18, con el que **Next 16 no arranca**.

### Problema 4 — `allowBuilds` con placeholders sin decidir

`frontend/pnpm-workspace.yaml` contenía:

```yaml
allowBuilds:
  esbuild: set this to true or false     # ← placeholder literal, nunca resuelto
  msw: true
  sharp: true
  unrs-resolver: true
ignoredBuiltDependencies:
  - sharp                                # ← contradice el allowBuilds de arriba
  - unrs-resolver
```

pnpm ≥ 10 **no ejecuta postinstall scripts** salvo aprobación explícita. Cuando encuentra
uno sin decidir, escribe el placeholder `set this to true or false` y **falla el install**.
Como `pnpm build` corre un chequeo de deps antes de compilar, el build fallaba aunque el
código estuviera bien. Además `sharp` y `unrs-resolver` estaban simultáneamente permitidos
e ignorados.

---

## Decisión

### 1. Todo el toolchain de Node corre **dentro de WSL**. Sin excepciones.

`pnpm install`, `pnpm dev`, `pnpm build`, `pnpm test` y `pnpm lint` se ejecutan desde una
shell de WSL, nunca desde Windows sobre el path UNC.

- En VS Code: usar la extensión **WSL** y abrir la carpeta como remota
  (`code .` desde la terminal de WSL). Sin esto, las extensiones de TS/ESLint resuelven
  contra un `node_modules` que no pueden leer.
- Si un `node_modules` fue creado desde Windows, no se repara: se purga y se reinstala
  desde WSL.

### 2. Node se fija por proyecto, no se hereda del sistema

Se agrega `.nvmrc` con la versión, y todo shell no interactivo carga nvm explícitamente
antes de correr comandos:

```bash
export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"; nvm use
```

**No** se usa `/usr/bin/node` (v18): no cumple el mínimo de Next 16.

### 3. `allowBuilds` es una lista de decisiones explícitas, no un placeholder

Cada postinstall se decide con su razón escrita en el archivo:

| Paquete | Decisión | Por qué |
|---|---|---|
| `esbuild` | `true` | Compila su binario nativo. Vitest no arranca sin él |
| `sharp` | `true` | Binario de procesamiento de imágenes que usa `next/image` en build |
| `unrs-resolver` | `true` | Resolver nativo, transitivo de `eslint-config-next`. Sin él ESLint no resuelve los alias `@/...` |
| `msw` | `false` | Su postinstall copia `mockServiceWorker.js` a `public/`. No usamos MSW |

Se elimina `ignoredBuiltDependencies`, que contradecía a `allowBuilds`.

---

## Consecuencias

**Positivas**
- `pnpm build`, `pnpm test` y `pnpm lint` pasan en verde por primera vez desde la migración
  a Tailwind v4.
- El toolchain queda determinístico: misma plataforma para instalar y para compilar.
- Las decisiones de `allowBuilds` quedan documentadas en el propio archivo, no en la
  memoria de nadie.

**Negativas / costos**
- Hay que trabajar siempre desde WSL. Abrir el proyecto desde Windows "porque es más rápido"
  vuelve a romper `node_modules`.
- El `node` del sistema (v18) sigue ahí y sigue siendo una trampa para scripts que no carguen
  nvm. Alternativa futura: sacar el node de apt, o usar Volta/mise que no dependen de que la
  shell sea interactiva.

**Regla derivada (va a CLAUDE.md sección 10, Definition of Done)**
> Ninguna migración de dependencias se da por terminada sin correr `build`, `test` y `lint`
> en verde. Esta migración estuvo ~6 semanas rota sin que nadie lo supiera, porque el
> `dev` server no la ejercitaba.

---

## Alternativas descartadas

- **Mover el repo al filesystem de Windows** (`C:\...`): resuelve el UNC, pero el I/O de
  Docker + WSL contra `/mnt/c` es notablemente más lento, y el backend corre Postgres en
  Docker. Peor trade-off.
- **Usar npm o yarn** en vez de pnpm: el problema no es pnpm, es la frontera Windows/WSL.
  npm tendría el mismo síntoma con las optionalDependencies de plataforma.
- **Commitear `node_modules`**: no.
