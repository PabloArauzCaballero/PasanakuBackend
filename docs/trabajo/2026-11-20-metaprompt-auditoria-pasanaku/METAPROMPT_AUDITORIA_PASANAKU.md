# Metaprompt — Auditoría profunda por módulo (Pasanaku / AportaYa)

Cómo se usa: cada persona copia el bloque de abajo, completa los tres parámetros del inicio y lo
envía a su agente, en una sesión abierta en la raíz del monorepo `Pasanaku`. El agente devuelve un
único entregable: `AUDITORIA-<modulo>.md`.

Al repartir los módulos: asignarlos por dominio de negocio y sin solapamiento, y darle dueño
también al código transversal (`plataforma/*`, `packages/*`, `contratos/`, `sql/`), que es lo que
todos consumen. Dos módulos tienen dueño obligatorio porque el flujo nuevo los atraviesa:
`aportes` + `nucleo-financiero` (cobro por QR y mayor) y `garantia` + `entregas` (respaldo,
cobertura y entrega del pozo).

Este metaprompt reemplaza a la versión genérica de otro producto. Está escrito solo para
Pasanaku y para el cambio de flujo vigente desde 2026-11-20.

---

```text
PARÁMETROS
- MÓDULO: <dominio, p. ej. "aportes y cobro QR", "garantía y entregas", "grupos y admisión",
  "identidad", "app móvil · pantallas del pasanaku", "código transversal">
- RESPONSABLE: <nombre de la persona que maneja esta sesión>
- CARPETAS CONOCIDAS (opcional): <rutas que ya se sabe que pertenecen al módulo>

═══ 1. CONTEXTO ═══

Pasanaku (marca AportaYa) administra dinero de terceros: cobra aportes por QR, los acredita en un
mayor de doble partida y entrega el pozo a quien le toca el turno. Un importe que aparece o
desaparece rompe el producto entero, porque lo único que un pasanaku vende es confianza. Por eso
esta auditoría trata cada defecto de dinero como más grave que cualquier defecto de pantalla.

El repositorio es un monorepo con microservicios:
- `servicios/` — aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad,
  inversiones, notificaciones, nucleo-financiero, organizador, publicidad, tarifas, transparencia
  (Java; paquete base `bo.aportaya.<servicio>`; casos de uso `CUnn…` en `aplicacion/`).
- `plataforma/` — gateway, comun-archivos, comun-datos, comun-dominio, comun-mensajeria,
  comun-pruebas, comun-web (código transversal).
- `apps/movil` (Flutter), `apps/backoffice` y `apps/web` (Angular), `packages/*` (diseno_flutter,
  dominio-cliente, simulado, tokens, tutoriales, ui), `clientes/{angular,dart}` (clientes
  generados).
- `sql/` (esquema por carpetas numeradas y semillas), `bd/`, `seeders/`, `contratos/pactos/`
  (pactos de contrato entre servicios), `postman/`, `docs/`, `planes/`, `scripts/`.

El código lo escribieron muchos agentes en paralelo y dejó un patrón de daño: mismo código escrito
varias veces; funcionalidades reescritas con las versiones anteriores todavía vivas; agentes que
se pisaron entre sí; parches que tapan el síntoma; resultado frágil (un cambio mínimo rompe lejos);
contratos entre servicios, esquema y clientes que no coinciden; y código que no respeta el diseño
elegido (servicios dueños de sus datos, arquitectura hexagonal, código limpio) ni las reglas de la
casa.

═══ 2. EL CAMBIO DE FLUJO QUE ESTA AUDITORÍA DEBE CONTRASTAR ═══

Pablo (dueño del producto) fijó dos reglas nuevas el 2026-11-20. Esta auditoría mide el módulo
contra ellas, además de medirlo contra la calidad general:

R-NUEVA-1 — Sin aprobaciones en el circuito del pasanaku. Ya no se requiere la aprobación de
nadie (ni persona del grupo, ni organizador, ni backoffice) para que el flujo avance. Lo único que
mueve el flujo es el pago por QR. Se lee así: un aporte queda cobrado cuando la pasarela lo
confirma por webhook con firma verificada (regla 91.3); no cuando el usuario dice que pagó, ni por
el `return_url`, ni por un comprobante subido a mano.

R-NUEVA-2 — El sistema no paga por quien no paga. La empresa ya no aporta el faltante, ni cubre
con respaldo empresarial, ni adelanta dinero propio al pozo por un participante que no pagó.

Esto contradice, en parte, decisiones vigentes escritas en el repo. Tratalas como fuentes a
contrastar, no como verdad: `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y
autogestión.md` (regla 7: «la empresa aporta el faltante»; pozo completo con respaldo
empresarial), `ADR-054 Grupos sin organizador humano.md` (ya quitó la aprobación del organizador
en el alta y el inicio), `ADR-049 Doble aprobación de retiros.md`, y el catálogo
`docs/catalogo-de-flujos/aporte-mora-cobertura.yaml` (pasos `cubrir-faltante` y
`recuperar-respaldo`). Hay que leer cuál de esas fuentes manda hoy según el código real.

AMBIGÜEDADES QUE NO RESOLVÉS VOS (regla 00 §1.7). Se registran con el supuesto tomado y se
escalan a Pablo; nunca se presentan como hecho:

  A-1. ¿«Nada de aprobación» incluye la verificación de identidad (KYC), el cumplimiento
       UIF/ASFI y la doble aprobación de retiros (ADR-049)? Supuesto de trabajo: NO se asume que
       se eliminan. Se listan como DECISIÓN REQUERIDA, porque pueden ser obligaciones legales y no
       pasos de producto.
  A-2. ¿Qué pasa con la entrega del turno cuando falta plata porque alguien no pagó (entrega
       parcial, entrega diferida hasta completar, devolución, reasignación)? Es una decisión de
       negocio. No la inventes: describí las ramas que el código tiene hoy y marcá la que falta
       como DECISIÓN REQUERIDA.
  A-3. ¿Qué pasa con lo ya construido sobre el respaldo (tablas `reserva_respaldo`,
       `cobertura_respaldo`, `recuperacion_respaldo`, `capacidad_respaldo`, `abono_recuperacion`,
       etc.) y con el aval entre participantes (`CU26EjecutarAval`)? El aval lo ponen otros
       participantes, no la empresa: no entra en R-NUEVA-2 salvo que Pablo lo diga.
  A-4. ¿Cómo se trata la mora y el recargo cuando nadie cubre el faltante?

═══ 3. TU TRABAJO ═══

Auditar el módulo MÓDULO y documentar todo lo que esté mal en un solo archivo:
`AUDITORIA-<modulo>.md`. «Todo» es literal: errores, huecos, malas prácticas, deuda, riesgos y
desvíos de estándar, en cualquier capa, más la distancia entre el código y las dos reglas nuevas.
El archivo lo va a leer gente que no estuvo en tu sesión y que tomará decisiones a partir de él:
cada afirmación lleva su evidencia y su ubicación exacta.

Actuá como un auditor externo estricto que evalúa si este módulo puede operar con dinero de
terceros. Un auditor así no aprueba por intuición y tampoco condena por intuición: lo que afirma,
lo leyó o lo ejecutó.

Es un trabajo de documentación. No modificás código, SQL, contratos, pruebas ni documentación del
repo, aunque el arreglo sea evidente (regla 00 §3: lo roto fuera de alcance se anota, no se
arregla). Lo único que escribís vive en `docs/trabajo/<AAAA-MM-DD>-auditoria-<modulo>/`:
- `PLAN.md` mínimo (un hito, una microtarea por cada fase de la sección 8), escrito primero;
- `AUDITORIA-<modulo>.md` (el entregable);
- `REPORTE.md` al cerrar (regla 40), aunque la auditoría quede incompleta, con el avance en la
  primera línea como `microtareas HECHO / total`.

═══ 4. LÍMITES: TU MÓDULO Y SUS CONEXIONES ═══

Otros agentes auditan los demás módulos al mismo tiempo. Si te metés en el de otro, se duplica
trabajo y aparecen dos diagnósticos de lo mismo.

Tu módulo se audita completo, cada archivo de principio a fin, en todas sus capas: servicio Java,
tablas en `sql/`, pacto en `contratos/pactos/`, cliente generado, pantallas de `apps/movil`,
`apps/backoffice` y `apps/web`, y sus pruebas.

Las conexiones también son tuyas: todo punto donde tu módulo llama a otro, es llamado por otro, o
comparte un evento (Kafka/outbox), un pacto, una tabla, una ruta o un componente. Quedarse corto
en las conexiones es un defecto de la auditoría tan serio como invadir un módulo ajeno.

Del código ajeno leé solo lo necesario: el punto de contacto exacto, el fragmento que parece
duplicar algo tuyo, y la parte del código transversal que usás. No lo recorras ni audites su
interior; si ves algo grave, una línea con su ruta en «Observado fuera de alcance».

Regla para decidir si algo es tuyo: si el problema existe por cómo está escrito tu módulo o cómo
se conecta, es tuyo; si existiría igual sin tu módulo, no lo es. Cuando la frontera de una carpeta
sea dudosa, anotá el supuesto y seguí.

Excepción que NO es invasión: los inventarios de la sección 6 (aprobaciones y respaldo) se llenan
con lo que aparezca en tu módulo y en sus conexiones directas. Cada módulo llena su parte; el
consolidado lo arma quien coordina.

═══ 5. REGLAS DE TRABAJO ═══

- Rama: auditá `origin/test` actualizada (el servidor de TEST sigue esa rama). Registrá el commit
  exacto. Si `dev` y `test` difieren en tu módulo, decilo y anotá la diferencia: las líneas
  citadas tienen que poder reproducirse.
- Leé primero `CLAUDE.md`, `AGENTS.md` y `.claude/rules/` completos. Sus reglas siguen vigentes y
  además son criterio de auditoría: el código que las incumple es un hallazgo. Las más pesadas
  para auditar: 91 (dinero), 98 (microservicios), 96 (backend), 97 (datos y esquema), 90
  (seguridad), 95 (frontend).
- Línea base previa: ya existen auditorías en `docs/auditoria-produccion/`,
  `docs/auditoria-recursos/` y `docs/Auditoria-Robustez.md`. Leelas antes de empezar. No repitas
  lo que ya está probado: citá el documento, comprobá si sigue vigente en el commit auditado y
  reportá solo lo que cambió o lo que faltó.
- Datos personales y financieros (regla 90.2): ningún dato de participante, documento, cuenta
  bancaria, QR de cobro real ni importe atribuible a una persona aparece en el informe. Si
  encontrás un secreto en el código, ruta y línea, nunca el valor. Los importes de ejemplo son
  sintéticos y se declaran así.
- Ejecución (regla 70, autorizada en `CLAUDE.md`): hasta 4 subagentes simultáneos con lotes
  disjuntos en archivos; como máximo un build de Gradle/Java y uno de Flutter/Dart a la vez; un
  solo navegador de test; un servidor por puerto. Podés ejecutar verificaciones que no cambian
  nada (análisis estático, linters, verificadores de `scripts/`, pruebas contra la base local).
  No ejecutes migraciones, seeds ni nada contra TEST, producción ni entornos compartidos; no
  toques el servidor de TEST. Lo que lances en background lo cerrás vos antes de terminar y lo
  declarás en el reporte.
- Escribí el informe en disco a medida que avanzás. Después de cada tanda de archivos volcá
  hallazgos y cobertura; si retomás tras un resumen de contexto, releé tu propio archivo.
- No te detengas a preguntar lo que podés resolver leyendo. Tomá el supuesto más razonable y
  anotalo en «Límites». Las ambigüedades de negocio de la sección 2 son la excepción: se
  registran, no se resuelven.
- Todo hallazgo que entre al informe lo verificás vos abriendo el código, aunque lo haya
  encontrado un subagente. No delegues la verificación al agente que lo encontró (70.4.8).
- Vocabulario de estado: usá la escalera de evidencia (regla 30). Leer el código es
  `DISCOVERED`; solo ejecutar sube de ahí. Prohibido «funciona», «probado» o «verificado» sin
  salida pegada.

═══ 6. QUÉ SE BUSCA ═══

Las clases de abajo son el piso, no el techo. Todo lo que un auditor experto señalaría entra al
informe: si no encaja en una clase, va como OTRO.

Preguntas fijas por cada archivo: ¿qué hace y por qué existe? ¿existe ya en otro lado? ¿es
correcto en los casos de borde y de error? ¿está en la capa que le corresponde? ¿qué se rompe si
cambia y qué prueba lo avisaría? ¿qué pasaría con el dinero de un participante real si esto falla?

A0. Flujo nuevo (clases propias de esta auditoría)

- APROB. Todo punto del circuito del pasanaku donde algo espera que alguien apruebe, confirme,
  habilite o valide a mano antes de avanzar: admisión de integrantes, inicio del grupo, sorteo,
  confirmación de aportes o de comprobantes, liberación de la entrega, habilitación de organizador,
  postulaciones, doble aprobación de retiros, verificaciones del backoffice. Por cada uno:
  estado de la máquina de estados, endpoint, permiso, pantalla, evento y tabla que lo
  implementan, y quién lo ejecuta hoy. Búsqueda orientativa (no exhaustiva): `aprobar`,
  `aprobaci`, `APROBAD`, `PENDIENTE_`, `comprobante`, `habilit`, `confirmar`, `postulaci`,
  `requiere_doble_aprobacion`.
- RESPALDO. Todo lugar donde el sistema o la empresa pone dinero propio por quien no pagó:
  `cubrirConRespaldo`, `CU23CubrirFaltanteDelCorte`, `CU23RecuperarRespaldo`, `CU23ReservarRespaldo`,
  `CU23CubrirIncumplimiento`, `CU23ReversarCobertura`, `RespaldoEmpresarial`, `CoberturaAplicable`,
  `RespaldoPorHttp` en entregas, tablas `*_respaldo*`, `politica_cobertura`, asientos y cuentas
  contables de reserva de respaldo, textos de la app y del backoffice que prometen «pozo completo»
  o «te cubrimos», pruebas y semillas que lo asumen. Por cada uno: si hoy es camino vivo, si se
  dispara solo, y qué asiento deja en el mayor.
- FLUJO-QR. El único camino que debe mover el flujo. Verificá de punta a punta: orden de cobro →
  QR → pago → webhook (`CU100RecibirWebhookPasarela`) → `aporte_cobrado` → aplicación al pozo en
  `nucleo-financiero` → siguiente estado del grupo. Qué pasa si el webhook llega dos veces, tarde,
  con firma inválida, con monto distinto, para una orden inexistente o vencida; si el pago llega
  después del corte; si `nucleo-financiero` no puede aplicarlo (`compensar-aporte`). Dónde el flujo
  se puede avanzar sin QR confirmado (comprobante manual, endpoint administrativo, semilla, mock).
- FLUJO-FALTANTE. Con R-NUEVA-2, qué hace hoy el código cuando al corte del turno falta plata:
  ramas existentes, estados, importes, quién queda con qué. Marcá cada rama como VIGENTE,
  CONTRADICE R-NUEVA-2 o DECISIÓN REQUERIDA (A-2). Prohibido inventar la regla faltante.
- COHERENCIA. Fuentes que se contradicen sobre el flujo (ADR, catálogos de flujos, casos de uso
  en `docs/CasosDeUso`, planes, contratos, código, textos de pantalla). Cuál manda según la
  prioridad de evidencia de la regla 00 §8.

A. Desorden del trabajo en paralelo

- DUP-INT. Funciones, validaciones, mapeos, consultas, componentes, estilos o constantes
  repetidos. Distinguí el clon idéntico del clon divergente (misma intención, comportamiento
  distinto): el segundo es más grave, porque una regla de negocio con dos respuestas es un
  defecto. Compará línea a línea.
- DUP-X. Duplicación entre módulos: tipos, utilitarios, validadores, reglas, componentes.
  Buscá por nombre y por concepto, abrí solo el fragmento que coincide, listá todas las
  ubicaciones y decí cuál debería ser la versión única y dónde debería vivir (`plataforma/comun-*`,
  `packages/*`). Incluí el caso inverso: tu módulo reimplementa algo que el transversal ya ofrece.
- REESCRITURA. La misma funcionalidad más de una vez: `v2`, `new`, `legacy`, `old`, dos casos de
  uso para lo mismo, dos pantallas para lo mismo, código sin referencias. Indicá cuál está en uso
  (rutas registradas, inyecciones, imports, consumidores Kafka registrados) y cuál quedó muerta.
- PISADA. Rastros de agentes que se pisaron: lógica revertida y reaplicada, dos soluciones
  conviviendo, convenciones mezcladas en un archivo, ADR que se contradicen. Hay mucho trabajo
  concurrente en `docs/trabajo/` y en las ramas `dev`/`test`: usá el historial.
- PARCHE. Casos especiales por ID o nombre, valores fijos, `catch` que traga el error,
  reintentos y timeouts sin causa demostrada, banderas que nunca se apagan, umbrales literales,
  datos corregidos en la presentación porque el origen llega mal. Nombrá la causa de fondo.
- FRAGIL. Lo que hace que un cambio mínimo rompa lejos: acoplamiento oculto, estado compartido,
  cadenas mágicas repetidas en capas (códigos de error, estados, tipos de obligación como
  `RECARGO_MORA`), efectos laterales en lecturas. Describí la cadena concreta: «si cambia X en A,
  se rompe Y en B y ninguna prueba lo detecta».

B. Corrección: lo que está mal o falta

- DEFECTO. Lógica invertida o incompleta, bordes sin tratar (vacío, nulo, cero, negativo,
  duplicado), errores de uno, fechas y zonas horarias (el calendario y los vencimientos usan
  `America/La_Paz`), feriados, redondeo y residuo al repartir (nunca se pierde: se asigna),
  condiciones de carrera, estados imposibles que el código permite.
- HUECO. Lo declarado y no implementado, métodos que devuelven datos fijos, flujos que empiezan
  y no cierran, operaciones sin su inversa (se cobra y no se puede reversar con contra-asiento),
  permisos definidos y nunca aplicados, datos simulados que llegan a producción, validación en
  una capa y ausente en la que decide.
- RESILIENCIA. Excepciones tragadas, errores sin `correlationId`, escrituras de varios pasos sin
  transacción ni compensación, llamadas entre servicios sin timeout explícito, reintentos sobre
  operaciones no idempotentes, falta de circuit breaker y de degradación declarada,
  guardar-y-publicar sin outbox, consumidores no idempotentes, cadenas síncronas de más de dos
  saltos, mensajes fallidos sin tratamiento (reglas 96.3, 96.7, 98.2, 98.3).

C. Dinero (regla 91) — una clase entera, porque acá el daño no se deshace

- DINERO. Verificá cada una de las cinco prohibiciones de la regla 91.1 y los invariantes de
  91.2: `float`/`double` en cualquier capa (incluido Dart y TypeScript, scripts y planillas);
  importe sin moneda ISO-4217; edición o borrado de asientos, movimientos, bitácoras, eventos de
  reputación o abonos (append-only con `UPDATE`/`DELETE` revocados en el rol de la aplicación);
  operación de dinero sin `clave_idempotencia` con índice `UNIQUE`; saldo calculado con `SUM()`
  ad-hoc sobre pagos en lugar de leerse del mayor (`asiento_contable` + `movimiento_contable`);
  `SUM(debe) = SUM(haber)` por asiento garantizado en la base; deducciones que pueden superar la
  bolsa bruta; comisión del organizador o dinero del grupo pasando por la cuenta del organizador
  (RN-18); desembolso sin cuenta verificada ni periodo de enfriamiento; webhook aceptado sin firma
  verificada incluso en desarrollo; efectos secundarios disparados dentro de la transacción.
- DEBIDO-PROCESO. Marcar mora, sancionar, ejecutar un aval o bajar reputación sin expediente,
  plazo de descargo y apelación (regla 91.4). Atención: con R-NUEVA-1 no se elimina el debido
  proceso de las sanciones; se audita que siga existiendo y que no dependa de una aprobación que
  ya no hay.

D. Diseño: DDD, arquitectura de la casa y código limpio

- DDD. Lenguaje ubicuo roto (mismo concepto con nombres distintos entre servicios, front y
  esquema; mezcla de idiomas en identificadores); modelo anémico; reglas de negocio en
  controladores, DTO, SQL, widgets o en el cliente cuando la autoridad es el servicio;
  agregados sin invariantes o con fronteras violadas; obsesión por primitivos (dinero, moneda,
  fechas, identificadores como `String`/`double`/`int` sueltos en lugar de objetos de valor);
  entidades de persistencia devueltas como respuesta o publicadas como evento (98.1.5); eventos
  de dominio ausentes o usados como llamadas remotas disfrazadas.
- ARQ. Desvíos de la arquitectura de la casa (puertos y adaptadores, ADR-033; propiedad de datos
  por servicio, ADR-017; jOOQ, ADR-016; contratos OpenAPI primero, ADR-020): dominio que importa
  framework, jOOQ o HTTP; casos de uso que hacen varias cosas; controladores gordos; dependencias
  hacia afuera; dependencias circulares; carpetas que no siguen la estructura; archivos por encima
  de los topes de líneas del repo (los verificadores de la sección 8 los miden).
- CLEAN. Responsabilidades mezcladas, funciones largas o con anidamiento y complejidad altos,
  parámetros booleanos que cambian el comportamiento, números y cadenas mágicas, código muerto o
  comentado, comentarios que mienten, `TODO`/`FIXME` (el `TODO` como sustituto de implementación
  está prohibido por la regla 00 §5.4), escapes del sistema de tipos (`any`, `dynamic`, `!`,
  `as`) y supresiones del linter con lo que tapan, cadenas de `if`/`switch` por tipo que crecen
  con cada caso, dependencias concretas creadas adentro en lugar de inyectadas. Los problemas
  repetidos se reportan como un hallazgo de patrón con todas sus ubicaciones. Lo que el linter ya
  controla y pasa en verde no se reporta.

E. Conexiones y contratos

- ACOPLE. Un servicio que lee o escribe tablas de otro (regla 98.1.1 y 98.1.2: prohibido, ni vista,
  ni `JOIN` entre esquemas, ni usuario de solo lectura); importa internos de otro módulo en lugar
  de su interfaz; dependencia circular entre servicios; el front que reimplementa una regla de
  negocio en lugar de pedirla al servicio.
- CONTRATO. Discrepancias entre OpenAPI, implementación del servicio, pacto en
  `contratos/pactos/`, cliente generado (`clientes/*`), modelo de la app, mock de
  `packages/simulado` y esquema en `sql/` (se documentan en la matriz de la sección 9). Cambios
  incompatibles sin versión (98.1.3). Ausencia de prueba de contrato en el lado productor (98.1.4).

F. Calidad por capa

- DATOS. La fuente de verdad del esquema es `sql/` (regla 97.1; verificá en `CLAUDE.md`/ADR-032
  cuál es el mecanismo declarado). Claves foráneas y `CHECK` que faltan; nulabilidad que no
  refleja la regla; `DECIMAL(14,2)`/`(16,2)` frente a otros tipos para dinero; índices sin nombre
  explícito; índices que bloquean escritura; tablas o columnas huérfanas; entidades o clientes
  que no coinciden con el esquema (deriva, 97.2); migraciones irreversibles; RLS incompleto;
  seeds que no son idempotentes, catálogos con datos sin procedencia o inventados presentados
  como reales, o parámetros financieros inferidos (97.4 y 97.5.4).
- RENDIMIENTO. Consultas dentro de bucles, listados sin paginar ni límite, orden no
  determinista, cargas completas donde alcanza una proyección, trabajo pesado en el hilo de la
  petición o de la interfaz, suscripciones y controladores sin liberar, caché sin invalidación.
- PRUEBAS. Rutas de dinero sin prueba; pruebas que no afirman nada; `skip`/`only`/comentadas;
  aserciones debilitadas; mocks del backend presentados como integración (80.5.10); pruebas con el
  ORM mockeado que dicen probar persistencia; falta de matriz de autorización negativa (otro
  usuario, otra organización, rol insuficiente, sin token); falta de prueba de idempotencia en
  mutaciones reintentables; pruebas dependientes del reloj o del orden; pruebas que todavía
  validan el flujo viejo (aprobación o respaldo) y por eso contradicen R-NUEVA-1/2.
- UX-A11Y. En `apps/movil`, `apps/backoffice` y `apps/web`: los cuatro estados (cargando, con
  datos, vacío, error) y los adicionales cuando apliquen (95.2); formularios con error asociado
  al campo, sin doble envío, sin perder lo escrito (95.3); accesibilidad WCAG 2.2 AA (nombre
  accesible, foco, teclado, objetivo táctil, contraste, nada solo por color; 95.4 y 80.7);
  movimiento reducido (95.5); modo claro y oscuro; valores literales de color/espaciado en lugar
  de tokens (`packages/tokens`); textos fijos en el código que impiden cambiar de idioma;
  animaciones sobre dinero que provoquen un malentendido o muestren un saldo en cero.
  Textos y pantallas que todavía hablan de «aprobar», «esperando aprobación» o «cubrimos tu
  faltante» entran como hallazgo APROB/RESPALDO.
- OBSERV. Operaciones de dinero sin log estructurado con `correlationId`, `service`, `version`,
  `actorId` e identificador del agregado; sin traza distribuida en el camino de dinero; datos
  personales o financieros en logs, trazas, URLs o analítica (90.2); ruido que tapa lo
  importante. Hay un enmascarador de PII en la configuración de logs: comprobá que cubre lo que
  el módulo escribe.
- CONFIG. Dependencias con vulnerabilidades conocidas, sin mantenimiento o duplicadas en versiones
  distintas (`yarn.lock`, `gradle.lockfile`, `pubspec`); valores fijos que deberían venir del
  entorno; secretos fuera de lo permitido; configuración que no falla al arrancar cuando falta.
  Contraseña, tokens o claves en cualquier archivo versionado: ruta y línea, nunca el valor.
- DOC. Documentación, plan, ADR, caso de uso, catálogo de flujos, guion de tutorial o comentario
  que describe algo que el código ya no hace — empezando por todo lo que describe aprobación o
  respaldo empresarial.

Lentes propias de cada tecnología:
- Servicios Java: validación de entrada con lista blanca, autorización por rol Y por propiedad del
  recurso en cada endpoint (IDOR/BOLA y BFLA), alcance por contexto de organización, transacciones,
  SQL por concatenación, mapeo de errores a códigos HTTP estables (nunca `200` con cuerpo de
  error), códigos de error documentados, jobs con timeout/reintento/`ShedLock`, consumidores
  idempotentes.
- Angular (backoffice y web): fugas de suscripciones (`takeUntilDestroyed`), acceso a `window` o
  `document` que rompe el SSR, lógica en plantillas, guards usados como si fueran seguridad,
  datos simulados en el paquete final, componentes por encima del tope de líneas.
- Flutter: `dispose` faltantes, `BuildContext` usado después de un `await`, almacenamiento de
  credenciales, permisos del sistema, `Semantics`, comportamiento sin conexión, goldens que
  dependen de la plataforma.

G. Producción y normativa

- SEG. Autorización decidida en el servicio en cada operación (ocultar un botón no cuenta);
  autorización a nivel de objeto y de función; validación de entradas; inyección; respuestas con
  más datos personales de los necesarios; sesiones y tokens (ADR-010, ADR-024, ADR-030); tokens,
  OTP y credenciales persistidos solo como hash con pepper; confianza cero entre servicios: nada
  de `X-User-Id` sin firmar, ni «confiable porque está en la red interna» (98.6); rate limiting en
  autenticación, búsqueda y operaciones costosas.
- TRAZA. Bitácora de quién leyó o cambió qué dato financiero y cuándo (la de lectura separada de
  la de escritura), integridad e inmutabilidad del mayor y de las bitácoras, consentimiento,
  retención y borrado.
- REG. Lo que cada hallazgo compromete en materia regulatoria (sección 7). Reportás riesgo o
  evidencia faltante; la determinación la hace una persona.

═══ 7. ESTÁNDARES DE REFERENCIA ═══

Medí contra estándares, no contra gusto personal. En cada hallazgo citá el estándar solo si lo
conocés con certeza; si no estás seguro de la cláusula, citá solo el estándar; si no estás seguro
del estándar, no lo cites (regla 00 §1.3).

- Reglas de la casa, que mandan sobre todo lo demás: `.claude/rules/` (jerarquía en su README) y
  los ADR de `docs/Arquitectura/`.
- Calidad de software: ISO/IEC 25010 (ubicá cada hallazgo en una característica) e ISO/IEC 5055.
- Seguridad: CWE, OWASP Top 10, OWASP API Security Top 10, OWASP ASVS y OWASP MASVS para la app.
- Accesibilidad: WCAG 2.2 nivel AA.
- Interfaces y datos: OpenAPI 3, semántica HTTP (RFC 9110), formato de errores (RFC 9457), fechas
  ISO 8601, monedas ISO 4217.
- Lenguajes y frameworks: Java (versión del repo), jOOQ, Angular y TypeScript estricto, Effective
  Dart y lints de Flutter.
- Dinero y cumplimiento en Bolivia: el repo ya recoge obligaciones de ASFI y de la UIF (ver
  `sql/10_tablas/12_cumplimiento_asfi`, `servicios/cumplimiento`, `docs/Cumplimiento.md`,
  `docs/Restricciones.md`). Usá esos documentos como base. No cites artículos ni resoluciones de
  memoria: si no están en el repo con su fuente, anotá «norma a identificar por Cumplimiento».
  Para datos personales, la regla 90.2 y `data-privacy-financial`. Pablo dejó constancia de que la
  habilitación de dinero real sigue condicionada al dossier H15
  (`docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F/H15-dossier-habilitacion-real.md`):
  tenelo presente en cualquier hallazgo REG.

No declarés conformidad ni no conformidad con una norma.

═══ 8. MÉTODO ═══

Fase 0 — Preparación
- `git fetch --all`, posicionate en `origin/test` y anotá el commit; anotá también el de
  `origin/dev` si difiere en tu módulo. Revisá `git status` limpio al empezar.
- `git rev-parse --is-shallow-repository`: si es superficial, profundizá o decí que limita lo que
  podés afirmar sobre reescrituras y pisadas.
- Leé `CLAUDE.md`, `AGENTS.md`, `.claude/rules/*`, `PENDIENTES.md`, `PROGRESO.md` y las tres
  auditorías previas de `docs/` (sección 5). Leé los cuatro ADR de la sección 2.
- Creá `PLAN.md` mínimo antes de cualquier otra escritura.

Fase 1 — Huella del módulo y mapa de conexiones
Identificá todo lo que pertenece a MÓDULO: `servicios/<x>` (main y test), el descriptor
`descriptor.yml`, `sql/10_tablas/<carpeta>` y sus claves, índices, reglas y semillas
(`sql/20_claves`, `30_indices`, `35_append_only`, `40_reglas`, `50_verificacion`, `60_semillas`,
`61_dev`, `62_volumen`), `seeders/`, pactos en `contratos/pactos/`, entradas en
`docs/catalogo-de-flujos/` y `docs/CasosDeUso`, clientes en `clientes/*`, `packages/simulado`,
`postman/`, y las pantallas de `apps/movil/lib/pantallas/<x>`, `apps/backoffice/src/app/rutas/<x>`
y `apps/web`. Los nombres cambian entre capas: buscá por concepto, por ruta HTTP, por nombre de
tabla y por tópico de evento (`scripts/listar_topicos.py`, `scripts/inventario_endpoints.py`
ayudan), no solo por carpeta.

Armá el mapa de conexiones: qué consume tu módulo, quién lo consume, mecanismo (HTTP, evento,
tabla, componente) y punto exacto. Para dinero, listá los saltos de la operación (98.8.1).

Fase 2 — Lectura completa con registro de cobertura
Leé cada archivo de la huella de principio a fin, en tandas, aplicando la sección 6 completa.
Una fila por archivo: ruta, líneas, leído completo sí/no, y qué hace en una frase. Un archivo no
leído completo se declara con su motivo. Una auditoría que cubre el 70 % y lo dice es útil; una
que aparenta el 100 % es peligrosa. Los archivos generados (`clientes/*`, `build/`) se
inventarían, no se leen línea a línea; se comparan con su fuente.

Fase 3 — Inventarios del flujo nuevo
Construí las dos tablas de la sección 9 (inventario de aprobaciones e inventario de respaldo) para
lo que hay en tu módulo y conexiones directas. Recorré el flujo del módulo de punta a punta y
decí qué pasa si falla a mitad de camino.

Fase 4 — Vista de conjunto
Con todo leído, mirá lo que solo se ve de lejos: duplicación y reescrituras entre archivos,
fronteras de agregados, flujo de dependencias entre capas, y qué queda inconsistente, y por cuánto
tiempo, si la operación falla en el medio (98.8.6).

Fase 5 — Conexiones y contratos
- Recorré cada conexión del mapa según la clase ACOPLE/CONTRATO y DUP-X.
- Armá la matriz de contratos (sección 9).
- Ejecutá los verificadores que ya existen y pegá su salida recortada: `scripts/verificar_contratos_limites.py`,
  `scripts/validar_flujos.py`, `scripts/verificar_criterios.py`, `scripts/verificar_seguridad.py`,
  `scripts/inventario_endpoints.py`, `scripts/idempotencia.py`, `scripts/matriz_de_pruebas.py`,
  `scripts/cobertura.py`, `scripts/verificar_boveda.py`, `scripts/verificar_carriles.py`,
  `scripts/verificar_frontend.py`, `scripts/verificar_rutas_frontend.py`,
  `scripts/verificar_pruebas_web.py`, y los de `scripts/auditoria/` y `scripts/auditar_backend.py`
  que apliquen; además `yarn lint` y `yarn typecheck` (front) y las tareas de barrido de Gradle del
  servicio (el barrido de tamaño de archivos —tope de 300 líneas—, de umbrales literales y de
  secretos no corre con la tarea `:test`: ejecutalo aparte, de a uno). Si alguno no corre, decilo;
  no lo des por verde. Al leer cada script, confirmá primero qué hace y qué escribe: si genera o
  modifica archivos del repo, no lo corras.

Fase 6 — Estándares y normativa
Pasá los hallazgos por la sección 7: asigná a cada uno la característica de calidad y los
estándares, y revisá el módulo una vez más con la clase G en mano.

Fase 7 — Refutación antes de entregar
Intentá tumbar tus propios hallazgos. Para cada S1 y S2, y una muestra del resto:
- Volvé a abrir cada ubicación y confirmá que las líneas dicen lo que el informe afirma.
- Buscá la explicación que lo desmiente. Falsos positivos típicos acá: una autorización que
  parece faltar en el controlador y está en el filtro o la política de RLS (ADR-007/021); un
  `UPDATE` que parece violar el append-only y está revocado en el rol de la base
  (`sql/35_append_only`); una restricción que parece faltar y vive en `sql/20_claves` o
  `40_reglas`; código que parece muerto y se referencia por consumidor Kafka, por tarea
  programada, por cadena de ruta o desde una plantilla `.html`/`.dart`; un cálculo con `double`
  que es solo de presentación y no de dinero; dos fragmentos que difieren a propósito.
- Lo que no resiste se retira o baja a «sospecha».
- Pasada inversa: por cada clase de la sección 6 sin hallazgos, preguntate si no hay nada o si
  no miraste; si no miraste, mirá.

═══ 9. EL ENTREGABLE: `AUDITORIA-<modulo>.md` ═══

Un solo archivo en español, con estas secciones y en este orden:

1. Ficha — módulo, responsable, fecha, commit auditado de `origin/test` (y de `origin/dev` si
   difiere), si el historial estaba completo, total de archivos y de líneas cubiertos.
2. Resumen — estado del módulo en un párrafo; tabla de hallazgos por clase y severidad; los diez
   más graves, una línea cada uno con su identificador; y una línea con el peldaño de evidencia
   (regla 30) alcanzado por área.
3. Huella y mapa de conexiones.
4. Distancia al flujo nuevo — dos inventarios:
   a. Inventario de aprobaciones: ID · dónde (`ruta:línea`) · qué aprueba y quién · estado,
      endpoint, permiso, pantalla y evento asociados · clasificación: ELIMINABLE (no hay norma
      que lo exija) / CONSERVAR POR NORMA (cita la fuente del repo) / DECISIÓN REQUERIDA (A-1) ·
      qué se rompe aguas abajo si se quita.
   b. Inventario de respaldo y cobertura: ID · dónde · qué pone la empresa y cuándo se dispara ·
      asiento que deja en el mayor · clasificación: CONTRADICE R-NUEVA-2 / NO APLICA (aval entre
      participantes, devolución del participante) / DECISIÓN REQUERIDA (A-2, A-3, A-4) · qué
      depende de ello (pruebas, semillas, pantallas, eventos, pactos).
   c. Tabla de ambigüedades A-1 a A-4 con lo que el código muestra hoy y a quién confirmárselo
      (Pablo; Cumplimiento para lo regulatorio).
5. Hallazgos — agrupados por clase y, dentro de cada una, de mayor a menor severidad, con esta
   ficha exacta:

   ### <MODULO>-<CLASE>-<NNN> — <título que describe el problema>
   - Clase / Severidad / Confianza:
   - Ubicaciones: `ruta:líneas` (todas)
   - Evidencia: fragmentos relevantes, recortados (sin datos personales ni secretos)
   - Cómo se comprobó: qué leíste, buscaste o ejecutaste, y qué salió
   - Impacto: qué se rompe o qué riesgo corre, y para quién (participante, grupo, empresa)
   - Causa de fondo:
   - Solución de fondo: el arreglo que lo resuelve a largo plazo, no otro parche. Si depende de
     una decisión de negocio, decí cuál y no la tomes
   - Relación con el flujo nuevo: APROB / RESPALDO / FLUJO-QR / ninguna
   - Conexiones y módulos afectados:
   - Estándares y normas: característica de ISO/IEC 25010 y estándares que lo respaldan; regla
     de la casa incumplida (número)
   - Esfuerzo estimado: chico / medio / grande

6. Matriz de contratos — una fila por operación HTTP y por evento del módulo, con columnas:
   especificación OpenAPI · implementación en el servicio (controlador y DTO) · pacto en
   `contratos/pactos/` · cliente generado · modelo de la app / del front · esquema en `sql/` ·
   discrepancias. Comparar nombres de campo, tipos, nulabilidad, enumeraciones y sus valores,
   fechas y zona horaria, dinero y moneda, paginación, forma y códigos de error, permiso
   exigido, idempotencia. Para eventos: quién publica y quién consume, y si el consumidor es
   idempotente. Una operación que alguien llama y no existe, o que existe y nadie llama, es
   hallazgo.
7. Evidencia obligatoria del módulo (reglas 91.6, 98.8 y 90.6), solo lo que aplique y con
   estado PASS / FAIL / NO COMPROBADO:
   asientos que genera cada operación de dinero y su suma; idempotencia ejercitada (misma
   operación dos veces); cuadre del mayor contra el total esperado; caso de reversa;
   redondeo declarado; prueba de contrato; prueba de duplicado de mensaje; qué hace el sistema
   con el servicio dependido apagado; traza con `correlationId`; matriz de autorización
   negativa. Lo que no pudiste ejercitar se declara NO COMPROBADO; nunca se completa por
   inferencia.
8. Cobertura por clase — una fila por cada clase de la sección 6 (incluidas APROB, RESPALDO,
   FLUJO-QR, FLUJO-FALTANTE, COHERENCIA y DINERO): qué revisaste, con qué método, cuántos
   hallazgos dio. Ninguna fila vacía.
9. Lo que está bien — lo bien resuelto que conviene preservar o tomar como modelo (por ejemplo,
   si el webhook ya verifica firma, o el mayor ya es append-only).
10. Observado fuera de alcance — una línea por cosa vista en otros módulos, con su ruta.
11. Límites de esta auditoría — lo no leído, no ejecutado, no comprobado y los supuestos.
12. Anexo de cobertura de archivos — la tabla de la Fase 2.

Clases válidas: APROB, RESPALDO, FLUJO-QR, FLUJO-FALTANTE, COHERENCIA, DUP-INT, DUP-X,
REESCRITURA, PISADA, PARCHE, FRAGIL, DEFECTO, HUECO, RESILIENCIA, DINERO, DEBIDO-PROCESO, DDD,
ARQ, CLEAN, ACOPLE, CONTRATO, DATOS, RENDIMIENTO, PRUEBAS, UX-A11Y, OBSERV, CONFIG, DOC, SEG,
TRAZA, REG, OTRO. Si un hallazgo cabe en dos, va en la que mejor describe su causa y menciona la
otra.

═══ 10. ESTÁNDAR DE EVIDENCIA ═══

Un hallazgo sin ubicación exacta y sin fragmento citado no entra. «Convendría mejorar el manejo
de errores» no es un hallazgo: decí dónde, qué pasa y qué rompe.

Confianza:
- confirmado: verificaste todas las ubicaciones leyéndolas o ejecutando algo;
- probable: la evidencia apunta ahí y falta una comprobación concreta, que nombrás;
- sospecha: un indicio que merece que alguien mire.

Para afirmar que algo no se usa, mostrá la búsqueda de referencias. Para afirmar que algo falla,
ejecutalo y pegá la salida. Lo que no pudiste comprobar se reporta como no comprobado. Toda
decisión no trivial cita archivo y línea, contrato, esquema, test o salida real (regla 00 §7).

Severidad (por el daño real, no para que el informe impresione):
- S1 crítico: puede acreditar, perder, duplicar o corromper dinero o el mayor; saltar la
  autorización; exponer datos personales o financieros; un contrato roto en un flujo en uso; un
  camino que avanza el flujo o mueve fondos sin QR confirmado; o un camino por el que el sistema
  pone dinero propio por quien no pagó, en cualquier flujo vivo.
- S2 alto: fallo funcional probable ante un cambio menor, regla de negocio duplicada con
  comportamientos distintos, discrepancia de contrato latente, defecto de lógica en un flujo en
  uso, aprobación residual que bloquea el flujo nuevo.
- S3 medio: duplicación consistente, parche con deuda acotada, desvío de diseño que encarece cada
  cambio, falta de pruebas en una ruta no crítica, documentación que contradice el flujo nuevo.
- S4 bajo: mala práctica o higiene que conviene ordenar.

No recortes la lista para que quede prolija: van todos los hallazgos reales, de S1 a S4. Si una
clase no aparece en tu módulo, decilo y explicá qué revisaste para concluirlo.

═══ 11. CONDICIONES PARA DAR EL TRABAJO POR TERMINADO ═══

- Todos los archivos de la huella figuran en el anexo, leídos o declarados no leídos con motivo.
- Todas las conexiones del mapa tienen su fila en la matriz o su análisis.
- Todas las clases de la sección 6 tienen su fila en «Cobertura por clase».
- Los inventarios de aprobaciones y de respaldo están completos para el módulo (o dicen
  «ninguno encontrado» con la búsqueda que lo sustenta).
- Las ambigüedades A-1 a A-4 están tratadas: ninguna se presenta como decidida.
- Cada hallazgo tiene la ficha completa; ninguno carece de ubicación o de evidencia.
- La Fase 7 está hecha y los S1 y S2 la resistieron.
- Los números del resumen coinciden con los hallazgos del archivo.
- El informe no contiene datos de participantes, importes reales ni secretos.
- No modificaste ningún archivo fuera de `docs/trabajo/<fecha>-auditoria-<modulo>/`: `git status`
  solo muestra esa carpeta.
- `PLAN.md` actualizado y `REPORTE.md` escrito con el avance en la primera línea; sin procesos en
  background, o declarados.

Al terminar, entregale el archivo a RESPONSABLE y cerrá con un mensaje breve: archivos y líneas
cubiertos, hallazgos por severidad, los tres más graves en una línea cada uno, cuántas
aprobaciones y cuántos puntos de respaldo encontraste y cuántos son DECISIÓN REQUERIDA, y qué
quedó sin comprobar.
```
