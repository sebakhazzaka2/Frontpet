# Checklist de prueba en producción

Recorrido de punta a punta sobre `https://frontpet.com.br` y `https://api.frontpet.com.br`, con datos marcados como prueba, y limpieza posterior con [`limpieza-datos-prueba.sql`](limpieza-datos-prueba.sql).

**Resultado**: marcar `[x]` / anotar el problema al lado. Un fallo no detiene la prueba; se anota y se sigue.

---

## 0. Antes de empezar (no saltear)

- [ ] **Backup de la BD** y comprobar que el archivo existe y pesa algo. Desde Coolify (Postgres → *Backups* → *Backup now*), o por terminal del servidor:
  `docker exec <contenedor-postgres> pg_dump -U <usuario> -Fc <bd> > frontpet-pre-prueba.dump`
  (Esto además cierra la duda de D.7: si el backup se puede hacer, falta programarlo y probar una restauración.)
- [ ] **Anotar la hora de inicio** (con zona horaria): `______` → va en `t0` del SQL.
- [ ] **Anotar conteos reales previos** (consulta en la BD): `SELECT count(*) FROM orders;` `… FROM appointments;` `… FROM products;`
  Pedidos ____ · Turnos ____ · Productos ____ (se comparan al final).
- [ ] Anotar el estado actual de **horarios de funcionamiento** y **precios de servicios** (la prueba no debería cambiarlos; si los editás, hay que revertirlos a mano).
- [ ] Tener a mano: un celular real, otra sesión/navegador, y el panel de Sentry y Plausible.

### Reglas de marcado (para que el SQL pueda limpiar)

| Dato | Cómo marcarlo |
|---|---|
| Nombre del cliente (pedido / turno) | Empieza con `ZZ-TEST` → `ZZ-TEST Ana` |
| Nombre del pet | `ZZ-TEST Rex` |
| Teléfono | `(55) 90000-0001` … `(55) 90000-0005` (normaliza a `5555900000001`…) |
| Producto / marca | Nombre `ZZ-TEST Ração A`, marca `ZZ-TEST Marca` |
| Bloqueo de agenda | Motivo `ZZ-TEST bloqueio` |

⚠️ **No modificar productos, pedidos ni turnos reales.** Probar solo sobre los `ZZ-TEST`.

### Efectos colaterales a conocer

- **Meta Pixel / Plausible**: en el banner de consentimiento, **rechazar** durante casi toda la prueba. Aceptar solo en §2.9 y asumir que esos eventos quedan en las herramientas (no se pueden borrar desde acá).
- **Rate limit**: 5 pedidos y 5 turnos cada 10 min por IP. Planificar el orden; el sexto intento devuelve `429` (eso es una prueba más, §2.5).
- **Email de reset** (§3.9): envía un email real, y completar el flujo **cambia la contraseña real del admin**.
- **WhatsApp**: el botón abre `wa.me`; **no enviar** el mensaje al cliente real (cerrar la ventana).
- **Sentry**: cualquier error provocado aparecerá en el dashboard; ignorar los de la hora de la prueba.

---

## 1. Infraestructura y cabeceras (sin login)

- [ ] `https://frontpet.com.br` carga (200) y redirige `http://` → `https://`.
- [ ] `https://api.frontpet.com.br/actuator/health` devuelve `{"status":"UP"}` y **nada más** (sin detalles).
- [ ] `/actuator/info`, `/actuator/metrics`, `/actuator/env` → 401/404 (no expuestos).
- [ ] `https://frontpet.com.br/sitemap.xml` y `/robots.txt` → 200.
- [ ] `https://frontpet.com.br/og-image.jpg` → 200 (ya no 404). Pegar el link en WhatsApp o en un validador de Open Graph: se ve la foto.
- [ ] Cabeceras (`curl -I https://frontpet.com.br` y `…api…`): `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`.
  Anotar si falta `Content-Security-Policy` (no está configurado en el repo).
