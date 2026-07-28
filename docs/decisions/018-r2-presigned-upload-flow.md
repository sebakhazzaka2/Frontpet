# ADR 018 — Flujo de firmado para upload de imágenes a Cloudflare R2

**Estado**: Aceptada
**Fecha**: 2026-07-27
**Sprint**: 3 (tarea 3.5/3.5b)

---

## Contexto

R2 ya estaba elegido como storage a nivel infra (ADR 001) — lo que faltaba decidir era **cómo**
el admin sube la foto de un producto sin que esos bytes pasen por el backend (el único server
que también sirve el resto de la API, sin capacidad de sobra para proxyear archivos) y, dentro
de eso, qué puede garantizar realmente ese mecanismo.

La issue de la tarea 3.5 pedía explícitamente un ADR corto antes de codear — no se hizo en su
momento (se documentó en `docs/pending-decisions.md` en cambio, que es para decisiones *no*
tomadas), así que este ADR se escribe retroactivamente sobre lo ya implementado.

Dos preguntas concretas necesitaban respuesta antes de codear:
1. ¿Con qué SDK/config se firma una URL contra R2, que es S3-compatible pero no S3?
2. ¿Qué de lo que pide 3.5b (mime permitido, tamaño máximo, expiración corta "firmados en la
   política") se puede aplicar de verdad del lado de R2, y qué no?

La segunda resultó tener una respuesta menos obvia de lo esperado — ver Decisión.

---

## Decisión

**AWS SDK v2 (`software.amazon.awssdk:s3`), pero solo `S3Presigner` — nunca `S3Client`.** El
backend no sube ni baja bytes de R2 en ningún punto del flujo: firma la URL, el browser hace el
PUT directo, y recién después el admin manda `mainImageUrl` en el alta/edición del producto.

### Config específica de R2 (no es la config default de S3)
- `region(Region.of("auto"))` — R2 exige este valor exacto; el SDK no lo valida contra la lista
  real de regiones AWS, así que funciona aunque no sea una región real.
- `endpointOverride` al endpoint de cuenta de R2 (`R2_ENDPOINT`).
- `S3Configuration.pathStyleAccessEnabled(true)` — sin esto, R2 devuelve 403/`HeadBucket`
  porque no hay DNS por-bucket como en S3.

Los tres puntos están confirmados contra la documentación oficial de Cloudflare
(`developers.cloudflare.com/r2/examples/aws/aws-sdk-java/`), no inferidos del comportamiento de
S3.

### Qué queda firmado y aplicado, y qué no
- **`Content-Type`**: se setea en el `PutObjectRequest` antes de presignar → queda firmado. Un
  PUT real con otro tipo distinto rompe la firma (403). Confirmado contra la doc de R2. Esto sí
  es una garantía real.
- **`Content-Length` / tamaño máximo**: **no es una garantía real.** R2 no soporta
  `content-length-range` ni presigned POST (a diferencia de S3) — confirmado contra la doc
  oficial y contra issues de los SDKs de AWS (Go, JS) que documentan que firmar el
  `Content-Length` tampoco lo aplica de forma confiable del lado del servidor. `5MB` es un
  chequeo sobre el valor que el cliente *declara* al pedir la URL, no algo que R2 verifique
  contra los bytes reales del PUT.

### Por qué se acepta ese límite igual
El endpoint de presign vive detrás de `anyRequest().authenticated()` (JWT en cookie, ADR 004) —
nadie sin sesión de admin puede pedir una URL. El único que podría declarar un tamaño chico y
subir algo mucho más grande ya tiene, con esa misma sesión, acceso a todo el CRUD del catálogo.
El costo real de storage en R2 (~$0.015/GB/mes, sin egreso) hace que el peor caso sea barato.

### Otros detalles de la firma
- Key del objeto: `products/{tenantId}/{uuid}.{ext}` — aleatoria, no el nombre original del
  archivo (evita colisiones y no filtra info del admin).
- Expiración de la URL firmada: 5 minutos.

---

## Alternativas consideradas

### ❌ Presigned POST (S3 POST policy con `content-length-range`)
Es el mecanismo estándar de S3 para topear tamaño de verdad. Descartada: la doc oficial de R2
confirma que **no soporta POST** en absoluto — solo GET/HEAD/PUT/DELETE firmados.

### ❌ Upload proxyado por el backend
El backend recibe el archivo, valida el tamaño real (bytes en mano, no declarado), y recién ahí
lo sube a R2. Daría una garantía dura de tamaño. Descartada para MVP1: le suma carga de subida
de archivos al único server que también sirve el resto de la API (sin capacidad de sobra), y la
AC de la tarea 3.5 ya asumía explícitamente "frontend sube directo a la URL firmada".

### ❌ `HeadObject` post-upload + borrado si excede el tope
Requeriría un `S3Client` además del `S3Presigner` actual, y un endpoint nuevo de "confirmar
upload" que el frontend tendría que integrar — no estimado en 3.5/3.5b. Queda documentada como
candidata en `docs/pending-decisions.md` §8 si el modelo de amenaza cambia (multi-tenant real,
más admins, señales de abuso).

---

## Consecuencias

### Positivas
- Cero carga de bytes de imagen en el backend — el server solo firma, nunca transporta.
- `Content-Type` sí queda garantizado por R2: no se puede subir un `.exe` disfrazado de imagen
  aunque el admin manipule el request a mano.
- Expiración corta (5 min) acota la ventana en la que una URL filtrada sería explotable.

### Negativas / a vigilar
- El tope de 5MB **no es una garantía de storage**, es un chequeo honesto sobre lo declarado.
  Documentado en `docs/pending-decisions.md` §8 — revisar si MVP1 deja de ser single-admin.
- Suma una env var no prevista en la lista original de la tarea 3.5 (`R2_PUBLIC_URL`, para
  construir el link final después del upload) — requiere habilitar acceso público r2.dev del
  bucket (o el dominio custom del Sprint Despliegue) antes de poder probar el flujo end-to-end
  desde el browser.
- Nada en el código de MVP1 redimensiona ni reencodea la imagen — el spec recomendado (WebP,
  máx 1200x1200, <200KB) depende de disciplina externa. Documentado como hueco aparte en
  `docs/pending-decisions.md` §7 (candidato a resolverse client-side, Canvas API).
