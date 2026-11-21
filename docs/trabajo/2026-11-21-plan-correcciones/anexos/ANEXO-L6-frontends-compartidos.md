# Anexo L6 — Correcciones por hallazgo

Este anexo convierte en microtareas los hallazgos de `AUDITORIA-frontends-compartidos.md` (lote L6, `origin/test` 07fc6971). El informe tiene 42 fichas `### frontends-compartidos-*`, y las 42 están trazadas al final. Además, los 40 candidatos del §5-bis del informe (sospechas que nadie volvió a verificar) van listados aparte como **«a re-verificar en F4»**: no se cuentan como hallazgos.

Decisiones que aplica, de `2026-11-20-auditoria-consolidado/DECISIONES.md`:
- **P-01/P-02 (modelo A, «Tu garantía vuelve»):** el fondo es del grupo y no de la empresa; al cierre del ciclo se devuelve.
- **P-04:** se elimina el respaldo de la empresa.
- **P-06:** pagar con saldo recargado por QR cuenta como pago por QR.
- **P-07:** se eliminan el comprobante manual y los canales manuales.
- **P-08:** el KYC lo valida un proveedor y una persona solo revisa las excepciones.
- **P-09:** los retiros son automáticos con controles.
- **P-11:** se elimina el organizador.
- **Tanda 5:** se entra a un grupo solo por invitación.
- **P-16:** los textos nuevos los redacta Claude y los aprueba Pablo.

Todos los hallazgos de origen están en el peldaño `DISCOVERED`. DINERO-002 es la excepción: se reprodujo como función, pero no en Dart. Ninguna microtarea de este anexo se ejecutó.

## Resumen

**Microtareas por fase** (40 en total)

| Fase | Microtareas |
|---|---:|
| F0 | 2 |
| F1 | 10 |
| F2 | 2 |
| F3 | 5 |
| F4 | 21 |

**Hallazgos por fase** (la fase de la microtarea principal). FLUJO-QR-002 no tiene acción y se cuenta aparte, en la columna «—».

| Fase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| F0 | 0 | 0 | 2 | 0 | 2 |
| F1 | 1 | 6 | 4 | 0 | 11 |
| F2 | 0 | 2 | 1 | 0 | 3 |
| F3 | 0 | 4 | 1 | 0 | 5 |
| F4 | 0 | 2 | 15 | 3 | 20 |
| — (sin acción) | 0 | 0 | 1 | 0 | 1 |
| **Total** | **1** | **14** | **24** | **3** | **42** |

**Hallazgo que se resuelve con otra microtarea:** ninguno en este lote. APROB-001 y RESPALDO-002 dependen de los retiros de L3 y L2, pero cada uno tiene su propio cambio en el frontend.

**Hallazgo sin acción:**

| Hallazgo | Motivo |
|---|---|
| FLUJO-QR-002 | P-06: el débito del saldo de la billetera recargado por QR confirmado cuenta como pago por QR. Que el catálogo contemple «cobrar el aporte contra el saldo» pasa a ser correcto. La cadena del lado del servidor la verifica el lote L1. |

**Ambigüedades abiertas:** ninguna nueva. Los textos de F3 dependen de que Pablo los apruebe (P-16). La parte regulatoria depende de Cumplimiento.

---

## F0 — Preparación (contratos base)

### F0.L6.M01 — Catálogo de códigos de error publicado en el contrato y clientes verificados contra él
- **Hallazgos:** FRAGIL-001 (S3).
- **Causa de raíz:** los códigos de error no se publican como catálogo versionado en el OpenAPI. Cada cliente los copia a mano: `apps/movil/lib/dominio/errores.dart` tiene 50 y `apps/backoffice/src/app/nucleo/errores.ts` tiene 74. Además, hay números reutilizados por operaciones distintas, que el cliente desambigua por la URL.
- **Corrección de raíz:**
  - Cada `openapi/*.yaml` publica su tabla de códigos con un significado único (RFC 9457 `type`).
  - Los códigos que hoy se reutilizan se separan con versión del contrato.
  - Los catálogos de la app y del backoffice se generan del contrato, o se verifican contra él en `scripts/verificar_frontend.py`.
  - La microtarea F4.L3.M27(c) se apoya en esta.
- **CA:** dado un código nuevo o renombrado en un contrato, cuando corre el verificador sin el texto correspondiente en un cliente, entonces falla.
- **DoD:**
  - `python scripts/verificar_frontend.py todo` en verde;
  - prueba de mutación: quitar un texto y ver el rojo;
  - `grep` de códigos duplicados entre operaciones con resultado vacío.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F0.L6.M02 — Los clientes generados no se versionan y salen siempre del artefacto
- **Hallazgos:** CONTRATO-002 (S3).
- **Causa de raíz:** hay 26 archivos de `clientes/` versionados con `git add -f`, aunque `.gitignore:50-51` los excluye. Un clon trae fragmentos viejos que pisan lo generado.
- **Corrección de raíz:**
  - `git rm --cached` de esos 26 archivos.
  - El build y la CI generan los clientes desde el artefacto por SHA (`docs/auditoria-recursos/FINAL.md` §20).
  - Un chequeo falla si `git ls-files clientes` no está vacío.
- **CA:** dado un clon limpio, cuando se construyen las apps, entonces los clientes provienen del contrato actual y no queda ningún archivo de `clientes/` en el índice.
- **DoD:**
  - `git ls-files clientes | wc -l` igual a 0;
  - build de las tres apps en verde.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

---

## F1 — Seguridad e integridad del importe

### F1.L6.M03 — `CampoMonto` interpreta sin ambigüedad el separador decimal
- **Hallazgos:** DINERO-002 (S1).
- **Causa de raíz:** `aCadenaDelContrato` (`packages/diseno_flutter/lib/atomos/campo_monto.dart:30-49`) trata el punto solo como separador de miles (es-BO) y lo borra, aunque el formateador (`:72-73`) lo acepta. «12.50» se envía como `1250.00` en recargar, retirar, transferir, puja, subasta y crear grupo. `campo_monto_grande.dart:50` tiene el mismo problema.
- **Corrección de raíz:**
  - Un único parser de importes, el de `Monto`/`packages/tokens` con sus vectores `vectores/monto.json`, usado por los dos átomos.
  - Si hay un solo separador seguido de 1 o 2 dígitos al final, se toma como decimal.
  - Una entrada ambigua (`1.240.50`, o mezcla de punto y coma) se rechaza con un error asociado al campo (95.3.1).
  - El campo muestra al vuelo el importe interpretado, con el separador de la configuración regional.
  - El candidato A-H02 del §5-bis (`valorInicial`) se corrige en el mismo cambio si se confirma.
