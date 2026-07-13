# Preguntas para confirmar con el cliente — antes de diseñar la DB

> **Cómo usar este doc**: cada ítem tiene la pregunta en lenguaje natural (para mandar por
> WhatsApp) y abajo, en *cursiva*, el impacto en el desarrollo. La cursiva es para vos — no
> se la mandes al cliente. Ideal mandarlo por bloques (una sección por mensaje) para que no
> se abrume y conteste ordenado.

---

## 1. Catálogo de productos

**1.1 — Variantes**
¿Vendés un mismo producto en varios tamaños o presentaciones con precio distinto? Por ejemplo la misma ración en bolsa de 3kg, 10kg y 15kg. ¿En qué tipo de productos te pasa esto?
> *Define si el precio vive en `product` o si hace falta una tabla `product_variant`. Es lo más caro de cambiar después: afecta catálogo, carrito y pedido. Si no se confirma, no puedo cerrar el schema.*

**1.2 — Stock**
¿Querés que la página muestre "agotado" cuando no hay un producto, o preferís que se vea todo siempre disponible y vos coordinás por WhatsApp lo que hay?
> *Decide si existe columna `stock` y lógica de disponibilidad. Si dice que no le importa, lo dejamos afuera del MVP1 y simplificamos.*

**1.3 — Categorías y especies**
Las categorías que pensamos son: Rações, Petiscos, Higiene, Saúde, Acessórios, Brinquedos, Camas. Y por animal: Cães, Gatos, Aves, Peixes, Roedores. ¿Te sirven así, sacarías o agregarías alguna? ¿Un mismo producto puede estar en varias a la vez (ej: sirve para perros y gatos)?
> *Confirma las tablas `category` / `species` y, sobre todo, si la relación con producto es N:N (un producto en varias). Cambia el modelo de relaciones del catálogo.*

**1.4 — Marca**
¿Querés que el cliente pueda filtrar los productos por marca (Royal Canin, Pedigree, etc.)?
> *Define si hay tabla `brand` y filtro por marca, o si la marca es solo texto. Decisión chica pero hay que tomarla antes de la migración.*

**1.5 — Precios especiales / promos**
¿Manejás precios con descuento o promociones tipo "de tanto por tanto"? ¿O siempre un precio único por producto?
> *Si hay precio promocional, el producto necesita dos campos (precio normal / precio promo). Si nunca hay promos, un solo campo. No lo agrego "por las dudas".*

**1.6 — Imágenes**
¿Cada producto tiene una sola foto o varias (galería)? ¿Las fotos las tenés vos o las sacamos de algún lado?
> *La galería ya está en scope, pero confirma cuántas imágenes por producto y, clave, si el cliente tiene el material o hay que generarlo (bloquea la carga real).*

---

## 2. Servicios y grooming

**2.1 — Precio y duración por tamaño**
En los servicios de baño y peluquería, ¿el precio cambia según el tamaño del perro? ¿Y lleva más tiempo un perro grande que uno chico? Pasame, de cada servicio (Banho & Tosa, Tosa Higiênica, Spa Premium), el precio y cuánto lleva más o menos, por tamaño si aplica.
> *Lo más importante de esta sección. Si varía por porte, el servicio necesita una tabla de precios/duración por tamaño, y la duración por porte entra directo en el cálculo de disponibilidad. Sin esto no puedo modelar booking.*

**2.2 — Capacidad en paralelo**
¿Cuántos animales pueden estar atendiendo al mismo tiempo? ¿Es el mismo número para todos los servicios, o por ejemplo pueden bañar a varios a la vez pero tosar de a uno?
> *Define el número de capacidad del local. Si es un número único, el modelo simple alcanza. Si depende del servicio, hay que revisar la decisión de capacidad antes de codear el algoritmo de cupos.*

**2.3 — Profesionales / recursos**
¿Cuántas personas hacen los baños y la peluquería? ¿Importa quién atiende, o para el turno da igual qué profesional lo toma?
> *Confirma que la capacidad se modela como un número y no como personas nominales (agenda por profesional es bastante más complejo y es Fase 2). Si dice "importa quién", hay que conversarlo.*

