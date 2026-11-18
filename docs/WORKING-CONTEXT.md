# Working Context

> Estado efímero del proyecto — lo que cambia sprint a sprint. **No es** CLAUDE.md
> (permanente) ni ROADMAP.md (planificado). Si algo de acá deja de cambiar, se promueve
> a uno de esos dos o a un ADR; si es una decisión sin tomar, va a `pending-decisions.md`.

**Última actualización**: 2026-11-16 (doc-sync). `main` en `6a6e89a`, con cambios de documentación sin
commitear (ver abajo).

---

## Verdad actual

- **Branch**: `main`, sin branches locales de trabajo. Último PR: #87 (2026-11-07, polish mobile y reporte a
  Sentry de los fallos de Google Places). Sin commitear, todos de documentación salvo `backend/.env.example`
  (comentarios): el doc-sync de hoy y la lista de la reunión.
- **Producción en vivo**, verificada el 2026-11-16 con `curl`: home, `/produtos` y API responden. Funcionan
  las reseñas de Google (desde el 2026-11-07, `pending-decisions.md` §28), el mapa y la subida de fotos a R2
  (desde el 2026-11-16, §29). El catálogo real tenía **1 producto** cargado al 2026-11-16.
- **Cobro**: estado confirmado por escrito (marcado en `docs/reunion-2026-11-07.md` el 2026-11-16).
- **Infra**:
  - IPv4 del VPS `167.233.254.140` (antes `167.235.134.150`); sin confirmar si es fija. El host sale a
    internet por IPv6 de forma preferente.
  - **Sin acceso a la cuenta de Hetzner** (contraseña olvidada; el código de verificación llega al mail del
    cliente). La terminal de Coolify (Servers → localhost → Terminal) es un shell del host y alcanza para
    trabajar en el VPS. Se resuelve en la próxima reunión con el cliente.
  - Coolify mostraba 39 actualizaciones del sistema y reinicio pendiente.
- **Sprint Despliegue**: D.1–D.6 y D.8 hechos; **D.7 (backups) sin hacer**; D.9 y D.10 en curso. Detalle en
  `ROADMAP.md`.
- **Sprint 7**: 7.1–7.4, 7.6, 7.9 y 7.12–7.14 hechas. Parciales: 7.7 (tres arreglos mobile en #87, probados
  en celular real contra producción; falta la auditoría completa), 7.10 y 7.11.
- **Lista de trabajo vigente**: `docs/reunion-2026-11-07.md` (juntos, cliente, mío, y §4 con los pedidos nuevos
  del cliente). Es la fuente de qué está hecho y qué no; Sebastián la actualiza a mano.
- **Entorno de prueba local** (servidores de desarrollo, reenvío del puerto 3000, regla de firewall, base
  `frontpet_phonetest`): desmontado el 2026-11-16.

## Urgente / bloqueado

- 🔴 **Cuenta de Cloudflare propia del cliente para R2** (§18). R2, DNS y Resend corren en la cuenta
  personal de Sebastián, compartida con TurnosUy y con su tarjeta. Hacerlo **antes de cargar el catálogo
  real**: cada foto guarda la URL completa del bucket. Necesita el mail y la tarjeta del cliente.
- 🔴 **D.7 backups**: hay datos reales sin respaldo. Ya se puede hacer desde la terminal de Coolify.
- Acceso a Hetzner: necesita el código que llega al mail del cliente.

## Pedidos del cliente del 2026-11-16 (sin implementar)

Detalle y hallazgos en `docs/reunion-2026-11-07.md` §4; tareas 9.1 a 9.9 en el colchón (Sprint 9) del
`ROADMAP.md`:
- Capacidad de 2 a 1 (un `UPDATE` en `tenants.config`, no hay pantalla de admin).
- Raça y observaciones del pet no se ven en el detalle del turno (se guardan, no se muestran).
- Teléfonos de Uruguay mal normalizados (backend y frontend, y clave de la anonimización LGPD).
- Mensaje del pedido al revés, entrega siempre gratis y falta la forma de pago.
- Quitar la patita del selector de tamaño del wizard.
- Banho Antisséptico: adicional sin costo (R$ 0), +10 min, primero en la lista (decidido el 2026-11-16).
- Horarios liberados a mano (§31): decidido liberar **rangos de horario**, con la pantalla de admin
  desktop-first (mobile después); el diseño se planifica al desarrollarlo.

## Deuda técnica y documental conocida

- 🔴 "Produtos em destaque" de la home muestra 4 productos inventados con precios que no existen (§30).
- V5 corrió en producción (verificado de forma indirecta, §4). Falta decidir si se separan las migraciones
  por perfil.
- **Paleta**: los valores de `globals.css` (`@theme`) siguen siendo placeholders pendientes de confirmación
  del cliente. Se sigue codeando con ellos; cambiarla después es tocar un solo archivo. (Movido desde
  CLAUDE.md §5.)
- Cloudflare: `/_next/image` sin caché (`DYNAMIC`); 0,61 % cacheado en 30 días; tráfico mayormente de bots y
  de la API, sin costo extra en el plan Free.
- `StartupEnvValidator` solo exige que las variables de R2 no estén vacías; no valida su formato (§29).
- Sin agendar: §19 (prueba real del mail de reset), §20 (validación visual del dashboard), §21 (doble submit
  del wizard), §22 (500 ante UTF-8 inválido), §23 (`send-default-pii`, el banner LGPD ya existe), §27 (stock),
  §17 (valores arbitrarios de Tailwind), §14 (`SERVICES_PREVIEW` estático), §13 (inputs nativos de fecha).
- Local: la base `frontpet` de desarrollo quedó desfasada de V12 y V13 (otro checksum en V12; la columna de
  V13 ya existía). Recrearla desde cero borra sus datos locales.
- Toolchain alineado el 2026-11-16 (branch `chore/align-node-pnpm`, sin mergear): Node 24.15 y pnpm 10.34.9
  en WSL, CI y Docker (ADR 015). La primera vez, `pnpm install` en WSL pide recrear `node_modules`: aceptar.
- El build puede fallar al azar por `next/font/google` (§32): reintentar; en local, borrar antes
  `frontend/.next/cache`. Mitigación propuesta: autoalojar las fuentes.
- Referencias: `docs/reuse-consultorio.md` fue eliminado a propósito (recuperable con
  `git show fbd9aa0^:docs/reuse-consultorio.md`) y lo siguen citando comentarios Javadoc de `identity` y
  `config`. `docs/estado-frontpet.md` y `FrontPet.md` están en `.gitignore` y no existen en esta máquina,
  aunque `ESTADO.md` cita el primero.
- `docs/ui/` es un export de Stitch viejo; `docs/learnings.md` está gitignored.

## Regla de actualización

Se reescribe (no se acumula) en cada `/sprint-close` y cada `/doc-sync`. Si una entrada deja de ser cierta, se borra.
Si algo permanece igual durante 2+ sprints, probablemente no es efímero: mover a CLAUDE.md o a un ADR.