- **CA:**
  - Dado «12.50», «12,50» o «12.5», cuando se envía, entonces el contrato recibe `12.50`.
  - Dado «1.240», entonces se interpreta como `1240.00` o se pide confirmar.
  - En ningún caso viaja un importe 100 veces mayor.
- **DoD:**
  - pruebas de unidad con los vectores compartidos (`12.50`, `12.5`, `1.240`, `1.240.50`, `0.01`, vacío);
  - prueba de widget en cada una de las seis pantallas de dinero;
  - `flutter test` en la máquina de Pablo (P-19), porque hoy no hay SDK;
  - prueba visual en tres viewports y dos temas.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F1.L6.M04 — El patrón de importe de `cumplimiento.yaml` es válido y no admite negativos
- **Hallazgos:** CONTRATO-003 (S2).
- **Causa de raíz:** el escape de YAML en `servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml:1251,1339` no se verificó. El patrón rechaza `"100.00"` y admite `-?`, en contra de la regla 91.2.2. El mock lo «cumple» con basura.
- **Corrección de raíz:**
  - Patrón `'^\d+\.\d{2}$'` en comillas simples, con versión del contrato.
  - Ejemplos reales en `packages/simulado/ejemplos/cumplimiento/{elevarDiligencia,verificarAlcance}.json`.
  - Prueba de CI que valide un valor real por cada patrón de importe de todos los contratos. Comparte verificador con F0.L5.M05.
- **CA:** dado `"100.00"`, el patrón valida. Dado `"-1.00"`, no valida.
- **DoD:** prueba de patrones sobre todos los `openapi/*.yaml` con su salida pegada.
- **Depende de:** F0.L5.M05.
- **Esfuerzo:** chico.

### F1.L6.M05 — La acción «simular pago» solo existe en compilaciones de prueba
- **Hallazgos:** FLUJO-QR-001 (S3).
- **Causa de raíz:** el modo de prueba vive en el mismo binario y en el mismo contrato público que el modo real. `apps/movil/lib/dominio/cu10_pago_de_prueba.dart` trae el botón, y del lado del servidor `RecargasDePruebaController` acredita saldo sin webhook. Lo único que lo apaga es una propiedad de despliegue.
- **Corrección de raíz:**
  - En la app, el código de `cu10_pago_de_prueba.dart` y su botón se compilan solo con un `--dart-define` de entorno de prueba. El build de release falla si viene ese define.
  - En el servidor (lote L1), el arranque falla con `modo=prueba` en un perfil productivo, y `/recargas-de-prueba` sale del contrato productivo.
  - El doble de pasarela con firma real (regla 65) reemplaza al simulador en las pruebas.
- **CA:** dado un APK de release, cuando se busca el símbolo o el texto del simulador, entonces no aparece.
- **DoD:**
  - `flutter build apk --release` con inspección del binario;
  - prueba de configuración del build.
- **Depende de:** la microtarea del lote L1 sobre `RecargasProveedorLocal`.
- **Esfuerzo:** chico.

### F1.L6.M06 — «Gastable» lo calcula el mayor, no el navegador
- **Hallazgos:** DINERO-001 (S3).
- **Causa de raíz:** el contrato de `nucleo-financiero` no entrega `gastable`, y `packages/dominio-cliente/src/posiciones.ts:80-101` lo deduce restando. Si el libro es incoherente, recorta en silencio (regla 91.1.5).
- **Corrección de raíz:**
  - `nucleo-financiero` devuelve `gastable` calculado desde el mayor, con versión del contrato.
  - El cliente lo muestra tal cual. Ante un comprometido mayor que el disponible, muestra un error accionable y no un número.
  - Se retira la resta del paquete.
- **CA:** dado un saldo con comprometido mayor que disponible, cuando el operador lo consulta, entonces ve un estado de error y no una cifra recortada.
- **DoD:**
  - prueba de contrato del campo nuevo;
  - prueba del componente en tres niveles: normal, borde (gastable 0) e inválido (incoherente).
- **Depende de:** F4.L5.M52 (API de saldos del mayor).
- **Esfuerzo:** chico.

### F1.L6.M07 — El validador del gateway compara orígenes resueltos
- **Hallazgos:** SEG-001 (S3).
- **Causa de raíz:** `packages/dominio-cliente/src/configuracion.ts:52,80-89` valida la cadena por prefijos. Con eso, `/\host-ajeno/api/v1` pasa y el navegador lo resuelve a otro host, junto con el `Bearer`.
- **Corrección de raíz:** siempre `new URL(cruda, origenPropio)`, y se exige `origin === origenPropio` o que el host esté en la lista blanca.
- **CA:** dados `/\x`, `/%5C`, `\\x` o `//x`, el validador los rechaza.
- **DoD:** casos agregados a `configuracion.spec.ts` y `yarn test` del paquete.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L6.M08 — El SSR público emite cabeceras de seguridad y su proxy usa lista blanca y timeout
- **Hallazgos:** SEG-002 (S2).
- **Causa de raíz:** se asumió un NGINX que no existe en la imagen (`apps/web/docker/Dockerfile.web`). El proxy `/api` de `apps/web/src/server.ts:55-85` reenvía por lista negra y sin timeout. Además, el backoffice tiene dos CSP distintas.
- **Corrección de raíz:**
  - En `server.ts`, agregar CSP, `frame-ancestors 'none'`, HSTS, `X-Content-Type-Options` y `Referrer-Policy`.
  - Reenviar por lista blanca de cabeceras.
  - `AbortSignal.timeout` en el `fetch`.
  - Una sola CSP versionada para el backoffice (`nginx.conf` y `nginx.desplegado.conf`).
- **CA:** dada cualquier respuesta HTML del sitio, entonces trae las cabeceras. Un gateway colgado corta en el timeout.
- **DoD:**
  - prueba de integración del servidor SSR que inspecciona las cabeceras;
  - prueba con gateway lento;
  - `curl -I` contra TEST pegado.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F1.L6.M09 — Las exportaciones de datos personales solo pasan por el servidor, con permiso y bitácora