- [ ] `https://api.frontpet.com.br/api/v1/admin/orders` sin cookie → **401** (no 403, no datos).
- [ ] `https://coolify.frontpet.com.br` no resuelve (se borró el registro DNS, pending-decisions §26).
- [ ] `.env`, `/actuator/heapdump`, `/swagger-ui` → 404 en ambos dominios.

## 2. Superficie pública

### 2.1 Landing
- [ ] Hero con foto (no solo degradado), texto legible, botones "Agendar horário" y "Ver produtos" funcionan.
- [ ] Cards de servicios con foto; el rating de Google se muestra (si falla la API de Places, la página no se rompe).
- [ ] Footer: horarios, Instagram, WhatsApp correctos para el cliente. (Hoy la dirección del footer puede ser un placeholder: anotar.)
- [ ] Botón flotante de WhatsApp abre `wa.me/<número correcto>`.

### 2.2 Catálogo
- [ ] `/produtos` lista, búsqueda y filtro por categoría funcionan; "carregar mais" si hay >24.
- [ ] Producto **sin foto** → ícono 🐾, no imagen rota.
- [ ] Detalle `/produtos/<slug>`: precio, variantes, promo (precio tachado), botón WhatsApp.
- [ ] Un producto inactivo **no** aparece en la lista ni en su URL (404).

### 2.3 Carrito y pedido
- [ ] Agregar producto simple y uno con variante; cambiar cantidad; quitar.
- [ ] Recargar la página: el carrito se conserva (sessionStorage); cerrar la pestaña: se pierde (esperado).
- [ ] Checkout: validaciones en PT-BR (nombre, teléfono con letras → error, dirección obligatoria si es entrega).
- [ ] Sin marcar la casilla de privacidad (LGPD) → no deja enviar.
- [ ] Pedido válido (`ZZ-TEST Ana`, `(55) 90000-0001`) → pantalla de éxito + enlace de WhatsApp con el resumen correcto (nombres, cantidades, total = subtotal).
- [ ] **No** enviar el mensaje por WhatsApp.
- [ ] Modalidad "Retirada" se refleja bien en el admin.

### 2.4 Servicios y agendamiento
- [ ] `/servicos` muestra servicios con foto, precio "a partir de" y adicionales.
- [ ] Wizard `/agendamento`, 3 pasos: servicio base + adicionales + porte → fecha y hora → datos.
- [ ] El total y la duración mostrados coinciden con la tabla de precios real.
- [ ] Domingo (cerrado) y fechas pasadas **no** son seleccionables; hoy respeta la anticipación mínima (1 h).
- [ ] Reservar `ZZ-TEST Ana` / pet `ZZ-TEST Rex` / `(55) 90000-0002` → confirmación + `/agendamento/<publicId>`.
- [ ] La página de confirmación muestra nombre y pet, **sin teléfono**.
- [ ] Enlace de WhatsApp de confirmación correcto.

### 2.5 Límites y robustez
- [ ] **Capacidad**: el negocio atiende 2 a la vez. Reservar el mismo horario 2 veces (desde dos dispositivos) → ambas pasan; una tercera → "sem vagas" (409).
- [ ] **Doble submit** (bug conocido, §21): hacer doble clic rápido en "Confirmar". Anotar si crea 2 turnos (confirmar con el admin; ambos son `ZZ-TEST`).
- [ ] **Rate limit**: enviar 6 pedidos (o 6 turnos) seguidos desde la misma IP → el sexto devuelve 429 con mensaje claro en la UI.
- [ ] Probar un nombre con emojis y acentos (`ZZ-TEST Zoë 🐶`): se guarda y se ve bien en admin.