**2.4 — Servicios simultáneos del mismo cliente**
¿Puede pasar que un mismo turno junte dos servicios (ej: baño + corte de uñas)? ¿O cada turno es un solo servicio?
> *Decide si un `appointment` apunta a un servicio o a varios. Cambia la relación turno–servicio.*

---

## 3. Booking y agenda

**3.1 — Horarios de atención**
¿Qué días y en qué horario abrís? ¿Es horario corrido o cerrás al mediodía? ¿El sábado es distinto a los días de semana?
> *Define la estructura de `business_hours` (uno o dos rangos por día). Es la base del cálculo de slots: sin esto no se puede generar disponibilidad.*

**3.2 — Feriados y excepciones**
¿Cómo manejás los feriados o cuando te vas de vacaciones? ¿Necesitás poder bloquear un día puntual para que no entren turnos?
> *Define si existe una tabla de excepciones/bloqueos. Si no se contempla, no hay forma de cerrar días sueltos y se rompe la agenda en feriados.*

**3.3 — Anticipación mínima y máxima**
¿Hay un mínimo de aviso para reservar? (ej: que no te reserven para dentro de una hora). ¿Y un máximo hacia adelante? (ej: no dejar reservar a más de 30 días).
> *Son las reglas que filtran qué slots se ofrecen. Valores que van a `tenant.config`. Si no se definen, hay que asumir defaults y validarlos igual.*

**3.4 — Cómo se confirma un turno hoy**
Hoy, sin sistema, ¿cómo arreglás un turno? ¿La persona te escribe por WhatsApp y vos anotás? ¿Le confirmás vos o queda agendado al toque?
> *Define el flujo de estados del turno (¿nace PENDING y vos confirmás, o entra directo CONFIRMED?). Impacta el modelo de estados y el flujo del admin.*

**3.5 — Cancelaciones**
Si alguien quiere cancelar un turno, ¿cómo lo hace hoy? ¿Te avisa por WhatsApp y vos lo sacás de la agenda?
> *Como en MVP1 no hay login de cliente, lo más probable es que cancele el admin. Confirma el flujo de cancelación y justifica los timestamps de estado.*

**3.6 — Flujo completo del servicio (contámelo como pasa)**
Contame exactamente qué pasa desde que una persona reserva un baño hasta que el servicio termina. Paso por paso, como lo vivís vos.
> *Pregunta abierta, vale oro. De acá salen estados, notificaciones y campos que no aparecen preguntando de a uno. Escuchá y anotá lo que no encaja con lo que asumimos.*

---

## 4. Pedidos y WhatsApp

**4.1 — Flujo completo del pedido (contámelo como pasa)**
Contame exactamente qué pasa desde que una persona hace un pedido hasta que recibe el producto. Todo el camino, como es hoy.
> *Igual que el del baño: revela datos y pasos que el negocio necesita y que no surgen de preguntas cerradas. Base para diseñar `order` y su flujo.*

**4.2 — Datos del checkout**
Cuando alguien arma un pedido de productos, ¿qué datos le pedimos? ¿Nombre y teléfono nada más? ¿Hace falta algo más?
> *Define los campos del cliente en `order`. Pedir de más molesta, pedir de menos te deja sin info para procesar. Hay que cerrarlo.*

**4.3 — Precio y Zona Entrega**
 El costo de envío es a combinar o es gratis siempre? ¿a qué zonas llegás? a algun lugar de rivera o livramento que no llegues?
> *Si hubiera zonas con costo, cambia el modelo. Confirmar que sigue siendo "a combinar" cierra esto sin agregar complejidad.*

**4.4 — Qué necesitás para procesar el pedido**
Cuando te llega un pedido, ¿qué información necesitás sí o sí para prepararlo y entregarlo?
> *Valida que los campos del pedido alcanzan para la operación real. Evita rediseñar `order` después de la primera semana de uso.*

---

## 5. Panel admin

**5.1 — Quién lo usa y desde dónde**
El panel de control, ¿lo vas a usar vos solo o alguien más del local también? ¿Lo abrís más desde la computadora o desde el celular?
> *Confirma desktop-first (decisión ya tomada) y cuántos usuarios admin hay. Impacta auth y el diseño de las pantallas.*