- **Hallazgos:** SEG-003 (S2).
- **Causa de raíz:** el componente compartido trae un valor por omisión inseguro (`packages/ui/src/listado/listado.ts:50`, `exportable = input(true)`), y el canal auditado `POST /extraccion/exportaciones` depende de un servicio que no existe.
- **Corrección de raíz:**
  - `exportable` pasa a ser `false` por omisión.
  - Para datos personales, la exportación se hace con un endpoint del servicio dueño, con permiso `exportar:*`, enmascarado igual que la vista y registro en la bitácora de lectura (`comun.registro_acceso_datos`).
  - Se retira `exportador.ts` hacia el servicio inexistente.
- **CA:** dado un listado de participantes, cuando un operador sin `exportar:*` lo abre, entonces no hay botón de exportar. Con el permiso, la exportación queda registrada con quién y qué.
- **DoD:**
  - prueba del componente;
  - autorización negativa del endpoint;
  - consulta de la bitácora.
- **Depende de:** F3.L5.M40 (bitácora de lectura).
- **Esfuerzo:** medio.

### F1.L6.M10 — El rastro de lectura de expedientes se registra en el servidor que sirve el dato
- **Hallazgos:** TRAZA-001 (S2).
- **Causa de raíz:** el cliente se adelantó al servidor. `registro-de-acceso.interceptor.ts:17-25` está apagado y su aviso (`:60-68`) afirma que avisó a auditoría.
- **Corrección de raíz:**
  - Cada endpoint que sirve datos personales o financieros registra la lectura del lado del servidor (regla 90.2.7), a partir de la identidad del token.
  - Se retira el interceptor del cliente y su aviso.
- **CA:** dada una lectura de expediente desde el backoffice, entonces existe su fila en la bitácora sin que el cliente la informe.
- **DoD:** prueba de integración por servicio dueño, empezando por `identidad` y `aportes`.
- **Depende de:** F3.L5.M40.
- **Esfuerzo:** medio.

### F1.L6.M11 — Backoffice: clave de idempotencia por intento de negocio, en toda escritura
- **Hallazgos:** DEFECTO-001 (S3) y DINERO-003 (S2).
- **Causa de raíz:**
  - La idempotencia es opcional por pantalla: el interceptor de `idempotencia.interceptor.ts:58-81` solo la agrega con marca de contexto.
  - La identidad del intento la define el reloj (15 minutos, `:27-33`) y vive en un mapa global (`:35-36`), cuando debería atarse a la operación de negocio.
  - El texto global «si la repetís, no se duplica» (`errores.ts:104-108`) no es cierto en todos los casos.
- **Corrección de raíz:**
  - El interceptor agrega la clave a todo `POST/PUT/PATCH/DELETE` por omisión, con exclusión explícita.
  - La clave se deriva del identificador del intento de negocio (`ordenId`, `cierreId`), sin vencimiento por reloj.
  - Se conserva ante un resultado desconocido (5xx o timeout) y se libera ante un 4xx definitivo.
  - El mapa se limpia al cerrar sesión.
  - El texto solo aparece si la petición llevó clave.
- **CA:**
  - Dada una escritura sin marca, entonces lleva clave.
  - Un reintento tras 20 minutos de un 5xx usa la misma clave.
  - Otro operador no reutiliza la clave del anterior.
- **DoD:**
  - pruebas de unidad del interceptor con los tres resultados (2xx, 4xx, 5xx o timeout);
  - prueba de cierre de sesión;
  - E2E de un cobro reintentado contra el servidor real, con un solo efecto.
- **Depende de:** F1.L5.M08.
- **Esfuerzo:** medio.

### F1.L6.M12 — App: clave de idempotencia por intento, liberada ante el rechazo y conservada ante la duda
- **Hallazgos:** DEFECTO-002 (S2).
- **Causa de raíz:** la identidad del intento se ata a la cuenta y no al contenido (`apps/movil/lib/proveedores/idempotencia.dart`, `cu10/cu11/cu12`), y no se distingue el rechazo definitivo del resultado desconocido. Corregir el importe choca con la clave vieja, y el backoffice resuelve lo mismo de otra forma.
- **Corrección de raíz:**
  - La misma política que en F1.L6.M11, especificada en un ADR corto de cliente.
  - La clave se genera por intento (contenido normalizado y un identificador de intento), se libera ante 4xx y se conserva ante timeout o 5xx.
  - Se corrige el comentario de `cu10_recargar_saldo.dart:12-15`.
- **CA:** dado un rechazo 422 por importe, cuando la persona corrige y reenvía, entonces la operación se procesa. Un timeout reintentado no duplica.
- **DoD:**
  - prueba de unidad del ciclo rechazo, corrección y reenvío;
  - prueba de integración contra el servidor real en la máquina de Pablo.
- **Depende de:** F1.L6.M11 y F1.L5.M08.
- **Esfuerzo:** medio.

---

## F2 — Retiro de lo que contradice el flujo nuevo

### F2.L6.M13 — El shell del backoffice queda sin aprobaciones eliminables
- **Hallazgos:** APROB-001 (S2).
- **Causa de raíz:** ADR-054 y las decisiones nuevas cambiaron la regla, pero el shell no se actualizó: menú, aterrizaje, secciones y catálogo de errores. El «trabajo del día» de Operación lo decide una cadena fija que apunta a la admisión.
- **Corrección de raíz:**
  - Retirar del shell las rutas y secciones de admisiones y postulaciones (`app.routes.ts:95-102`, `menu.ts:85-88`, `secciones.ts:52-55`), y también «Retiros en revisión» (`menu.ts:76`, `secciones.ts:19,27`, P-09).
  - Retirar sus permisos de vista y los códigos `AP-CU68-15`, `AP-CU69-10`, `AP-CU20-02` y `AP-CU90-03..08` de los dos catálogos.
  - Retirar sus explicadores (`explicaciones-operacion.ts:38,86`).
  - El aterrizaje de Operación pasa a ser la bandeja de alertas: excepciones de KYC (P-08), alertas UIF de retiros (P-09) y descargos (P-12).
  - Este cambio va junto con F2.L3.M10, F2.L3.M14 y el retiro de la doble aprobación de retiros del lote L4.
- **CA:** dado un operador de Operación, cuando entra al backoffice, entonces no hay ninguna pantalla para aprobar ingresos, postulaciones ni retiros, y aterriza en la bandeja de alertas.
- **DoD:**
  - `yarn lint` y `yarn typecheck` del backoffice;
  - verificador de rutas en verde;
  - prueba visual del aterrizaje en tres viewports y dos temas (95.7).