### 2.6 LGPD y tracking
- [ ] Banner de consentimiento aparece en la primera visita.
- [ ] **Antes de aceptar**: en DevTools → Network, no hay requests a `plausible` ni a `facebook`/`fbevents`.
- [ ] Rechazar → sigue sin haber requests de tracking al navegar.
- [ ] (Último paso de la sección, asume el ruido) Aceptar → aparecen los eventos; se puede cambiar la decisión después.
- [ ] `/privacidade` carga y el texto es coherente con lo que se recolecta.

### 2.7 Mobile real y responsive
- [ ] En celular real (4G, no wifi de casa): landing, catálogo, carrito, wizard. Sin scroll horizontal, botones tocables, teclado numérico en teléfono.
- [ ] Revisar a 320 / 768 / 1024 px (DevTools): hero, cards, wizard, admin.
- [ ] Hero: la foto se recorta bien en vertical y el texto sigue legible.

### 2.8 Rendimiento rápido
- [ ] Lighthouse móvil sobre `/` y `/produtos`: anotar LCP/CLS (referencia de la auditoría previa: el LCP es el h1/hero).

## 3. Admin (`/admin`)

### 3.1 Sesión
- [ ] `/admin` sin sesión → redirige a `/admin/login`.
- [ ] Login con credenciales equivocadas → mensaje genérico (no revela si el email existe).
- [ ] 10 intentos fallidos desde la misma IP → bloqueo temporal (⚠️ **no probar con la IP que usás para trabajar**: te bloqueás 15 min; hacerlo desde datos móviles).
- [ ] Login correcto → cookie `frontpet_session` con `HttpOnly`, `Secure`, `SameSite=Lax` y `Domain=frontpet.com.br` (DevTools → Application → Cookies).
- [ ] Recargar `/admin`: la sesión persiste.
- [ ] Logout → la cookie se borra y `/admin` vuelve a pedir login.

### 3.2 Dashboard
- [ ] Conteos (turnos hoy / próximos 7 días / pendientes, pedidos pendientes) coinciden con lo creado en §2.
- [ ] Se ve bien en móvil (hamburger + drawer, sin barra inferior).

### 3.3 Productos
- [ ] Crear `ZZ-TEST Ração A` (marca nueva `ZZ-TEST Marca`, categoría, especie, precio simple).
- [ ] **Subir foto** (JPG/PNG/WebP pequeño): se sube a R2 y se muestra en el admin y en el público.
- [ ] Probar un archivo no imagen (PDF) y uno muy grande → rechazo con mensaje claro. Anotar el tope real (pending-decisions §8).
- [ ] Crear `ZZ-TEST Ração B` con **variantes** (10kg / 15kg) y con **promo** (precio original > precio).
- [ ] Cambiar de precio simple ↔ variantes en un producto de prueba.
- [ ] Desactivar un producto → desaparece del público; reactivar → vuelve.
- [ ] Hacer un pedido con `ZZ-TEST Ração A` y comprobar que el **stock no cambia** (comportamiento conocido §27).

### 3.4 Pedidos
- [ ] Lista y detalle de los pedidos `ZZ-TEST` de §2.3.
- [ ] PENDING → CONFIRMED → CANCELLED funciona; CANCELLED → otro estado **no** se permite.
- [ ] Botón "Enviar confirmação no WhatsApp" arma el mensaje correcto para el estado (no enviar).

### 3.5 Turnos
- [ ] Lista por día y próximos 7 días; filtro por estado.
- [ ] Confirmar y cancelar un turno `ZZ-TEST`; un turno cancelado **libera** el horario en el wizard público.
- [ ] **Turno manual** (`ZZ-TEST Beto`, `(55) 90000-0003`) fuera de horario o sobre capacidad → se crea con **aviso**, no con error.
- [ ] Marcar/desmarcar **tempo extra** (+20 %): la duración cambia y vuelve al desmarcar (sin deriva).

### 3.6 Servicios y horarios
- [ ] Ver servicios y precios (**no guardar cambios** salvo que estés dispuesto a revertir).
- [ ] Crear bloqueo de agenda con motivo `ZZ-TEST bloqueio` sobre un día futuro → ese día desaparece en el wizard público; borrar el bloqueo → vuelve.
- [ ] Horarios: ver; si se edita uno, **restaurar** al valor anotado en §0.