**5.2 — Qué decisiones tomás mirando el panel**
Cuando abrís el panel a la mañana, ¿qué es lo primero que querés ver para arrancar el día? ¿Qué te ayuda a decidir?
> *Ordena qué va arriba en el dashboard. Si lo primero que mira son los turnos del día, eso manda; si son los pedidos, otra cosa. Define prioridad visual.*

**5.3 — Expectativa del panel (para no prometer de más)**
En esta primera versión el panel te muestra: pedidos de hoy / del mes / total, turnos del día y de los próximos días, productos más pedidos y servicio más reservado. Cosas como facturación mensual, clientes que vuelven o tasa de conversión las dejamos para una segunda etapa, porque necesitan pago online y un seguimiento que ahora no entra. ¿Con esto arrancás bien?
> *Pregunta de protección. Cerrarla por escrito evita que en tres semanas pida "la facturación" cuando no es calculable sin pagos. Dejalo confirmado.*

---

## 6. Contenido y assets

**6.1 — Sistema de diseño / identidad de marca**
¿Tenés un manual de marca o sistema de diseño armado? Necesito tu logo en buena calidad (preferentemente vectorial / SVG o PNG con fondo transparente) y los colores oficiales de FrontPet (los códigos exactos si los tenés).
> *Sin el logo y la paleta oficial no puedo cerrar el diseño visual. Si no tiene códigos, los sacamos de las piezas que ya usa, pero hay que confirmarlos.*

**6.2 — Cantidad real de productos**
¿Más o menos cuántos productos vas a querer cargar al principio? ¿Diez, cincuenta, doscientos?
> *Define el esfuerzo de carga y si conviene un import masivo o carga manual. También orienta búsqueda e índices.*

**6.3 — Material de fotos y textos**
Las fotos de los productos y los textos (descripciones), ¿los tenés vos o hay que armarlos? ¿Tenés fotos del local para la landing?
> *Bloquea la carga real de contenido. Si el material no existe, hay una tarea previa de generación que no estaba en el plan.*

**6.4 — Textos de la marca**
¿Hay algún texto que quieras sí o sí en la web (una frase, una historia del local, algo del "sobre nosotros")?
> *Define el copy de la landing. Si no lo tiene, lo redactamos y se lo pasamos a aprobar.*

---

## 7. Técnico y operativo

**7.1 — Carga de productos / exportación**
Los productos que ya vendés, ¿los tenés cargados en algún sistema o planilla de donde se puedan exportar (Excel, otro sistema, el catálogo de Instagram)? ¿O hoy no están en ningún lado digital?
> *Decisión clave: si puede exportar, evaluamos un import masivo (CSV) y le ahorramos cargar todo a mano. Si no hay nada, es carga manual y eso cambia el esfuerzo y las herramientas del admin.*

**7.2 — Quién carga los productos**
Una vez que esté el sistema, ¿quién va a cargar y mantener los productos al día? ¿Vos, alguien del local?
> *Define cuán simple tiene que ser el ABM de productos y si hace falta capacitación. Impacta el diseño del admin.*

**7.3 — Número de WhatsApp real**
¿A qué número de WhatsApp tienen que llegar los pedidos y las reservas? Pasame el número tal cual, con característica de país.
> *Es el destino de todos los botones de WhatsApp del sitio. Sin esto, el flujo principal del MVP no funciona.*

**7.4 — Dominio**
¿Tenés un dominio comprado para la web (ej: frontpet.com.br) o hay que conseguir uno? ¿Tenés preferencia de nombre?
> *Necesario para el despliegue. No urge para codear, pero conviene saberlo ya para no frenarse después.*

**7.5 — Facebook Page y Meta Pixel**
¿Tenés página de Facebook del negocio? ¿Alguna vez configuraron el Pixel de Meta para publicidad?
> *El Pixel es parte del scope (tracking de Contact, Schedule, Purchase). Si ya existe una cuenta de Business Manager, lo conectamos; si no, hay que crearla.*