- **Depende de:** F2.L3.M10, F2.L3.M14 y la microtarea de retiros automáticos del lote L4.
- **Esfuerzo:** medio.

### F2.L6.M14 — Mock, clientes y catálogo de operaciones regenerados sin respaldo ni aprobaciones retiradas
- **Hallazgos:** RESPALDO-002 (S2) y APROB-002 (S3).
- **Causa de raíz:** el mock y los clientes se generan del OpenAPI vigente, sin una lista de operaciones retiradas. Por eso siguen ofreciendo como normales:
  - `cubiertoEmpresa`, `pozo-completo` y toda la familia de respaldo;
  - los pasos de aprobación `PENDIENTE_BACKOFFICE`, `decidirSolicitudDeIngreso`, `requiereAprobacion`, `aprobarRetiro`, `aprobarReembolso` y `organizador/*`.
- **Corrección de raíz:**
  - En el mismo cambio que retira cada operación del contrato (lotes L2, L3 y L4), regenerar `packages/simulado/ejemplos`, los clientes y `operaciones.json`.
  - Agregar al verificador una lista de términos retirados (`cubiertoEmpresa`, `respaldo`, `PENDIENTE_BACKOFFICE`, `aprobarPostulacion`…) que falle si reaparecen.
  - `aprobarReembolso` se conserva porque P-13 mantiene los cuatro ojos reales, pero los ejemplos dejan de llevar el aprobador en el cuerpo.
- **CA:** dado `yarn dev:mock`, cuando se recorren los ejemplos, entonces ninguno contiene términos retirados.
- **DoD:**
  - verificador con la lista en verde;
  - prueba de mutación: reintroducir un término y ver el rojo.
- **Depende de:** los retiros de F2 de los lotes L2, L3 y L4.
- **Esfuerzo:** medio.

---

## F3 — Flujo nuevo

### F3.L6.M15 — El sitio público explica el fondo del grupo desde una sola fuente
- **Hallazgos:** RESPALDO-001 (S2).
- **Causa de raíz:** los textos de marketing se escribieron contra el modelo de garantía viejo y están duplicados en ocho puntos:
  - `cierre.ts:71`;
  - `producto.ts:75`;
  - `transparencia.ts:31,65`;
  - `hero.ts:31`;
  - `inicio.md:5`;
  - `como-funciona.md:20`;
  - `preguntas.md:20`;
  - el JSON-LD de `app.routes.ts:117`.

  Ningún verificador los vigila.
- **Corrección de raíz:**
  - Un solo texto de política en `apps/web/contenido/` que alimente la portada, las preguntas frecuentes y el JSON-LD. Explica el modelo A: el aporte chico al fondo del grupo en cuenta segregada, la cobertura de la cuota de quien no paga con su deuda al fondo, y la devolución al cierre con el reparto de lo incobrable. También la pieza D: la puntualidad tiene beneficios.
  - Nada de «la empresa cubre» ni de «pozo completo garantizado».
  - Lo redacta Claude y lo aprueba Pablo (P-16). La parte regulatoria la valida Cumplimiento (fondo segregado y captación).
  - `scripts/verificar_frontend.py` hace un barrido que falla ante «cubr…», «fondo de garantía» o «pozo completo» fuera de la fuente única.
- **CA:** dado el sitio y su JSON-LD, entonces el texto sobre el faltante es idéntico en todas las superficies y coincide con el modelo A.
- **DoD:**
  - verificador en verde con prueba de mutación;
  - validación del JSON-LD;
  - prueba visual de la portada en tres viewports;
  - aprobación de Pablo registrada en el PR.
- **Depende de:** el ADR del modelo A, P-16 y la validación de Cumplimiento.
- **Esfuerzo:** medio.

### F3.L6.M16 — El tour de bienvenida de la app explica el fondo del grupo
- **Hallazgos:** RESPALDO-003 (S2).
- **Causa de raíz:** es la misma de F3.L6.M15, en la app. Lo prometen `arte_fondo_de_garantia.dart:6-8`, `laminas_del_tour.dart:45` y `textos.dart:42-46`.
- **Corrección de raíz:**
  - Reescribir la lámina con el mismo texto aprobado en F3.L6.M15, tomado de un catálogo de textos de la app que se verifica contra la fuente web.
  - Revisar la ilustración para que no muestre un pozo completo garantizado.
- **CA:** dado el tour, entonces la lámina del fondo dice lo mismo que el sitio.
- **DoD:**
  - prueba de widget;
  - golden en claro y oscuro;
  - verificación de paridad del texto.
- **Depende de:** F3.L6.M15.
- **Esfuerzo:** chico.

### F3.L6.M17 — Los medios de pago visibles salen del contrato
- **Hallazgos:** FLUJO-QR-003 (S2).
- **Causa de raíz:** los textos de marketing no tienen una fuente única frente al catálogo de medios de pago. `producto.ts:67,71` ofrece efectivo y transferencia.
- **Corrección de raíz:**
  - `aportes` publica los medios habilitados: QR y saldo recargado por QR (P-06).
  - La portada los toma de una fuente de contenido que se verifica contra ese contrato.
  - Se retiran efectivo y transferencia.
- **CA:** dada la portada, entonces solo aparecen QR y billetera recargada por QR.
- **DoD:** verificador de paridad con el contrato y prueba visual.
- **Depende de:** F2.L5.M32.
- **Esfuerzo:** chico.

### F3.L6.M18 — Una sola fuente del estado regulatorio
- **Hallazgos:** COHERENCIA-001 (S2).
- **Causa de raíz:** los textos están duplicados entre `contenido/*.md`, la portada y el JSON-LD (`app.routes.ts:119`, `cierre.ts:72`, `producto.ts:74`). `producto.ts` afirma custodia en presente, aunque el dossier H15 la condiciona.
- **Corrección de raíz:**
  - `apps/web/contenido/legal/estado-regulatorio.md` alimenta la portada, las preguntas frecuentes y el JSON-LD.
  - El texto lo valida Cumplimiento.
  - El verificador falla si hay afirmaciones regulatorias fuera de esa fuente.
- **CA:** dado el sitio, entonces existe una sola afirmación sobre ASFI y custodia, validada.
- **DoD:** verificador y acuse de Cumplimiento en el PR.
- **Depende de:** Cumplimiento.
- **Esfuerzo:** chico.