### 3.7 Privacidad (LGPD)
- [ ] Con un teléfono de prueba (`(55) 90000-0004`) con 1 pedido y 1 turno: *preview* muestra ambos.
- [ ] Anonimizar → los registros quedan como "Titular removido", sin teléfono; el log registra solo el hash.
- [ ] Teléfono inexistente → mensaje de "sin registros" claro (no error 500).

### 3.8 Aislamiento básico
- [ ] Con la sesión cerrada, `GET /api/v1/admin/products`, `/admin/appointments`, `/admin/privacy/...` → 401.
- [ ] `GET /api/v1/appointments/<uuid-inexistente>` → 404.

### 3.9 Reset de contraseña (⚠️ afecta al admin real)
- [ ] Decidir antes: ¿se completa el flujo? Si sí, **definir y guardar la nueva contraseña** antes de empezar.
- [ ] `/admin/esqueci-senha` con el email del admin → mensaje neutro; con un email inexistente → **mismo** mensaje.
- [ ] El email llega (anotar tiempo) **a la bandeja de entrada**, no a spam, en Gmail y en Outlook/Hotmail. Remitente y enlace correctos (`https://frontpet.com.br/...`).
- [ ] Usar el enlace → define nueva contraseña → el enlace **no** sirve una segunda vez.
- [ ] **Invalidación**: con una sesión abierta en otro navegador antes del reset, después del reset esa sesión queda cerrada.
- [ ] Contraseña nueva funciona en el login.
- [ ] 6.º pedido de reset en 15 min desde la misma IP → bloqueado.

## 4. Errores y observabilidad

- [ ] En Coolify, los logs del backend están en **JSON** y no contienen contraseñas, tokens ni teléfonos completos.
- [ ] Sentry: durante la prueba no aparecen errores 500 inesperados (anotar los que sí: ej. doble submit, UTF-8 inválido §22).
- [ ] Plausible registra visitas **solo** de la sesión donde se aceptó el consentimiento.

## 5. Resultados

| # | Sección | Qué falló / observación | Gravedad |
|---|---|---|---|
| | | | |

---

## 6. Limpieza (después de la prueba)

1. **Cerrar sesiones de prueba** y dejar de generar datos.
2. Abrir [`limpieza-datos-prueba.sql`](limpieza-datos-prueba.sql). Editar `t0` con la hora anotada en §0.
3. Correr **PASO 1 (vista previa)** y revisar que **solo** haya filas `ZZ-TEST` / teléfonos de prueba / anonimizaciones de hoy. Si hay algo real → `ROLLBACK;` y ajustar.
4. Copiar las `main_image_url` de los productos de prueba (para §7).
5. Correr **PASO 2 y 3**. Los conteos "restantes" deben ser 0.
6. Comparar los totales con los anotados en §0 (deben ser **iguales**).
7. `COMMIT;`

## 7. Fuera de la BD

- [ ] **R2**: en Cloudflare → bucket `frontpet-products`, carpeta `products/<tenant-id>/` → borrar los objetos de las URLs copiadas en el paso 4 (la BD no borra el archivo).
- [ ] Restaurar horarios/precios si se tocaron (§3.6).
- [ ] Si se completó el reset de contraseña: guardar la contraseña nueva en el gestor.
- [ ] Verificar en el sitio público que ya no aparece ningún `ZZ-TEST` (productos, marca en filtros) y que el dashboard del admin vuelve a los conteos reales.
- [ ] Anotar fecha y resultado de la prueba (para el sprint-close / `WORKING-CONTEXT.md`).

> Lo que **no** se puede deshacer: eventos en Plausible/Meta Pixel (solo si se aceptó el consentimiento), errores en Sentry y el email de reset ya enviado. Ninguno contiene datos personales reales.