**7.6 — Instagram**
Confirmame que el Instagram oficial es @frontpet.br y que lo manejás vos. ¿Lo querés enlazado desde la web?
> *Confirma la fuente de verdad de marca/idioma (PT-BR) y el link social de la landing.*

---
# Respuestas

## Checklist de cierre (lo mínimo para arrancar a codear)

Estas son las que, si no están, te frenan el diseño de la DB:

    - [ ] 1.1 Variantes de producto
    - [ ] 1.3 Categorías/especies y si es N:N
    - [ ] 2.1 Precio/duración por tamaño
    - [ ] 2.2 Capacidad en paralelo
    - [ ] 3.1 Horarios de atención
    - [ ] 3.4 Cómo se confirma un turno hoy
    - [ ] 4.2 Datos del checkout
    - [ ] 4.3 Entrega o retiro
    - [ ] 5.3 Expectativa del panel
    - [ ] 7.1 Carga / exportación de productos
    - [ ] 6.1 Logo y paleta oficial
    - [ ] 7.3 Número de WhatsApp real


## Catalogo de producto
1.1 Variantes
Tenemos variaciones en raciones (3k,10k,15k,20kg), tapetes higienicos(7unidades, 30 unidades, 60u pero no se trabaja ahora), sobre todo en alimentos e higiene
1.2 stock
Me gustaria que aparezca agotado.
1.3 categorias. 
Racoes / Acessórios / higiene / Petiscos / Conforto/ Brinquedos. En conforto entra lo que es casas, camas, colchonetes etc)
Caso sirva para perros y gatos Puede estar en varias paginas sii
1.4 Marca
Seria Interesante que pueda filtrar si
1.5 precios y promos.
Trabajamos con promos de tanto por tanto y promos por dia
1.6 imagenes
Cada producto tiene una sola foto

## Servicios
2.1 el precio cambia por tamaño del perro, P(incluye filiotes) M G GG, diferente duracion y segun tipo de pelo, 2 modelos de banhos, esencial y premium, esencial es un baño completo, el premium suma corte de unha, limpieza dental, spray bucal, limpieza de oidos, servicios adicionales: tosa higienica, tosa completa, carding(remocion de pelos muertos), hidratacion, banho antisceptico y banho antipulga, proximamente cromoterapia, pendiente precio de cada uno y duracion(variante segun tamaño y servicio)
2.2 cant animales al mismo tiempo 2 para cualquier servicio. servicio extra 1 perro por horario(disponibizilable) solo la mañana  no es el foco de momento
2.3 2 personas en simultaneo, si importa pero es una caracteristica interna al negocio, asi q puede pasar por alto
2.4 cada turno es solo un servicio

## Booking y agenda
3.1 9 a 17 lunes a viernes 9 a 19 sabado(horario del baño), 9 19 lunes a sabado no cierra al medio dia
3.2 si se debe bloquear dias en particular
3.3 no problem
3.4 se comunican por whatsapp y se agenda en el sistema del erp q tiene agenda de banho
3.5 cancelaciones y reagendiemiento solo por whatsapp manejado por admin
3.6 el cliente por whatsapp contigo, le ofreces lo horarios y dias disponibles, el cliente elige uno, vos lo ingreses a tu erp,  el perro viene al petshop el dia indicado( lo pueden ir a buscar), recepcion del animal, se registra el animal, se pregunta observaciones todas, se retoca el servicio(puede cambiar segun las observaciones del perro), adicionando servicios extra, avaliacao, canil, banho, fin de servicio, avaliacao f…

## Pedidos y Whatsapp
4.1 El cliente hace el pedido, frontpet recibe, ingresa el pedido al erp, pasa a logística, logística prepara y entrega 
4.2
Nombre*
Número de teléfono*
Direccion *
Forma de pago*
Horario de entrega*
Cuál es pedido
4.3 
Gratis hasta 5km llegamos en toda rivera y livramento 
4.4 todo lo de 4.1

## Panel Admin
5.1 Funcionarios, más pc futura tablet posible
5.2 Ventas y agendamiento
5.3 Pedidos del mes no es interesante al menos para q lo vean todos

## 6 pendiente hacer

## 7.3 por ver, podemos comprar chip nuevo, por ahora al número frontpet