### F3.L6.M19 — El recordatorio de aporte lleva a la pantalla de pago por QR
- **Hallazgos:** HUECO-001 (S3).
- **Causa de raíz:** el mapa de prefijos de `enlaces_profundos.dart:16` y `ruta_del_aviso.dart:46-48` está escrito a mano y no se prueba contra el árbol de rutas. `aportaya://aporte` lleva a `/billetera/aportes`, que no existe.
- **Corrección de raíz:**
  - El destino es la cuota del grupo con su QR.
  - El mapa se genera desde `rutas.dart`, o una prueba resuelve cada prefijo contra el router.
- **CA:** dado el enlace del recordatorio (F3.L5.M43), cuando se abre, entonces la persona ve la cuota con su QR.
- **DoD:** prueba de navegación por cada prefijo.
- **Depende de:** F3.L5.M43.
- **Esfuerzo:** chico.

---

## F4 — Calidad y deuda

Cada microtarea es un cambio y un PR. La DoD de las visuales incluye capturas en tres viewports y dos temas (95.7).

### F4.L6.M20 — Un único `Refrescador` por sesión
- **Hallazgos:** DEFECTO-003 (S2).
- **Causa de raíz:** el single-flight vive en la instancia (`refrescador.dart:5-17`), y hay tres instancias: `cliente.dart:90`, `renovar_acceso.dart:13-17` y `habilitacion_basica.dart:101`. Por eso dos refrescos con el mismo token pueden revocar la familia.
- **Corrección de raíz:**
  - Un `Provider` de Riverpod ligado a la sesión, que usan el interceptor y el vigilante.
  - El refresco propaga `x-request-id`.
  - `habilitacion_basica.dart` cae con F2.L3.M12.
- **CA:** dado un 401 simultáneo con la renovación del vigilante, entonces sale un solo `POST /sesion/refrescar`.
- **DoD:** prueba de unidad concurrente con un doble HTTP que cuenta las llamadas.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M21 — La protección de captura lleva contador
- **Hallazgos:** DEFECTO-004 (S3).
- **Causa de raíz:** `FLAG_SECURE` es un estado global y se maneja como si fuera local (`proteccion_de_pantalla_dinero.dart:11-27`).
- **Corrección de raíz:** el puerto lleva la cuenta: `activar` suma y `desactivar` resta, y solo apaga al llegar a 0.
- **CA:** dadas dos pantallas protegidas apiladas, al cerrar la de arriba la de abajo sigue protegida.
- **DoD:** prueba de unidad del puerto y prueba en dispositivo en la máquina de Pablo.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M22 — El release de Android se firma con la clave de producción y exige el host
- **Hallazgos:** CONFIG-001 (S3).
- **Causa de raíz:** la plantilla de Flutter quedó sin completar: `build.gradle.kts:36-40` tiene un `TODO`, y App Links apunta a TEST por omisión.
- **Corrección de raíz:**
  - `signingConfigs.release` lee los secretos de CI.
  - El build de release falla sin la firma y sin `hostDeLaAppWeb` explícito.
- **CA:** dado `flutter build apk --release` sin parámetros, entonces falla.
- **DoD:** build con y sin parámetros, más `apksigner verify`.
- **Depende de:** los secretos de CI que provee Pablo.
- **Esfuerzo:** chico.

### F4.L6.M23 — El vigilante de verificación solo absorbe errores de red
- **Hallazgos:** PARCHE-001 (S4).
- **Causa de raíz:** el `catch` de `vigilante_de_aprobacion.dart:84-86` no distingue errores reintentables de errores de programación.
- **Corrección de raíz:**
  - Capturar solo `DioException` de red y `ErrorDeApi` reintentable, y reportar el resto con el registrador seguro.
  - Con P-08, el vigilante pasa a esperar el resultado del proveedor de KYC.
- **CA:** dado un error de tipos, entonces se registra y se muestra el estado de error.
- **DoD:** prueba de unidad en tres niveles: red, 4xx y error de programación.
- **Depende de:** la microtarea de KYC con proveedor del lote L4.
- **Esfuerzo:** chico.

### F4.L6.M24 — Cada ruta declara si exige identidad verificada
- **Hallazgos:** UX-A11Y-001 (S3).
- **Causa de raíz:** `acceso_a_rutas.dart:17-22` mantiene una lista de expresiones regulares paralela al árbol de rutas, y no incluye `rutas_operacion.dart:7,15,25,44`.
- **Corrección de raíz:** marcar en la definición de cada ruta si exige identidad, y que la guardia lea esa marca.
- **CA:** dada una ruta de dinero, una persona sin verificar es llevada a verificar antes de llenar el formulario.
- **DoD:** prueba que recorre todas las rutas marcadas.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M25 — `CelebracionDeExito` usa un rol de color «sobre marca»
- **Hallazgos:** UX-A11Y-002 (S2).
- **Causa de raíz:** la tilde se dibuja con un rol de texto sobre fondo neutro, pero queda sobre el color de marca: `brand` y `brandTexto` tienen el mismo valor (`tokens.json:234,237,289,292`) y el contraste es 1:1.
- **Corrección de raíz:** dibujar la tilde con `brandInk` o con un rol dedicado en `packages/tokens`.
- **CA:** el contraste de la tilde con el círculo es ≥ 3:1 (WCAG 1.4.11) en los dos temas.
- **DoD:** golden en claro y en oscuro, más la prueba de contraste de F4.L6.M37.
- **Depende de:** F4.L6.M37.
- **Esfuerzo:** chico.

### F4.L6.M26 — Las piezas del sistema de diseño no muestran datos inventados
- **Hallazgos:** HUECO-002 (S3).
- **Causa de raíz:** hay piezas portadas de otro producto que traen datos de demostración como valores por omisión:
  - `grilla_metricas_miembro.dart:21` muestra «Bs 24.800»;
  - `tarjeta_riesgo_confianza.dart` dice «Nivel Excelente» y «asientos contables reales».
- **Corrección de raíz:**
  - Parámetros obligatorios sin valor por omisión.
  - Los datos de demo pasan solo a `catalogo/`.
  - La tarjeta de nivel muestra el nivel de puntualidad de la pieza D, que viene del servidor (F3.L5.M39), sin afirmar nada que no provenga del mayor.
  - Lo que no tenga uso se retira.
- **CA:** dado el componente sin parámetros, entonces no compila.
- **DoD:** build del paquete y del catálogo, más un golden.
- **Depende de:** F3.L5.M39.
- **Esfuerzo:** chico.

### F4.L6.M27 — Un parser de importes y un conversor a `America/La_Paz` en el sistema de diseño
- **Hallazgos:** DEFECTO-005 (S3).
- **Causa de raíz:** el paquete no tiene un objeto de valor `Dinero` ni una `FechaLaPaz` únicos. Hay cuatro tratamientos de zona y tres parsers de centavos, y `calendario_de_cuotas.dart:56-67` suma la deuda en el cliente.
- **Corrección de raíz:**
  - Usar `Monto` (F1.L6.M03) y un conversor único con vectores compartidos.
  - El total exigible lo informa el servidor.
- **CA:** dado un movimiento a las 22:00 de La Paz, entonces se agrupa en el día local.
- **DoD:** pruebas con vectores compartidos.
- **Depende de:** F1.L6.M03.
- **Esfuerzo:** medio.

### F4.L6.M28 — La sesión del backoffice siempre conoce al `sujeto`
- **Hallazgos:** DEFECTO-006 (S3).
- **Causa de raíz:** hay dos caminos para abrir sesión (`abrir` y `abrirConToken`) y solo uno lee los claims. Por eso los tutoriales se guardan con la clave `anonimo`.
- **Corrección de raíz:** un único método que derive `sujeto` del token en `auth-bootstrap.ts:35` y en `sesion.interceptor.ts:27`.
- **CA:** después de un F5 o de un refresco, `sujeto` no es nulo y los tutoriales se separan por operador.
- **DoD:** pruebas de unidad de los dos caminos.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M29 — El verificador de cadena del cliente coincide con el del servidor, o se retira
- **Hallazgos:** DUP-X-001 (S3).
- **Causa de raíz:** `dominio-cliente/src/cadena.ts:17-28` se escribió antes que `CadenaDeBloques.java:71-75` y nunca se reconcilió con él.
- **Corrección de raíz:** vectores generados desde la clase Java, como `sorteo.vectores.json`, más alinear el algoritmo. Si la web no lo usa, se retira.
- **CA:** dados los vectores del servidor, el cliente verifica las mismas cadenas.
- **DoD:** prueba con los vectores.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M30 — Un paquete de red Angular compartido por el backoffice y la web
- **Hallazgos:** DUP-X-002 (S3).
- **Causa de raíz:** no existe un paquete de infraestructura de red. `gateway`, `errores` y los interceptores están clonados entre las dos apps.
- **Corrección de raíz:** crear `packages/red-angular` parametrizado y que las dos apps lo consuman. Incluye F1.L6.M07 y F1.L6.M11.
- **CA:** existe una sola implementación de cada interceptor.
- **DoD:** `grep`, más tests, lint y typecheck de las dos apps.
- **Depende de:** F1.L6.M07 y F1.L6.M11.
- **Esfuerzo:** medio.

### F4.L6.M31 — Una sola configuración de idioma para la app y su prueba
- **Hallazgos:** DUP-INT-001 (S4).
- **Causa de raíz:** `localizacion.dart` y `app_real.dart` son clones que divergieron.
- **Corrección de raíz:** una constante única que usan `app_real.dart` y `idioma_test.dart`.
- **CA:** si cambia la configuración de la app, la prueba lo detecta.
- **DoD:** `flutter test test/widget/idioma_test.dart`.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M32 — El progreso de tutoriales se borra al cerrar sesión
- **Hallazgos:** DOC-001 (S4).
- **Causa de raíz:** el comentario de `almacen_de_progreso_seguro.dart:14-16` describe una intención que nunca se implementó.
- **Corrección de raíz:** borrar las claves de progreso en `sesion.dart:143-147`.
- **CA:** al cerrar sesión, no quedan claves de progreso.
- **DoD:** prueba de unidad.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M33 — El shell móvil no tiene código muerto que parezca un control
- **Hallazgos:** REESCRITURA-001 (S3).
- **Causa de raíz:** las reescrituras dejaron versiones viejas: `guardias.dart` con rutas que no existen, `hayConexionProvider`, `validacion.dart` (`montoValido` con `double.parse`) y `dispositivoIdProvider`.
- **Corrección de raíz:** borrar lo que no tiene uso. Si el bloqueo sin conexión es requisito (regla 95.2.4), implementarlo con su prueba.
- **CA:** `grep` de cada símbolo vacío, o el símbolo usado con su prueba.
- **DoD:** `flutter analyze` y tests.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M34 — El shell del backoffice y `packages/ui` quedan sin código sin consumidor
- **Hallazgos:** REESCRITURA-002 (S3).
- **Causa de raíz:** las reescrituras no retiraron las versiones previas. Quedan unas 560 líneas de explicadores, entre ellas RE-07 «el fondo… cubrir cualquier falta» y un endpoint inventado, y unos 22 componentes de `ui` sin uso.
- **Corrección de raíz:** borrar lo que no tiene consumidor real o moverlo al catálogo. Los explicadores que queden en uso se reescriben con F3.L6.M15.
- **CA:** el inventario de componentes sin consumidor queda vacío o justificado.
- **DoD:** script de inventario, más lint, typecheck y build.
- **Depende de:** F2.L6.M13.
- **Esfuerzo:** medio.

### F4.L6.M35 — Las rutas que el frontend usa tienen contrato
- **Hallazgos:** CONTRATO-001 (S3).
- **Causa de raíz:** el frontend consume rutas que el servidor no publica: ADR-045 en `identidad` y el servicio `extraccion`, que no existe. La lista `CONOCIDAS_SIN_CONTRATO` se mantiene a mano.
- **Corrección de raíz:**
  - Las rutas de exportación y registro de acceso caen con F1.L6.M09 y F1.L6.M10.
  - Las de tutoriales remotos se publican en el contrato del servicio dueño o se retiran.
  - Se elimina la lista de excepciones.
  - `GET /grupos/solicitudes-escaladas` cae con F2.L3.M10.
- **CA:** `scripts/verificar_rutas_frontend.py` queda en verde sin excepciones.
- **DoD:** salida del verificador.
- **Depende de:** F1.L6.M09, F1.L6.M10 y F2.L3.M10.
- **Esfuerzo:** chico.

### F4.L6.M36 — `test:front` corre todas las carpetas de prueba
- **Hallazgos:** PRUEBAS-001 (S3).
- **Causa de raíz:** la lista de carpetas está escrita a mano en `apps/movil/package.json:12`.
- **Corrección de raíz:** `flutter test test --exclude-tags goldens,a11y`, más un verificador que falla si alguna carpeta de pruebas queda fuera de un script.
- **CA:** `test/billetera` y `test/navegacion` corren en CI.
- **DoD:** salida de la corrida con los conteos por carpeta.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M37 — Las pruebas de contraste WCAG de los tokens corren
- **Hallazgos:** PRUEBAS-002 (S3).
- **Causa de raíz:** se mezclan dos convenciones de nombres. `vitest.config.ts:2` incluye solo `*.spec.ts`.
- **Corrección de raíz:** incluir `pruebas/**/*.test.mjs` o renombrar esos archivos.
- **CA:** al bajar el contraste de un rol por debajo de AA, la prueba falla.
- **DoD:** `yarn test` en `packages/tokens`, con una prueba de mutación.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M38 — Playwright del backoffice sin reintentos
- **Hallazgos:** PRUEBAS-003 (S3).
- **Causa de raíz:** nunca se hizo triaje de las pruebas inestables. Por eso queda `retries: 1` (`playwright.config.ts:30`), contra la regla 80.5.3.
- **Corrección de raíz:** `retries: 0` en todos los entornos. Las pruebas inestables quedan en cuarentena con dueño y fecha.
- **CA:** la configuración no tiene reintentos y la lista de cuarentena tiene dueño y fecha.
- **DoD:** corrida E2E del backoffice, de a un navegador por vez.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L6.M39 — El mock tiene datos con semántica de dominio y monedas ISO-4217
- **Hallazgos:** DATOS-001 (S3).
- **Causa de raíz:** el mock se genera desde el esquema, sin semántica de dominio (`generar-ejemplos.mjs:107-109`). Aparece `"4.44"` en 117 archivos, `"MMM"` como moneda, y los escenarios «adverso» son copias de «ok». Además, `Dinero.moneda` no tiene `pattern`.
- **Corrección de raíz:**
  - Agregar `pattern: '^[A-Z]{3}$'` a `moneda` en todos los contratos.
  - Ejemplos coherentes: importes distintos con `pozo = confirmado + faltante`.
  - Escenarios adversos reales.
  - Los datos son sintéticos y declarados como tales.
- **CA:** ningún ejemplo tiene una moneda inválida y todos los escenarios adversos difieren de los ok.
- **DoD:** validación por script de todos los ejemplos contra el contrato y una prueba de coherencia.
- **Depende de:** F2.L6.M14.
- **Esfuerzo:** medio.

### F4.L6.M40 — Re-verificar los candidatos del §5-bis
- **Hallazgos:** no son fichas. Son los 40 candidatos de la sección siguiente.
- **Causa de raíz:** los lectores delegados los encontraron, pero el auditor no volvió a abrir cada ubicación.
- **Corrección de raíz:**
  - Por cada candidato: abrir la ubicación en `origin/test` y clasificarlo como confirmado o descartado, con evidencia.
  - Los confirmados se suman a la microtarea indicada en la tabla o generan una nueva, con su CA y DoD.
  - Los descartados se registran con su motivo.
- **CA:** cada candidato tiene un veredicto con evidencia.
- **DoD:** una tabla de veredictos en `docs/trabajo/<fecha>/evidencia/`.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

---

## Candidatos del §5-bis — a re-verificar en F4 (no son hallazgos confirmados)

| Origen | Clase · sev. propuesta | Resumen | Si se confirma, va a |
|---|---|---|---|
| A-H02 | DINERO · S3 | `CampoMonto(valorInicial:)` re-parsea la cadena del contrato | F1.L6.M03 |
| A-H04 | APROB · S3 | Piezas de aprobación humana (`TarjetaDeSolicitud`, `PanelDeFactores`, `EstadoDeTramite`, `HAB_*`) | F2.L6.M13 (retiro) |
| A-H09, A-H12 | DINERO · S3 | Modelos con importe sin moneda; «Bs» fijo; prefijo de moneda en tres lugares | F4.L6.M27 |
| A-H11 | UX-A11Y · S3 | `mostrarSnackbar` siempre verde, también para errores | nueva en F4 |
| A-H13 | DINERO/RN-18 · S3 | «Comisión de entrega» y «bono de bienvenida» sin decir quién cobra ni quién paga | F3.L5.M36 (comisión fija) |
| A-H14 | FLUJO-QR · S3 | `recargaTransferencia`, `canalBanco`, `canalTarjeta` | F2.L5.M32 + F3.L6.M17 |
| A-H15, A-H16 | UX-A11Y · S3 | `TarjetaDeOferta` con códigos crudos; `TarjetaDeSolicitud` con doble envío | nueva en F4 (o F2.L6.M13 si se retira) |
| A-H17, A-H18 | UX-A11Y · S3 | Literales `Paleta.*` que saltan el tema oscuro; duraciones > 320 ms | nueva en F4 |
| A-H19..H26 | CLEAN/PRUEBAS · S3-S4 | Clones, comentarios falsos, mojibake, etiqueta no vinculada, animaciones en `build`, goldens de plataforma | nueva en F4 |
| A-H10 | REESCRITURA · S3 | Unas 35 piezas públicas sin referencias; `ListaDeMovimientos` divergente | F4.L6.M33/M34 |
| B-06 | DUP-X · S3 | Sorteo TS frente a Java con semilla o entropía vacías; vectores en Python | F1.L5.M20 |
| B-12, B-13, B-14 | PRUEBAS/DATOS · S3 | `intermitente.luego` sin implementar; rechazos con códigos ajenos; rellenos con forma de dato | F4.L6.M39 |
| B-17, B-18 | DUP-INT/DINERO · S3-S4 | Exclusión de tokens distinta entre CSS y Dart; bordes del formateo de montos | F4.L6.M27 |
| B-19, B-20 | CONFIG · S3 | Frontera del mock hacia producción; `/recargas-de-prueba` en el contrato productivo | F1.L6.M05 |
| C-H09 | DINERO · S3 | `lista-de-movimientos` (ui) agrupa por día UTC y suma sin verificar la moneda | F4.L6.M27 (equivalente Angular) |
| C-H10 | DOC · S3 | Notas internas de carril publicadas en el sitio; `.md` generado versionado | nueva en F4 |
| C-H12 | FRAGIL · S3 | Guardias y menú copian a mano la cadena de permisos | F4.L6.M24 (equivalente backoffice) |
| C-H13 | UX-A11Y · S3 | `.boton--claro` con contraste 3,32:1; hex literal; tutorial sobre `display: contents` | F4.L6.M37 |
| C-H14, C-H16, C-H17, C-H19, C-H20 | CONFIG/FRAGIL/CLEAN/RESILIENCIA · S3-S4 | Despliegue contradictorio, SEO duplicado, parches, arranque del SSR | F1.L6.M08 / nueva en F4 |
| C-H18 | SEG · S3 | `cuenta-enmascarada` recibe el número completo; `fila-de-cotejo` decide KYC en la vista | nueva en F1 si se confirma |
| D-13, D-19 | CLEAN · S4 | `!` sobre id de usuario; `r.data!`; `cu13_cuenta_propia` | nueva en F4 |
| D-17, D-18 | HUECO/CONFIG · S3-S4 | Push Android que no entrega; puente de cámara de desarrollo sin guarda de release | F3.L5.M43 / F1.L6.M05 |
| D-20 | DINERO · S3 | `BOB` fijo en 4 lugares; `montoValido` con `double.parse` | F4.L6.M33 |
| D-21, D-22 | UX-A11Y · S3 | Tres nombres de producto; hidratación solo en tema claro; textos de permisos iOS | F4.L3.M27(e) / nueva en F4 |
| D-23..D-26 | CLEAN/OBSERV · S4 | Drivers que escriben en `docs/trabajo`; comentarios desactualizados; `capacidadesProvider` duplicado; errores tragados | nueva en F4 |

Son 25 filas que agrupan los 40 candidatos del informe. Ninguno se cuenta en el total de 42.

---

## Trazabilidad hallazgo → microtarea (42 de 42)

| # | Hallazgo | Sev. | Microtarea | Tipo |
|---:|---|---|---|---|
| 1 | frontends-compartidos-RESPALDO-001 | S2 | F3.L6.M15 | corrección |
| 2 | frontends-compartidos-COHERENCIA-001 | S2 | F3.L6.M18 | corrección |
| 3 | frontends-compartidos-APROB-001 | S2 | F2.L6.M13 | corrección |
| 4 | frontends-compartidos-TRAZA-001 | S2 | F1.L6.M10 | corrección |
| 5 | frontends-compartidos-CONTRATO-001 | S3 | F4.L6.M35 | corrección |
| 6 | frontends-compartidos-CONTRATO-002 | S3 | F0.L6.M02 | corrección |
| 7 | frontends-compartidos-PARCHE-001 | S4 | F4.L6.M23 | corrección |
| 8 | frontends-compartidos-DEFECTO-001 | S3 | F1.L6.M11 | corrección |
| 9 | frontends-compartidos-FLUJO-QR-001 | S3 | F1.L6.M05 | corrección |
| 10 | frontends-compartidos-DEFECTO-002 | S2 | F1.L6.M12 | corrección |
| 11 | frontends-compartidos-DEFECTO-003 | S2 | F4.L6.M20 | corrección |
| 12 | frontends-compartidos-PRUEBAS-001 | S3 | F4.L6.M36 | corrección |
| 13 | frontends-compartidos-CONFIG-001 | S3 | F4.L6.M22 | corrección |
| 14 | frontends-compartidos-HUECO-001 | S3 | F3.L6.M19 | corrección |
| 15 | frontends-compartidos-UX-A11Y-001 | S3 | F4.L6.M24 | corrección |
| 16 | frontends-compartidos-DEFECTO-004 | S3 | F4.L6.M21 | corrección |
| 17 | frontends-compartidos-FLUJO-QR-002 | S3 | — | sin acción (P-06) |
| 18 | frontends-compartidos-FRAGIL-001 | S3 | F0.L6.M01 | corrección |
| 19 | frontends-compartidos-REESCRITURA-001 | S3 | F4.L6.M33 | corrección |
| 20 | frontends-compartidos-DUP-INT-001 | S4 | F4.L6.M31 | corrección |
| 21 | frontends-compartidos-DOC-001 | S4 | F4.L6.M32 | corrección |
| 22 | frontends-compartidos-RESPALDO-002 | S2 | F2.L6.M14 | corrección |
| 23 | frontends-compartidos-APROB-002 | S3 | F2.L6.M14 | corrección |
| 24 | frontends-compartidos-SEG-001 | S3 | F1.L6.M07 | corrección |
| 25 | frontends-compartidos-CONTRATO-003 | S2 | F1.L6.M04 | corrección |
| 26 | frontends-compartidos-DINERO-001 | S3 | F1.L6.M06 | corrección |
| 27 | frontends-compartidos-DUP-X-001 | S3 | F4.L6.M29 | corrección |
| 28 | frontends-compartidos-DATOS-001 | S3 | F4.L6.M39 | corrección |
| 29 | frontends-compartidos-PRUEBAS-002 | S3 | F4.L6.M37 | corrección |
| 30 | frontends-compartidos-DINERO-002 | S1 | F1.L6.M03 | corrección |
| 31 | frontends-compartidos-RESPALDO-003 | S2 | F3.L6.M16 | corrección |
| 32 | frontends-compartidos-UX-A11Y-002 | S2 | F4.L6.M25 | corrección |
| 33 | frontends-compartidos-HUECO-002 | S3 | F4.L6.M26 | corrección |
| 34 | frontends-compartidos-DEFECTO-005 | S3 | F4.L6.M27 | corrección |
| 35 | frontends-compartidos-FLUJO-QR-003 | S2 | F3.L6.M17 | corrección |
| 36 | frontends-compartidos-SEG-002 | S2 | F1.L6.M08 | corrección |
| 37 | frontends-compartidos-SEG-003 | S2 | F1.L6.M09 | corrección |
| 38 | frontends-compartidos-DEFECTO-006 | S3 | F4.L6.M28 | corrección |
| 39 | frontends-compartidos-DINERO-003 | S2 | F1.L6.M11 | corrección |
| 40 | frontends-compartidos-DUP-X-002 | S3 | F4.L6.M30 | corrección |
| 41 | frontends-compartidos-PRUEBAS-003 | S3 | F4.L6.M38 | corrección |
| 42 | frontends-compartidos-REESCRITURA-002 | S3 | F4.L6.M34 | corrección |
