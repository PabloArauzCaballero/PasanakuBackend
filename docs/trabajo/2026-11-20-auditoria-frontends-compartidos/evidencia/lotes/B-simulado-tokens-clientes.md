# Lote B — `packages/simulado`, `packages/tokens`, `packages/dominio-cliente`, `clientes/`

Auditoría de solo lectura sobre `origin/test @07fc697`. Los 433 archivos del lote salen de `git ls-files packages/simulado packages/tokens packages/dominio-cliente clientes`.
**Leídos: 433/433.** De esos, 84 se leyeron completos a mano y el resto se cubrió con lectura dirigida o con verificación programática: 349 JSON de ejemplos (los 84 de `aportes`, `nucleo-financiero`, `entregas` y `garantia` con los escenarios ok/adverso/rechazo leídos uno por uno; los otros 265 por muestreo de 3 por servicio más chequeos programáticos sobre todos los escenarios) y `vectores/monto.json` (366 KB, verificado por script).

Herramientas de verificación, todas fuera del repo: `scratchpad/b-tools/deriva.py` valida los 349 ejemplos contra el OpenAPI **fuente** (`servicios/<svc>/src/main/resources/openapi/<svc>.yaml`) con jsonschema Draft 2020-12, y su salida quedó en `scratchpad/b-tools/deriva.out`. También usé scripts python inline para buscar importes, monedas, PII y estados, y `node --experimental-strip-types` para ejecutar `validarConfiguracion` con dos entradas (es lectura, no build).

## 2) Hallazgos candidatos (ordenados por severidad propuesta)

### B-01 · El mock y el cliente Dart publican el fondeo del pozo con dinero de la empresa (`cubiertoEmpresa`, `/entregas/pozo-completo`)
- **Clase:** RESPALDO · **Severidad:** S1. Contradice R-NUEVA-2 en un contrato en uso.
- **Ubicaciones:**
  - `packages/simulado/ejemplos/entregas/fondearPozo.json:5` → `"ruta": "/entregas/pozo-completo"`
  - `packages/simulado/ejemplos/entregas/fondearPozo.json:21,25,50,54,79,83,108,112`: `"cubiertoMutual"` y `"cubiertoEmpresa": { "monto": "4.44", "moneda": "BOB" }`. Los 4 escenarios dicen `"estado": "FONDEADO"`.
  - `clientes/dart/entregas/lib/aportaya_cliente_entregas.dart:20,38` → exporta `entrada_fondeo_del_pozo.dart` y `salida_fondeo_del_pozo.dart`.
  - Origen, fuera del lote: `servicios/entregas/src/main/resources/openapi/entregas.yaml:324-326` (`operationId: fondearPozo # CU-22 pozo completo`) y `:792,800` (`required: [... cubiertoMutual, cubiertoEmpresa, ...]`).
- **Cómo lo comprobé:** grep de `cubiertoEmpresa|pozo-completo` y lectura del ejemplo y del fragmento del contrato.
- **Impacto:** las pantallas y pruebas que consumen el mock tratan como normal que la empresa complete el pozo de quien no pagó. El cliente Dart hace la operación invocable desde la app.
- **Causa de fondo:** el contrato CU-22 «pozo completo» todavía modela el respaldo empresarial. El mock solo lo refleja.

### B-02 · Familia «respaldo/cobertura/faltante» completa en mock y clientes (garantía y aportes)
- **Clase:** RESPALDO · **Severidad:** S1 en `cubrirConRespaldo` y `cubrirIncumplimiento`; S3 en el resto, que es solo nomenclatura.
- **Ubicaciones (en todas, la ruta está en la línea 5):**
  - `packages/simulado/ejemplos/garantia/cubrirConRespaldo.json` (`/garantia/respaldo/coberturas`; `"resultado": "APLICADA"`, `cubierto`, `sinCubrir`, `exposicionDespues`)
  - `.../garantia/reservarRespaldo.json`, `ampliarRespaldo.json`, `liberarRespaldo.json`, `recuperarRespaldo.json`, `reversarCoberturaRespaldo.json` (`/garantia/respaldo/...`)
  - `.../garantia/cubrirIncumplimiento.json:5,17,31,45,59` (`/incumplimientos/{expedienteId}/cobertura`, `"montoCubierto"`, `"requiereAprobacion": true`)
  - `.../garantia/devolverFondo.json` (`consumidoPorCoberturas`)
  - `.../aportes/consultarRecaudoDelPeriodo.json:21,25,54,58,79,83` (`"cubiertoMutual"`, `"faltante"`)
  - `.../nucleo-financiero/conciliarCustodia.json` y `listarConciliaciones.json` (`ratioCobertura`; es encaje de custodia, no respaldo a morosos: se lista para descartarlo)
  - `clientes/dart/garantia/lib/aportaya_cliente_garantia.dart:13` (`api/respaldo_api.dart`) y `:18,19,20,25,27,33,37,39,40,46,47,49,52` (`*_respaldo.dart`, `*_cobertura*.dart`, `linea_de_cobertura.dart`)
- **Cómo lo comprobé:** `grep -liE 'respaldo|cobertura|cubrir|cubierto|faltante|pozo-completo'` sobre `ejemplos/*/*.json` (18 archivos) y grep sobre `clientes/`.
- **Impacto:** el «doble» oficial de las pruebas de Angular y Flutter (`ejemploDe`, `test/comun.dart`) mantiene vivo el circuito de respaldo. Cualquier pantalla probada contra el mock queda verde aunque el flujo esté prohibido.
- **Causa:** el mock se deriva del contrato (`generar-ejemplos.mjs`) y conserva los ejemplos editados a mano. Mientras el contrato no se depure, el mock tampoco.

### B-03 · Aprobaciones humanas en el circuito, servidas por el mock y exportadas por los clientes
- **Clase:** APROB · **Severidad:** S2 dentro del circuito del pasanaku; S3 en backoffice y organizador.
- **Ubicaciones:**
  - `packages/simulado/ejemplos/grupos/canjearInvitacion.json:11,18,25,32` → `"estado": "PENDIENTE_BACKOFFICE"` (contrato: `servicios/grupos/.../grupos.yaml:1638`, `enum: [PENDIENTE_BACKOFFICE]`)
  - `.../grupos/decidirSolicitudDeIngreso.json:5,11` → `/grupos/solicitudes/{solicitudId}/decision`, `"estado": "APROBADA"`
  - `.../grupos/historialAdmision.json` → `"fase": "PROPUESTA", "decision": "ACEPTAR"` (ingreso por decisión humana) y `.../grupos/postularAlGrupo.json:5` (`/grupos/{grupoId}/postulaciones`)
  - `.../entregas/autorizarEntrega.json:5` → `/entregas/{entregaId}/autorizacion`, `"estado": "AUTORIZADA", "autorizadaPor"`
  - `.../aportes/aprobarReembolso.json:5` → `/pagos/reembolsos/{reembolsoId}/aprobacion`
  - `.../nucleo-financiero/aprobarRetiro.json:5` (`"aprobadaPor"`) y `listarRetirosEnRevision.json` (`"puedeDecidir": true`)
  - `.../garantia/aprobarReemplazo.json:5` (`/garantia/reemplazos/{id}/aprobacion`) y `cubrirIncumplimiento.json:17` (`"requiereAprobacion": true`)
  - Organizador: `.../organizador/postularOrganizador.json`, `aprobarPostulacion.json`, `habilitarOrganizador.json`, `revisarHabilitacion.json`, `resolverHabilitacion.json`, `listarBandejaDeHabilitaciones.json`, `aprobarTarea.json`
  - Fuera del circuito, solo para que conste: `publicidad/aprobarCampana.json`, `erp/aprobarOrdenDeCompra.json`, `erp/aprobarPresupuesto.json`, `cumplimiento/aprobarYEnviarReporte.json`, `notificaciones/listarPlantillas.json` (`estadoAprobacion`)
  - Clientes: `clientes/dart/nucleo-financiero/lib/aportaya_cliente_nucleofinanciero.dart:21,50` (`entrada_aprobacion_retiro`, `salida_aprobacion_retiro`); `clientes/dart/entregas/lib/aportaya_cliente_entregas.dart:34` (`salida_autorizacion`); `clientes/dart/garantia/lib/aportaya_cliente_garantia.dart:15` (`aprobar_reemplazo200_response`); `clientes/dart/organizador/lib/aportaya_cliente_organizador.dart:14,17,22,23,27,28,31,34-36,40,47-49`; `clientes/dart/grupos/lib/aportaya_cliente_grupos.dart:42,71` (`entrada_postulacion`, `salida_postulacion`); `clientes/angular/publicidad/model/entradaRevision.ts:20` (`Aprobada = 'APROBADA'`)
  - Estado `PENDIENTE_VERIFICACION` (KYC, no es aprobación del circuito): `identidad/registrarUsuario.json:11` y `identidad/consultarMiPerfil.json:33`
- **Cómo lo comprobé:** `grep -liE 'aprob|APROBAD|PENDIENTE_|comprobante|postulaci|habilitar|requiereAprobacion|autoriza'` (40 archivos) y lectura de los citados.
- **Impacto:** con R-NUEVA-1, ingreso, entrega, reembolso y retiro no deberían depender de una persona. El mock y los clientes los presentan como pasos normales.
- **Causa:** contratos previos a la regla nueva.

### B-04 · `validarConfiguracion` acepta `"/\host-ajeno/api/v1"` como ruta relativa, y el navegador la resuelve a otro host
- **Clase:** SEG · **Severidad:** S2. Anula el control D-A6 «host-ajeno» del gateway que lleva tráfico financiero.
- **Ubicación:** `packages/dominio-cliente/src/configuracion.ts:82-89`:
  `if (cruda.startsWith('//')) return { valida: false, motivo: 'esquema-relativo-a-protocolo' }` … `if (cruda.startsWith('/')) { … return { valida: true, url: ruta } }`
- **Cómo lo comprobé, ejecutando:**
  - `validarConfiguracion('/\\evil.example.com/api/v1', {modo:'produccion', origenPropio:'https://backoffice.aportaya.bo'})` devuelve `{"valida":true,"url":"/\\evil.example.com/api/v1"}`.
  - `new URL('/\\evil.example.com/api/v1','https://backoffice.aportaya.bo').href` devuelve `https://evil.example.com/api/v1`, porque WHATWG trata `\` como `/` en esquemas especiales.
- **Consumidores:** `apps/web/src/app/nucleo/gateway.ts:2,43` y `apps/backoffice/src/app/nucleo/gateway.ts:2,33`. La URL sale de una variable de entorno o de una etiqueta del HTML, así que el atacante necesita controlar esa configuración. Aun así, el control existe justamente para eso.
- **Defecto menor en el mismo archivo:** `configuracion.ts:51` incluye `'::1'` en `LOOPBACK`, pero `URL.hostname` devuelve `[::1]`, así que ese caso nunca coincide. Lo comprobé: `http://[::1]:4010/api/v1` en desarrollo devuelve `host-ajeno`.
- **Comentario que miente:** `configuracion.ts:24` dice «Los tres modos posibles» y el tipo tiene dos. `:17` habla de «Prism» y el servidor real es `@scalar/mock-server`.
- **Clon divergente:** `apps/movil/lib/dominio/configuracion.dart` (`validarGateway`, con `10.0.2.2` agregado) reimplementa la regla. No revisé si tiene el mismo hueco.

### B-05 · `cadena.ts` dice que no existe la implementación del backend, pero existe y calcula otra fórmula
- **Clase:** DUP-X (clon divergente) + DOC + REESCRITURA · **Severidad:** S2. Hoy es código sin consumidores: si se activa, toda cadena real daría «no íntegra».
- **Ubicaciones:**
  - `packages/dominio-cliente/src/cadena.ts:7-25`: «no existe en `servicios/transparencia` una clase Java `serializarCanonico`/`hashDeBloque`»
  - `cadena.ts:47-48`: `sha256Hex(`${numeroBloque}|${hashAnterior ?? ''}|${hashContenido}`)`
  - Backend: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CadenaDeBloques.java:74` → `sha256("%d|%s|%s|%s|%s".formatted(numero, hashAnterior, raizMerkle, desde, hasta))`. `:25` define `GENESIS = "0".repeat(64)` (el cliente usa `''`) y `:40` define `PRIMER_NUMERO = 2`. Además existe `servicios/transparencia/.../ContenidoCanonico.java` con su test.
  - `packages/dominio-cliente/vectores/README.md:23-31` repite el supuesto, que ya caducó.
- **Referencias de uso:** `grep recorrerCadena|hashDeBloque|serializarCanonico` en `apps/` y `packages/` solo encuentra comentarios (`apps/web/src/app/verificadores/verificador-de-cadena.ts:12,37` y `apps/web/pruebas/e2e/web-verificacion.e2e.ts:15`). No hay ninguna importación real.
- **Pruebas:** `cadena.spec.ts` solo comprueba que la función coincide consigo misma.

### B-06 · La regla del sorteo duplicada en TypeScript diverge de Java en los bordes
- **Clase:** DUP-X · **Severidad:** S3.
- **Ubicaciones:**
  - `packages/dominio-cliente/src/sorteo.ts:20` `if (!semilla) throw`, `:23` `if (!entropia) throw new Error('una entropía vacía no se puede comprometer')` y `:34` `if (!semilla || !hashComprometido) return false`
  - En Java (`plataforma/comun-dominio/.../SorteoVerificable.java:56,68,71`) solo se exige `!= null`: semilla `""` y entropía `""` son válidas.
- **Impacto:** un sorteo real con una entropía vacía hace que el verificador público lance un error en vez de verificar. Una semilla vacía devuelve `false` en el cliente y `true` en el backend.
- **Pruebas:** los vectores (`vectores/sorteo.vectores.json`, 4 casos) se calcularon en Python y no en Java (`vectores/README.md:10-21`, hueco declarado). Ninguno cubre los bordes.

### B-07 · El cliente calcula «gastable» y «total de la cuenta» (saldo ad-hoc, regla 91.1.5)
- **Clase:** DINERO / ARQ · **Severidad:** S2.
- **Ubicaciones:**
  - `packages/dominio-cliente/src/posiciones.ts:86` (`aCadena(disponible + retenido)`) y `:90-96` (`gastable: aCadena(disponible - comprometido)`, con `comprometido` recortado en silencio a `disponible` en `:91-92`)
  - Consumidor: `apps/backoffice/src/app/rutas/operacion/dominio/cu13-consultar-saldo.ts:3,33`
- **Lo que está bien:** usa BigInt, nunca `number`; con monedas distintas falla cerrado; si falta `comprometidoEnPozos`, `gastable` queda en `null`.
- **Lo que está mal:** la regla de negocio «gastable = disponible − comprometido», y qué hacer si lo comprometido excede lo disponible, se decide en el navegador y no en el libro. El comentario `posiciones.ts:5-8` y el de `posiciones.spec.ts:5-7` dicen que «el productor todavía NO implementa» `comprometidoEnPozos`, pero el contrato ya lo declara (`servicios/nucleo-financiero/.../nucleo-financiero.yaml:1507-1515`).
- **Datos del mock:** `packages/simulado/ejemplos/nucleo-financiero/consultarSaldo.json` no trae `comprometidoEnPozos` en ningún escenario (grep: 0), así que la rama `separado: true` nunca se ve con el mock.

### B-08 · Patrón del contrato mal escapado: el mock «valida» importes como `"\d\O\dd"`
- **Clase:** CONTRATO / DINERO · **Severidad:** S2. Una respuesta real como `"100.00"` **no** cumple el esquema publicado.
- **Ubicaciones:**
  - `packages/simulado/ejemplos/cumplimiento/elevarDiligencia.json:18,44,61` (`"monto": "\\d\\O\\dd"`) y `.../cumplimiento/verificarAlcance.json:14,26,38` (`"montoOperacion": "\\d\\O\\dd"`)
  - Origen: `servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml:1251,1339` → `pattern: '^-?\\d+\\.\\d{2}$'`. En YAML con comillas simples la barra doble queda literal, así que el patrón exige barras invertidas.
  - Además, los dos importes van sin moneda.
- **Cómo lo comprobé:** `grep -rnE "pattern: '[^']*\\\\\\\\"` sobre todos los OpenAPI (solo hay esas 2 líneas) y lectura de los ejemplos.
- **Fallo de las pruebas:** `ejemplos-contra-esquema.spec.ts` está en verde **porque** el ejemplo es basura que cumple un patrón roto. La prueba de contrato no detecta un patrón inutilizable.

### B-09 · `moneda` sin ISO-4217 en el mock: `"MMM"`, `"mon"`, `"moneda"`
- **Clase:** DINERO / DATOS / CONTRATO · **Severidad:** S3.
- **Ubicaciones:**
  - `packages/simulado/ejemplos/aportes/consultarRecaudoDiario.json:14,40` (`"MMM"`)
  - `notificaciones/listarEnvios.json` (`"MMM"`)
  - `grupos/consultarDetalleDelGrupo.json:14,50,75`, `grupos/consultarGrupoDeBackoffice.json:16,42,60`, `grupos/listarGruposDeBackoffice.json` y `tarifas/listarCostosDeProveedor.json` (`"mon"`)
  - `grupos/consultarEnlaceDelGrupo.json:13,25,37,49` y `grupos/consultarInvitacionPorEnlace.json` (`"moneda"`)
- **Causa:** en el contrato, `moneda` es a veces `enum: [BOB, USD]` (`aportes.yaml:570`), a veces solo `pattern: '^[A-Z]{3}$'` (`aportes.yaml:536`) y a veces texto libre o con largo 3 (`grupos.yaml:1571,1614,1689,1741,1872`). `muestra.ts:91-106` rellena el texto con el nombre del campo.
- **Impacto:** ninguna prueba que use el mock ejercita una moneda válida en esas vistas, y `formatearMonto` imprime `"mon 4,44"`.
- **Importes con forma decimal sin moneda al lado:** 32 archivos, todos por herencia del contrato. Ejemplos: `nucleo-financiero/listarReversos.json` (`montoReversado`), `nucleo-financiero/listarCierresDiarios.json` (`totalRecaudado/Conciliado/Excepciones`), `garantia/consultarRestriccion.json` (`montoQueLaLevanta`), `organizador/consultarHabilitacion.json` (`limiteDeMonto`), `cumplimiento/consultarReclamo.json`, `erp/*` y `publicidad/*`. La lista completa la produce el script de la sección «Evidencia».

### B-10 · Ejemplos «ok» que violan las invariantes de dinero: todo vale `"4.44"`
- **Clase:** DATOS / DINERO · **Severidad:** S3.
- **Ubicaciones:** 111 archivos y 602 apariciones de `"4.44"`. Por ejemplo:
  - `entregas/liquidarEntrega.json`: `bruto = totalDeducciones = neto = 4.44`, cuando debería cumplirse neto = bruto − deducciones.
  - `aportes/consultarRecaudoDelPeriodo.json:13-28`: `pozo = confirmado = cubiertoMutual = faltante = 4.44`.
  - `nucleo-financiero/cotizarOperacion.json`: `base = comision = impuesto = costoTotal = neto`.
- **Causa:** `muestra.ts:118-124` toma el punto medio del rango de `randexp` sobre `^\d+\.\d{2}$`, y nadie editó los valores.
- **Impacto:** las pruebas de UI con «ok» no detectan sumas mal presentadas ni desgloses incoherentes.

### B-11 · El escenario `adverso` es una copia de `ok` (336 de 349)
- **Clase:** HUECO / PRUEBAS · **Severidad:** S3.
- **Ubicaciones:** `packages/simulado/scripts/generar-ejemplos.mjs:109`, donde `adverso: { estado: exito, cuerpo: cuerpo(documento, operacion, exito, 'representativo') }` es idéntico a `ok` (`:107`). Resultado: 336 archivos con `adverso == ok`. Solo 13 lo editaron, por ejemplo `nucleo-financiero/consultarSaldo.json` y `aportes/generarObligacionesDelPeriodo.json`.
- **Impacto:** el «estado adverso» que exige la regla 65 (nivel límite) no existe en el doble, aunque cada archivo declare que sí.

### B-12 · `intermitente.luego` está declarado y no lo implementa nadie
- **Clase:** HUECO / RESILIENCIA · **Severidad:** S3.
- **Ubicaciones:**
  - `packages/simulado/src/ejemplos.ts:8-9` (`luego?: Escenario // la segunda llamada con la misma clave`)
  - `generar-ejemplos.mjs:113` (`luego: 'ok'`), presente en todos los POST (por ejemplo `aportes/cobrarAporte.json:59,66`)
- **Referencias:** grep de `luego` en `apps/` y `packages/` solo encuentra la declaración del tipo. `inyectar-ejemplos.mjs:49-67` inyecta el ejemplo como 503 fijo, y `servidor-mock.mjs` no tiene estado por clave.
- **Impacto:** el «reintento con la misma clave que tiene éxito», es decir la prueba de idempotencia del cliente, no se puede simular, aunque el JSON lo prometa.

### B-13 · Rechazos con 401/403/400/404 que llevan un código de negocio que no corresponde
- **Clase:** DATOS / CONTRATO / FRAGIL · **Severidad:** S3.
- **Conteo:** 401 → 21 casos, 400 → 10, 404 → 9, 403 → 4. Ejemplos:
  - `aportes/consultarRecaudoDiario.json:50-53`: 401 con `AP-CU51-05 "Filtro de listado invalido"`
  - `aportes/listarProveedoresDePago.json`: 401 con `AP-CU21-01 "Obligacion no vigente"`
  - `aportes/recibirWebhookPasarela.json:52-53`: 401 con `AP-CU100-01 "Periodo ya cerrado"`. Esto es FLUJO-QR: el webhook no tiene un escenario de firma inválida.
  - `nucleo-financiero/consultarSaldo.json`: 401 con `AP-CU24-01 "Asiento descuadrado"`
  - `auditoria/*`: 401 o 404 con `AP-CU07-01 "Identidad no verificada"`
  - `identidad/consultarUsuarioInterno.json`: 404 con `AP-CU01-03 "Cuenta ya existe"`
- **Causa:** `generar-ejemplos.mjs:104` toma como respaldo el primer 4xx aunque sea 401 o 403. Después `errorDeCatalogo` (`:44-53`) le asigna el primer código del **servicio** (`porServicio`), que no tiene relación con la operación. A eso se suman 8 rechazos con el mensaje genérico de `catalogo.ts:132` («La operación fue rechazada por la regla AP-CU136-0x.»), por ejemplo `entregas/abrirSubastaDeCupo.json`, `pujarEnSubasta.json` y `organizador/*HabilitacionBasica.json`.
- **Impacto:** una pantalla probada con `rechazo` trata un 401 (sesión vencida) como un error de negocio, o al revés.

### B-14 · Valores de relleno con forma de dato en el «ok»
- **Clase:** DATOS · **Severidad:** S4.
- **Ubicaciones:** `"estado": "estado"` en 25 archivos (por ejemplo `nucleo-financiero/cerrarRetencion.json`, `solicitarRecarga.json`, `entregas/liquidarEntrega.json`, `garantia/declararIncumplimiento.json`), `"estadoObligacion": "estadoObligacion"` (`aportes/aprobarReembolso.json:13`) y `"zonaHoraria": "zonaHoraria"` (`aportes/consultarRecaudoDiario.json:11,28`, donde debería ir `America/La_Paz`). Todas las fechas valen `2026-01-15T10:30:00Z` (`muestra.ts:96-100`).
- **Impacto:** los enums sin `enum` en el contrato se prueban con texto arbitrario, y la zona horaria nunca se ejercita.

### B-15 · Las dos pruebas de contraste WCAG de tokens no las corre nadie
- **Clase:** PRUEBAS / UX-A11Y · **Severidad:** S3.
- **Ubicaciones:** `packages/tokens/pruebas/roles-contraste.test.mjs` (127 líneas) y `packages/tokens/pruebas/texto-de-ejemplo-contraste.test.mjs` (30 líneas), ambas escritas con `node:test`. `packages/tokens/vitest.config.ts:2` solo incluye `pruebas/**/*.spec.ts`. `package.json:16` tiene `"test:front": "vitest run"`.
- **Cómo lo comprobé:** grep de `roles-contraste|texto-de-ejemplo-contraste|node --test` en `.github/workflows`, `package.json` y `scripts`. La única coincidencia es `scripts/humo.spec.mjs` (no relacionado) y la mención en `tokens.json:218`, que afirma «Contraste medido en pruebas/roles-contraste.test.mjs».
- **Impacto:** un cambio de color que rompa 4,5:1 en tema claro, oscuro o backoffice pasa el CI.

### B-16 · Clientes generados versionados bajo una ruta ignorada: instantánea parcial y desfasada
- **Clase:** CONFIG / CONTRATO / REESCRITURA · **Severidad:** S3.
- **Hechos:**
  - `.gitignore:50-51` ignora `clientes/angular/` y `clientes/dart/`, pero hay 26 archivos versionados: los 11 barriles y 11 `pubspec.yaml` de Dart, y 4 modelos de `clientes/angular/publicidad/model/`. El último commit es `b64e073d 2026-10-30`. Se regeneran con `generateOpenApiClients` (`build.gradle.kts:39,44`; `buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts:73-110`, con `typescript-angular` y `dart-dio`).
  - Son salida del generador: llevan la cabecera `AUTO-GENERATED FILE` y la cabecera de openapi-generator.
- **Deriva frente al contrato fuente** (exportados en el barril contra `components.schemas`):
  - `clientes/dart/aportes/lib/aportaya_cliente_aportes.dart` no tiene `primera_cuota`, `proveedor_de_pago_resumen` ni `webhook_recibido`. La operación `consultarPrimeraCuota` (`aportes.yaml:353-358`) tampoco tiene ejemplo en el simulado: es la única de 350 sin ejemplo.
  - `clientes/dart/notificaciones/...` no tiene `evento_de_entrega_recibido` ni `proveedor_de_mensajeria_resumen`.
  - `clientes/dart/tarifas/...` no tiene `costo_de_proveedor`.
  - `clientes/dart/nucleo-financiero/...` exporta `entrada_transferencia_destino`, que ya no es un esquema del contrato.
  - `clientes/dart/transparencia/...` exporta `hecho_a_sellar` (`HechoASellar`; el ajuste de mayúsculas de la heurística puede explicar parte).
- **Cobertura:** no hay barril Dart versionado para `auditoria`, `erp`, `inversiones` ni `publicidad`, y de Angular solo hay 4 modelos de un servicio.
- **Contradicción con el doble:** `apps/backoffice/src/tipos/clientes-angular.doble.d.ts:4-31` llama «DOBLE TEMPORAL» a esos 4 `.ts` y dice que `clientes/` «no existe en el árbol de dev» (2026-09-21). Pero los 4 archivos son salida literal del generador y coinciden con `publicidad.yaml`: comprobé campos, `required` y enums de `EntradaAnunciante`, `EntradaCampana`, `EntradaRevision` y `SalidaCampana`. `entradaCampana.ts:49` importa `./conjunto`, que no está versionado, así que sin generar no compila. El doble también nombra `clientes/angular/identidad.ts`, que no existe en el árbol.
- **Comentario que miente (fuera del lote):** `aportaya.openapi.gradle.kts` dice «clientes/typescript/ es GENERADO … el CI regenera y falla si hay diff». La ruta es otra y el directorio está ignorado, así que no hay diff que pueda fallar.
- **Resolución en las apps:** Flutter usa dependencias por ruta (`apps/movil/pubspec.yaml:29-51` → `../../clientes/dart/<svc>`). Angular usa alias de `paths` (`tsconfig.base.json:27`, `apps/backoffice/tsconfig.json:27-28`, `apps/web/tsconfig.json:30-31`). Cada `clientes/dart/<svc>/pubspec.yaml:3-4` declara `homepage: homepage` y `description: OpenAPI API client`, que son plantilla sin rellenar.

### B-17 · Tokens: lista de exclusión fija que diverge entre CSS y Dart; helper duplicado
- **Clase:** FRAGIL / DUP-INT · **Severidad:** S4.
- **Ubicaciones:**
  - `packages/tokens/scripts/a-css.mjs:13` filtra a mano `['ok','okbg','warn','warnbg','err','errbg','info','infobg']`. Omite `pii`/`piibg`, así que `--pii` se declara dos veces en `:root` (primitiva y rol).
  - `a-dart.mjs:67` emite **todas** las primitivas en `Paleta`.
  - `aKebab` está copiado en `pruebas/generados-coinciden.spec.ts:18` y `pruebas/tokens-contra-boveda.spec.ts:91`, además de `scripts/comun.mjs:16`.
  - `a-dart.mjs:116` usa `Theme.of(context).extension<Tokens>()!` y `:119` define `Tokens copyWith() => this`, que ignora cualquier cambio.
  - `pruebas/generados-coinciden.spec.ts:13-14` ejecuta los generadores con `execFileSync` mientras la prueba se importa, y eso escribe `generado/`.

### B-18 · Formato de dinero: correcto, con bordes sin cubrir
- **Clase:** DINERO / DEFECTO · **Severidad:** S4.
- **Ubicaciones:**
  - `packages/tokens/dinero/formatear.ts:24` acepta `"-0.00"`, que se muestra `"-Bs 0,00"`.
  - `:31-34`: con una moneda desconocida, `prefijoDe` devuelve el código tal cual, sin validar ISO-4217 (`ImporteDelContrato.moneda: string`).
  - `vectores/monto.json`: 5006 casos, todos `BOB` salvo 1 `USD`. No hay vectores de moneda desconocida ni de `-0.00`.
  - `formatear-monto.spec.ts:20-25` compara la función contra vectores que generó esa misma función (`scripts/vectores.mjs:43`). Es tautológico para TypeScript. Su valor real está del lado de Dart (`packages/diseno_flutter/test/unidad/formatear_monto_test.dart`).
- **Lo que está bien:** en ningún punto pasa por `number`.

### B-19 · `@aportaya/simulado`: frontera hacia producción y hechos menores del paquete
- **Clase:** CONFIG / SEG / DOC · **Severidad:** S4.
- **Riesgo de llegar a producción, controlado:**
  - Los únicos imports fuera de pruebas son `apps/backoffice/src/app/rutas/sistemas/dominio/proveedor-fuentes.demo.ts:15,19` (import dinámico). Ese archivo solo entra por `fileReplacements` en las configuraciones `development` y `demo` (`apps/backoffice/angular.json`, bloques en `:70-74` y `:91-95`). `production` es la configuración por defecto.
  - `@aportaya/simulado` es devDependency en `apps/web`, `apps/backoffice` y `packages/ui`.
  - `packages/ui/package.json:36` y `packages/ui/tsconfig.json:24` declaran la dependencia, pero no hay ningún import (grep: 0): es una dependencia muerta.
- **PII con apariencia real** en datos sintéticos: `packages/simulado/src/backoffice-sistemas/datos.ts:73-74`, campo `persona`. Son correos con forma inicial.apellido@dominio-de-la-empresa. Se omite el valor.
- **`servidor-mock.mjs:12`:** escucha en `hostname: '0.0.0.0'`, expuesto en todas las interfaces de la máquina de desarrollo.
- **`ejemplos.ts:25-34`:** busca la raíz del repositorio con `process.cwd()` al **cargar el módulo**, y lo reexporta `src/index.ts:5`. Importar `muestraDe` fuera del repo lanza un error.
- **Comentarios que mienten:**
  - `inyectar-ejemplos.mjs:2` dice «los catorce contratos» y `servicios/` tiene 15. La ruta `generado/prism/…` y la palabra «Prism» siguen, aunque el servidor es Scalar (`servidor-mock.mjs:5`, `package.json:31`).
  - `contratos-a-json.mjs:3` dice «Metro y Jest empaquetan JSON», un resto de React Native.
  - `muestra.ts:20-21` cita `CU01.contrato.spec.ts`, que no existe (`git ls-files | grep CU01.contrato` → 0).
- **Pruebas que dependen de `generado/` sin versionar:** `qr-interno-vs-interoperable.spec.ts:23` hace `readFileSync` en la importación (lanza sin build). `catalogo-de-errores.spec.ts:42-43` usa umbrales literales `> 50`. `ejemplos-contra-esquema.spec.ts:47-49` salta en silencio los estados 202 y 503 y los escenarios sin esquema.

### B-20 · `pagarRecargaDePrueba` y el escenario `qrDePrueba` en el contrato de producción
- **Clase:** FLUJO-QR / CONFIG · **Severidad:** S4. Está declarado y protegido por perfil en el backend; se lista por la regla del QR confirmado por webhook.
- **Ubicaciones:**
  - `packages/simulado/ejemplos/nucleo-financiero/pagarRecargaDePrueba.json` (`/recargas-de-prueba/{ordenId}/pago`, «da por pagado el QR de prueba»)
  - `.../solicitarRecarga.json:73` (escenario `qrDePrueba`, `"origen": "PRUEBA"`). Es un 7.º escenario que no está en `ESCENARIOS` (`src/escenarios.ts:11`), así que `ejemploDe()` no lo puede pedir tipado.
  - Contrato: `servicios/nucleo-financiero/.../nucleo-financiero.yaml:219-232`, que responde 404 fuera del perfil de prueba.

## 3) Lo que está bien (preservar)

- **Ningún importe como número JSON** en los 349 ejemplos: 0 floats (script). Los importes de dinero viajan como `{monto: "x.yy", moneda}` en casi todo `aportes`, `nucleo-financiero`, `entregas` y `garantia`.
- **Deriva de forma nula frente al OpenAPI fuente:**
  - 349 ejemplos y 1304 escenarios validados con jsonschema 2020-12, con 0 inválidos.
  - 0 rutas o métodos ausentes, 0 estados no declarados.
  - 350 operaciones en los contratos y solo 1 sin ejemplo (`aportes/consultarPrimeraCuota`).
- **`formatearMonto`** se hace solo con cadenas, lanza ante una forma fuera de contrato y tiene un único punto de verdad: `packages/ui/src/monto/formatear.ts` lo reexporta y Flutter consume los vectores.
- **`posiciones.ts`:** aritmética BigInt, falla cerrada con monedas distintas o importes negativos, `null` honesto si falta el dato y pruebas en tres niveles.
- **`sorteo.ts`:** igual a Java en el camino normal (separador `\n`, `semilla:paso`, módulo sin signo, Fisher-Yates desde el final). Comprobado contra `SorteoVerificable.java:44,88-100`.
- **El mock no se cuela en producción** por la ruta normal (ver B-19). Los datos sintéticos están marcados con el sufijo `Simulado` y llevan su justificación.
- **PII en ejemplos:** los nombres, documentos y correos son marcadores (`"nombres"`, `persona@ejemplo.invalid`). Los teléfonos de `identidad/listarVerificaciones.json` y `resolverVerificacion.json` son `+591` seguido de un patrón de 2 dígitos distintos, es decir sintéticos.
- **Tokens:** generación única de CSS y Dart con prueba de paridad de roles y prueba contra la bóveda; tema claro, oscuro y backoffice; `prefers-color-scheme`.
- **`catalogo.ts`:** los códigos de error salen del contrato y de los documentos, no se escriben a mano. La prueba `catalogo-de-errores.spec.ts:58-60` impide un 2xx con cuerpo de error.

## 4) Visto fuera del lote (una línea cada uno)

- `servicios/entregas/src/main/resources/openapi/entregas.yaml:324-326,792,800`: CU-22 «pozo completo» con `cubiertoEmpresa` (fuente de B-01).
- `servicios/garantia/.../garantia.yaml:797,809`: `requiereAprobacion` en la cobertura de un incumplimiento.
- `servicios/grupos/.../grupos.yaml:1638`: `enum: [PENDIENTE_BACKOFFICE]`.
- `servicios/cumplimiento/.../cumplimiento.yaml:1251,1339`: patrón de importe con escape doble, imposible de cumplir (B-08).
- `servicios/*/openapi/*.yaml`: el esquema de `moneda` no es uniforme (`aportes.yaml:536` contra `:570`; `grupos.yaml:1571,1614,1689,1741,1872`).
- `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CadenaDeBloques.java:40`: `PRIMER_NUMERO = 2`, un parche por una restricción contradictoria de la base (`ck_bloque_genesis` contra `NOT NULL`).
- `buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts`: un comentario cita `clientes/typescript/` y un gate de diff en CI que no puede funcionar sobre una ruta ignorada.
- `apps/backoffice/src/tipos/clientes-angular.doble.d.ts:1-77`: doble con `any` y fecha caducada (2026-09-21); su `eslint-disable` está justificado, pero el doble ya no es necesario.
- `apps/movil/lib/dominio/configuracion.dart`: clon Dart de `validarConfiguracion`; conviene revisar el hueco de B-04.
- `apps/backoffice/src/app/rutas/sistemas/dominio/datos-simulados.ts:16-20`: `puedeConfirmar`, una «doble confirmación simulada» por correo (APROB, backoffice).
- `landing/assets/css/tokens.css`: segunda copia de tokens, versionada y hecha a mano fuera de `packages/tokens` (posible DUP-X).
- `servicios/nucleo-financiero/.../nucleo-financiero.yaml:219-232`: ruta `/recargas-de-prueba` dentro del contrato de producción.

## Evidencia (salidas recortadas)

```text
$ python3 -I scratchpad/b-tools/deriva.py
contratos 15 ops en contratos 350 ejemplos 349
OK escenarios validados 1304
RUTA/METODO ausentes en contrato 0
ESCENARIO con estado no declarado 0
INVALIDOS 0
OPS SIN EJEMPLO 1  [('aportes', 'consultarPrimeraCuota')]

$ (script floats/monedas) FLOATS 0 · MONEDA rara {'aportes/consultarRecaudoDiario.json': {'MMM'}, 'grupos/consultarDetalleDelGrupo.json': {'mon'}, 'grupos/consultarEnlaceDelGrupo.json': {'moneda'}, ...}
$ (script) adverso identico a ok 336 de 349
$ (script) rechazos por estado Counter({422: 279, 401: 21, 400: 10, 404: 9, 403: 4, 429: 1}) · mensaje generico 8
$ grep -l '"4.44"' */*.json | wc -l → 111 ; total apariciones 602

$ node --experimental-strip-types (validarConfiguracion)
{"valida":true,"url":"/\\evil.example.com/api/v1"}
{"valida":false,"motivo":"host-ajeno"}          # http://[::1]:4010/api/v1 en desarrollo
$ node -e "new URL('/\\\\evil.example.com/api/v1','https://backoffice.aportaya.bo').href"
https://evil.example.com/api/v1

$ git check-ignore -v clientes/dart/aportes/lib/aportaya_cliente_aportes.dart
.gitignore:51:clientes/dart/   (sin embargo está en git ls-files)
```

**No cubierto:**
- No corrí `vitest` ni el generador de OpenAPI: estaba prohibido.
- La paridad de los clientes Dart se estimó por nombres de esquema en snake_case, una heurística sin `src/` generado.
- No ejecuté el servidor Scalar para confirmar si respeta `Prefer: example=`.
- `apps/movil/lib/dominio/configuracion.dart` no se probó contra el vector de B-04.

## 1) Tabla de cobertura
| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| clientes/angular/publicidad/model/entradaAnunciante.ts | 39 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/angular/publicidad/model/entradaCampana.ts | 39 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/angular/publicidad/model/entradaRevision.ts | 25 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/angular/publicidad/model/salidaCampana.ts | 30 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/dart/aportes/lib/aportaya_cliente_aportes.dart | 37 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/aportes/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/cumplimiento/lib/aportaya_cliente_cumplimiento.dart | 67 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/cumplimiento/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/entregas/lib/aportaya_cliente_entregas.dart | 49 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/entregas/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/garantia/lib/aportaya_cliente_garantia.dart | 53 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/garantia/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/grupos/lib/aportaya_cliente_grupos.dart | 82 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/grupos/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/identidad/lib/aportaya_cliente_identidad.dart | 65 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/identidad/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/notificaciones/lib/aportaya_cliente_notificaciones.dart | 31 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/notificaciones/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/nucleo-financiero/lib/aportaya_cliente_nucleofinanciero.dart | 67 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/nucleo-financiero/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/organizador/lib/aportaya_cliente_organizador.dart | 55 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/organizador/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/tarifas/lib/aportaya_cliente_tarifas.dart | 42 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/tarifas/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/transparencia/lib/aportaya_cliente_transparencia.dart | 57 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/transparencia/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| packages/dominio-cliente/eslint.config.js | 7 | sí | Config ESLint |
| packages/dominio-cliente/package.json | 27 | sí | Manifiesto dominio-cliente |
| packages/dominio-cliente/src/cadena.spec.ts | 68 | sí | Autoconsistencia de la cadena |
| packages/dominio-cliente/src/cadena.ts | 90 | sí | serializarCanonico/hashDeBloque/recorrerCadena (sin referencia backend) |
| packages/dominio-cliente/src/configuracion.spec.ts | 140 | sí | Casos válidos/límite/inválidos de validarConfiguracion |
| packages/dominio-cliente/src/configuracion.ts | 122 | sí | Valida URL de gateway (D-A6) |
| packages/dominio-cliente/src/index.ts | 4 | sí | Barril |
| packages/dominio-cliente/src/posiciones.spec.ts | 81 | sí | Tres niveles de separarPosiciones |
| packages/dominio-cliente/src/posiciones.ts | 106 | sí | Separa disponible/gastable/comprometido/retenido con BigInt |
| packages/dominio-cliente/src/sha256.ts | 24 | sí | SHA-256 sobre SubtleCrypto + índice modular |
| packages/dominio-cliente/src/sorteo.spec.ts | 54 | sí | Vectores dorados del sorteo |
| packages/dominio-cliente/src/sorteo.ts | 51 | sí | Reimplementación TS de SorteoVerificable (compromiso + Fisher-Yates) |
| packages/dominio-cliente/tsconfig.json | 6 | sí | Config TS |
| packages/dominio-cliente/vectores/README.md | 31 | sí | Origen de los vectores (Python, no Java) |
| packages/dominio-cliente/vectores/sorteo.vectores.json | 146 | sí | 4 vectores dorados del sorteo |
| packages/dominio-cliente/vitest.config.ts | 2 | sí | Config vitest |
| packages/simulado/.gitignore | 2 | sí | Ignora generado/ |
| packages/simulado/ejemplos/aportes/aprobarReembolso.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/reembolsos/{reembolsoId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/cobrarAporte.json | 78 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /aportes/obligaciones/{obligacionId}/pagos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/consultarEstadoDelParticipante.json | 41 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/participantes/{participanteId}/estado (ok, vacio, adverso) |
| packages/simulado/ejemplos/aportes/consultarRecaudoDelPeriodo.json | 108 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/periodos/{periodoId}/recaudo (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/consultarRecaudoDiario.json | 60 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/recaudo-diario (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/contarMorosos.json | 29 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/grupos/{grupoId}/morosos (ok, vacio, adverso) |
| packages/simulado/ejemplos/aportes/darDeAltaProveedor.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/proveedores (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/enrutarCobro.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/enrutamiento (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/generarObligacionesDelPeriodo.json | 153 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /aportes/periodos/{periodoId}/obligaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/listarProveedoresDePago.json | 49 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /pagos/proveedores (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/listarWebhooksRecibidos.json | 51 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /pagos/webhooks-recibidos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/recibirWebhookPasarela.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/webhooks/{proveedorCodigo} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/registrarDisputa.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/{pagoId}/disputas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/solicitarReembolso.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/{pagoId}/reembolsos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/actualizarEstadoElementoCatalogo.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PATCH /auditoria/catalogos/{codigo}/elementos/{id} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarBitacora.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/bitacora (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarCatalogoOperativo.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/catalogos/{codigo} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarEventoBitacora.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/bitacora/{id} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarOutboxPorServicio.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/outbox (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarSaludDeServicios.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/servicios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/descargarExportacion.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reportes/exportaciones/{exportacionId}/descargas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/ejecutarReporte.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reportes/{definicionId}/ejecuciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/ejercerDerechos.json | 76 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /auditoria/derechos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/guardarFichaDeGobierno.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/gobierno (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/listarCatalogosOperativos.json | 53 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/catalogos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarDefinicionesDeReporte.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reportes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarEjecucionesDeReporte.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reportes/ejecuciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarElementosCatalogo.json | 57 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/catalogos/{codigo}/elementos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarEndpointsDelDia.json | 64 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/endpoints (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarIncidentesOperativos.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/incidentes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarTablasDelCatalogo.json | 600 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos/tablas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/obtenerCatalogoDeDatos.json | 140 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/obtenerResumenDelCatalogo.json | 402 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos/resumen (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/obtenerTablaDelCatalogo.json | 550 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos/tablas/{esquema}/{tabla} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/publicarTablero.json | 93 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/revisarColumna.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/columnas/{columna}/revision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/abrirCasoLft.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/casos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/aceptarContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/contratos/{contratoId}/aceptaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/aprobarYEnviarReporte.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/reportes/envios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/consultarEfectoDeReclamos.json | 75 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reclamos/usuarios/{usuarioId}/efecto (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/consultarReclamo.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reclamos/{reclamoId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/decidirCasoLft.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/casos/{casoId}/decision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/declararPep.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/usuarios/{usuarioId}/pep (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/elevarDiligencia.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/usuarios/{usuarioId}/diligencia (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/generarReporteRegulatorio.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/reportes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarActasDeComite.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/comites/actas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarBandejaDeAlertas.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/alertas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarBandejaDeCasos.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/casos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarBandejaDeReclamos.json | 75 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reclamos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarContratosVigentes.json | 76 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/contratos/vigentes (ok, vacio, adverso) |
| packages/simulado/ejemplos/cumplimiento/listarEventosDeRiesgo.json | 89 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/riesgos/eventos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarIncidentesDeSeguridad.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/seguridad/incidentes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarPruebasDeContinuidad.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/continuidad/pruebas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarReportesRegulatorios.json | 69 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /uif/reportes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarRequerimientosDeAutoridad.json | 67 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /uif/requerimientos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/notificarTitulares.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/seguridad/incidentes/{incidenteId}/notificaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarIncidente.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/seguridad/incidentes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarPruebaDeContinuidad.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/continuidad/pruebas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarRequerimientoDeAutoridad.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/requerimientos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarRiesgoOperativo.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/riesgos/eventos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/reportarIncidente.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/seguridad/incidentes/{incidenteId}/reportes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/responderReclamo.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reclamos/{reclamoId}/respuesta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/responderRequerimientoDeAutoridad.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/requerimientos/{requerimientoId}/respuesta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/verificarAlcance.json | 53 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /licencia/alcance (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/abrirSubastaDeCupo.json | 174 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/subastas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/anotarRespuestaDesembolso.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /desembolsos/{ordenId}/respuesta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/autorizarEntrega.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/{entregaId}/autorizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/cancelarOferta.json | 54 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/mercado/ofertas/{ofertaId}/cancelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/cancelarSubastaDeCupo.json | 174 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/subastas/{subastaId}/cancelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/comprarOferta.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/mercado/ofertas/{ofertaId}/compra (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/consultarDisponibilidad.json | 41 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /cuentas-bancarias/{cuentaId}/disponibilidad (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/consultarOfertaParaPago.json | 65 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/mercado/ofertas/{ofertaId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/consultarSubastaDeCupo.json | 128 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/subastas/{subastaId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/designarCuentaPrincipal.json | 50 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cuentas-bancarias/{cuentaId}/principal (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/ejecutarEntrega.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/{entregaId}/ejecucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/emitirOrdenDesembolso.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /desembolsos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/fondearPozo.json | 142 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/pozo-completo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/liquidarEntrega.json | 102 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/listarDesembolsos.json | 75 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /desembolsos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/listarOfertas.json | 76 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/mercado/ofertas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/listarSubastasDeCupo.json | 108 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/subastas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/publicarOferta.json | 54 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/mercado/ofertas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/pujarEnSubasta.json | 54 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/subastas/{subastaId}/pujas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/registrarCuentaDestino.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cuentas-bancarias (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/verificarCuentaDestino.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cuentas-bancarias/{cuentaId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/abrirCuentaPorCobrar.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/cuentas-por-cobrar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/abrirEjercicioFiscal.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/ejercicios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/aprobarOrdenDeCompra.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/ordenes-de-compra/{ordenId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/aprobarPresupuesto.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/presupuestos/{presupuestoId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/cerrarPeriodoContable.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/periodos/{periodoId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/consultarPeriodoVigente.json | 53 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/periodos/vigente (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/correrDepreciacionDelPeriodo.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/periodos/{periodoId}/depreciaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/crearOrdenDeCompra.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/ordenes-de-compra (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/crearPresupuesto.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/presupuestos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/darDeAltaTercero.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/terceros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/depreciarActivo.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/activos/{activoId}/depreciaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/descargarEstadoFinanciero.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/estados-financieros/{estadoFinancieroId}/documento (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/generarEstadoFinanciero.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/periodos/{periodoId}/estados-financieros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/listarActivosFijos.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/activos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarCuentasPorCobrar.json | 85 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/cuentas-por-cobrar (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarEjerciciosFiscales.json | 76 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/ejercicios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarFacturasDeProveedor.json | 91 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/facturas-de-proveedor (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarOrdenesDeCompra.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/ordenes-de-compra (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarPartidasPresupuestarias.json | 85 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/partidas-presupuestarias (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarPeriodosDelEjercicio.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/ejercicios/{ejercicioId}/periodos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/pagarFacturaDeProveedor.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/facturas-de-proveedor/{facturaId}/pagos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/registrarCobro.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/cuentas-por-cobrar/{cuentaId}/cobros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/registrarFacturaDeProveedor.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/facturas-de-proveedor (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/validarPlantillaDeAsiento.json | 44 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/plantillas-de-asiento/{plantillaId}/validacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/garantia/ampliarRespaldo.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/reservas/{reservaId}/ampliacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/aprobarReemplazo.json | 50 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/reemplazos/{reemplazoId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/cerrarDisolucion.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/disoluciones/{disolucionId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/consultarRestriccion.json | 32 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /cobranza/restricciones/vigentes/{usuarioId} (ok, vacio, adverso) |
| packages/simulado/ejemplos/garantia/cubrirConRespaldo.json | 126 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/coberturas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/cubrirIncumplimiento.json | 82 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/cobertura (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/declararIncumplimiento.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/devolverFondo.json | 138 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/fondos/{grupoId}/devolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/ejecutarAval.json | 78 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/aval/ejecucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/ejecutarReemplazo.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/reemplazos/{reemplazoId}/ejecucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/iniciarDisolucion.json | 170 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/grupos/{grupoId}/disolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/levantarRestriccion.json | 50 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cobranza/restricciones/{restriccionId}/levantamiento (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/liberarRespaldo.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/reservas/{reservaId}/liberacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/presentarDescargo.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/descargo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/proponerReemplazo.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/reemplazo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/recuperarRespaldo.json | 102 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/recuperaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/reservarRespaldo.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/reservas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/resolverDescargo.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/descargo/resolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/restringirDeudor.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/restriccion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/reversarCoberturaRespaldo.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/coberturas/{coberturaId}/reversa (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/aceptarInvitacionPorEnlace.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/invitaciones/enlace/aceptar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/anularEnlaceDelGrupo.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/enlace-del-grupo/anulacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/calcularPlazoHabil.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/calendario/calcular (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/calcularVencimientoLocal.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/calendario/vencimiento (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/canjearInvitacion.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/invitaciones/{invitacionId}/canje (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/comprometerSorteo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/sorteo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/consultarActividad.json | 26 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/participantes/{usuarioId}/actividad (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarAdmisibilidadDePago.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/obligaciones/admisibilidad (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarCalendarioDeAportes.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/periodos/{periodoId}/calendario-de-aportes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarDetalleDelGrupo.json | 114 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/detalle (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarElegibilidadParaSubasta.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/elegibilidad-para-subasta (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarEnlaceDelGrupo.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/enlace-del-grupo/consultar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/consultarGrupoDeBackoffice.json | 87 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/backoffice/{grupoId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarInvitacionPorEnlace.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/invitaciones/enlace/consultar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/consultarMisLimites.json | 35 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/mis-limites (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarPaqueteDelSorteo.json | 56 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/sorteos/{sorteoId}/paquete (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarSiCompartenGrupo.json | 26 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/integrantes/{usuarioId}/comparte (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarSubastabilidadDelCupo.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/cupos/{cupoId}/subastabilidad (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/crearGrupo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/decidirSolicitudDeIngreso.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/solicitudes/{solicitudId}/decision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/firmarReglamentoDelGrupo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/reglamento/firma (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/generarEnlaceDelGrupo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/enlace-del-grupo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/historialAdmision.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/solicitudes/{solicitudId}/decisiones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/iniciarGrupo.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/inicio (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/invitarAlGrupo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/invitaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/listarAcuerdos.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /acuerdos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/listarBandejaDeAdmisiones.json | 57 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/solicitudes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/listarGruposDeBackoffice.json | 55 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/backoffice (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/listarMisParticipaciones.json | 32 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/participaciones (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/listarSolicitudesDeIngreso.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/solicitudes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/postularAlGrupo.json | 64 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/postulaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/proponerAcuerdo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /acuerdos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/proponerAdmision.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/solicitudes/{solicitudId}/propuestas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/resolverAdmision.json | 92 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/solicitudes/{solicitudId}/resoluciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/resolverAlias.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/participantes/alias/{alias} (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/revelarSorteo.json | 68 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/sorteo/revelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/revocarInvitacionDelGrupo.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/invitaciones/{invitacionId}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/solicitarIngresoPorEnlace.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/enlace-del-grupo/solicitudes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/solicitarPermuta.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /turnos/permutas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/solicitarRetiro.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/retiros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/sustituirAdministrador.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/administrador/sustitucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/traspasarCupo.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/cupos/{cupoId}/traspasos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/traspasarCupoPorSubasta.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/cupos/{cupoId}/traspasos-por-subasta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/votarAcuerdo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /acuerdos/{acuerdoId}/votos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/abrirDesafioDeStepUp.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesiones/desafios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/activarRolOrganizador.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/roles/organizador (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/actualizarFechaExpiracionDocumento.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios/{usuarioId}/documento/fecha-expiracion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/actualizarUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PATCH /usuarios-internos/{id} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/asignarRolesUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios-internos/{id}/roles (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/autenticar.json | 67 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesiones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/buscarPorTelefono.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/por-telefono (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/buscarUsuarioPorTelefono.json | 45 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/busquedas/por-telefono (ok, vacio, adverso, aceptado, intermitente) |
| packages/simulado/ejemplos/identidad/cambiarMiFotoDePerfil.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios/yo/foto-de-perfil (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/cerrarSesion.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos DELETE /sesion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/confirmarVerificacionCorreo.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/verificaciones/correo/{verificacionId}/confirmacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/consultarContactoEnGrupo.json | 35 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/contactos/grupos/{grupoId}/integrantes/{usuarioId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consultarEstadoDeVerificacion.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/{usuarioId}/verificacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consultarMiPerfil.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/yo/perfil (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/consultarNivelKyc.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/{usuarioId}/kyc (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consultarUsuarioInterno.json | 94 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios-internos/{id} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consumirInvitacion.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion/{tokenId}/consumos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/crearUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios-internos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/desbloquearUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios-internos/{id}/desbloquear (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/emitirTokenDeInvitacion.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/guardarBorradorOnboarding.json | 106 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios/verificaciones/correo/{verificacionId}/borrador (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/iniciarVerificacion.json | 72 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/listarPermisos.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /permisos (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/listarRoles.json | 46 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /roles (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/listarUsuariosInternos.json | 69 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios-internos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/listarVerificaciones.json | 56 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/verificaciones (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/registrarUsuario.json | 101 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/renovarSesion.json | 64 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesion/refrescar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/resolverVerificacion.json | 104 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /identidad/verificaciones/{verificacionId}/decision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/revocarInvitacion.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion/{tokenId}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/solicitarVerificacionCorreo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/verificaciones/correo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/subirDocumento.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/documentos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/validarTokenDeInvitacion.json | 41 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion/validar (ok, vacio, adverso, aceptado, intermitente) |
| packages/simulado/ejemplos/identidad/verContenidoDeFoto.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/verificaciones/{verificacionId}/fotos/{cara}/contenido (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/verFotoDelExpediente.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/verificaciones/{verificacionId}/fotos/{cara} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/verMiFotoDePerfil.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/yo/foto-de-perfil (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/verificarDesafioDeStepUp.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesiones/desafios/{desafioId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/verificarTitularidad.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/titularidad/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/correrDevengoDeDpf.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/devengos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/crearOrdenDeInversion.json | 98 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/ordenes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/estimarComisionDeExito.json | 98 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId}/comision (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/exportarReporteFiscalDeInversiones.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/reportes/fiscal/exportacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarComprobantesDeInversion.json | 99 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId}/comprobantes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarCorridasDeInversiones.json | 61 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/corridas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarDistribucionesDeInteres.json | 101 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId}/distribuciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarPosicionesDeInversion.json | 167 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarProductosDeInversion.json | 105 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/productos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/reintentarInstruccionesAlLibro.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/instrucciones/reintento (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/reporteFiscalDeInversiones.json | 274 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/reportes/fiscal (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/simularCajaDeInversion.json | 165 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/simulaciones/caja (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarCatalogoDeInversion.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/productos/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarOrdenDeInversion.json | 98 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/ordenes/{ordenId}/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarRescateDeInversion.json | 106 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/rescates/{rescateId}/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarValoresDeCuota.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/valores-cuota/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/solicitarRescateDeInversion.json | 106 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/posiciones/{posicionId}/rescates (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/verOrdenDeInversion.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/ordenes/{ordenId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/verPosicionDeInversion.json | 221 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/verProductoDeInversion.json | 128 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/productos/{codigo} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/consultarAvisosInternos.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/avisos-internos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/consultarSupresion.json | 26 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/supresion (ok, vacio, adverso) |
| packages/simulado/ejemplos/notificaciones/emitirAvisoInterno.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/avisos-internos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/emitirNotificacion.json | 60 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarEnvios.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/envios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarEventosDeEntregaRecibidos.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/webhooks-recibidos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarPlantillas.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/plantillas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarProveedoresDeMensajeria.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/proveedores (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/marcarAvisoLeido.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/avisos-internos/{id}/leido (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/marcarTodosAvisosLeidos.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/avisos-internos/marcar-todos-leidos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/procesarRespuestaEntrante.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/webhooks/{proveedor} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/verificarSupresion.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/supresion/consultas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/acreditarRecarga.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/recargas/{ordenId}/acreditacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/acreditarRescate.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/inversiones/acreditaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/aprobarRetiro.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retiros/{ordenId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/bloquearPorAutoridad.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/bloqueos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/cerrarRetencion.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones/{retencionId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/conciliarCustodia.json | 94 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /custodia/conciliaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/consultarCuentaPropia.json | 41 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/cuentas/propia (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/consultarSaldo.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/{cuentaId}/saldo (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/cotizarOperacion.json | 138 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/cotizaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/debitarInversion.json | 82 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/inversiones/debitos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/ejecutarCierreDiario.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /contabilidad/cierres-diarios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/emitirExtracto.json | 65 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/{cuentaId}/extracto (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/emitirQr.json | 78 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/qr (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/leerQr.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/qr/lecturas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/liberarRetencionDelMercado.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones/{retencionId}/liberacion-de-mercado (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarCierresDiarios.json | 67 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /contabilidad/cierres-diarios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarConciliaciones.json | 97 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /custodia/conciliaciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarMovimientosBilletera.json | 76 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/{cuentaId}/movimientos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarRetirosEnRevision.json | 85 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/retiros (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarReversos.json | 67 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /contabilidad/reversos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/pagarAlTercero.json | 82 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones/{retencionId}/pago-a-tercero (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/pagarQr.json | 94 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/qr/pagos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/pagarRecargaDePrueba.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /recargas-de-prueba/{ordenId}/pago (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/retenerSaldo.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/reversarTransaccion.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/reversos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/solicitarCierreBilletera.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/cierres (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/solicitarRecarga.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/recargas (ok, vacio, adverso, aceptado, intermitente, rechazo, qrDePrueba) |
| packages/simulado/ejemplos/nucleo-financiero/solicitarRetiro.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retiros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/transferirSaldo.json | 74 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/transferencias (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/aceptarHabilitacionBasica.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/yo/habilitacion-basica (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/activarRegla.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/reglas/{reglaId}/activacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/anotarEjecucion.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/tareas/{tareaId}/ejecuciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/apelarSancion.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/sanciones/{sancionId}/apelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/aprobarPostulacion.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones/{solicitudId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/aprobarTarea.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/tareas/{tareaId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/consultarExpedienteDeHabilitacion.json | 103 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/postulaciones/{solicitudId}/expediente (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/consultarHabilitacion.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/{organizadorId}/habilitacion (ok, vacio, adverso) |
| packages/simulado/ejemplos/organizador/consultarHabilitacionBasica.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/yo/habilitacion-basica (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/consultarHabilitacionPersonal.json | 39 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/usuarios/{usuarioId}/habilitacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/consultarMiExpedienteDeHabilitacion.json | 103 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/postulaciones/mia (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/definirRegla.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/reglas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/emitirContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/contratos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/evaluarDesempeno.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/evaluaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/firmarContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/contratos/{contratoId}/firma (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/firmarHabilitacionBasica.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/yo/habilitacion-basica/firma (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/habilitarOrganizador.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/habilitacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/listarBandejaDeHabilitaciones.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/postulaciones/bandeja (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/postularOrganizador.json | 77 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/programarTarea.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/tareas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/rescindirContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/contratos/{contratoId}/rescision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/resolverApelacion.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/sanciones/{sancionId}/resolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/resolverHabilitacion.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones/{solicitudId}/resolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/revisarHabilitacion.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones/{solicitudId}/revision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/sancionarOrganizador.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/sanciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/aprobarCampana.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/campanas/{campanaId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/consumoDelPeriodo.json | 41 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/cuentas/{cuentaId}/consumo (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/crearCampana.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/campanas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/darDeAltaAnunciante.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/anunciantes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/entregarAnuncio.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/espacios/{espacioId}/entrega (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/liquidarPeriodo.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/cuentas/{cuentaId}/liquidaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/listarAnunciantes.json | 69 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/anunciantes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/listarCampanas.json | 81 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/campanas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/listarPiezasCreativas.json | 63 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/piezas-creativas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/moderarPiezaCreativa.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/piezas-creativas/{piezaId}/revision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/postularSocioComercial.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/socios-comerciales (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/programarAnuncio.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/anuncios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/rechazarCampana.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/campanas/{campanaId}/rechazo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/registrarClic.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/impresiones/{impresionId}/clic (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/registrarConversion.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/conversiones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/subirPiezaCreativa.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/piezas-creativas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/verificarSocioComercial.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/socios-comerciales/{socioId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/aceptarCotizacion.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/cotizaciones/{cotizacionId}/aceptacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/anotarCobroDeComision.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/devengos/{devengoId}/cobros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/cerrarLiquidacion.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/liquidaciones/{periodo}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/consultarTarifarioVigente.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /tarifas/vigentes/{codigo} (ok, vacio, adverso) |
| packages/simulado/ejemplos/tarifas/cotizarComision.json | 137 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/cotizaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/crearSegmento.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/segmentos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/devengarComision.json | 78 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/devengos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/devolverComision.json | 78 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/{devengoId}/devoluciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/emitirFactura.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /facturas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/listarCostosDeProveedor.json | 45 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /tarifas/costos-de-proveedor (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/tarifas/listarTarifarios.json | 103 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /tarifas/tarifarios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/tarifas/ponerTarifarioVigente.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/tarifarios/{tarifarioId}/vigencia (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/publicarTarifario.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/tarifarios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/resolverPrecio.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/precios/resolver (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/cerrarAlertaRiesgo.json | 43 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/riesgo/alertas/{alertaId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/compensarEventoReputacion.json | 63 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/eventos/{eventoId}/compensacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/consultarEtapasDePersonas.json | 51 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/etapas/de-personas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/transparencia/consultarPerfilEnGrupo.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/{usuarioId}/perfil-en-grupo/{grupoId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/transparencia/consultarPuntaje.json | 32 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/{usuarioId}/puntaje (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/emitirCertificado.json | 75 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/certificados (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/evaluarInsignias.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/insignias/evaluacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/evaluarRiesgo.json | 114 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/riesgo/evaluacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/listarBloques.json | 40 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publico/grupos/{grupoId}/bloques (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/listarEtapas.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/etapas (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/moderarResena.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/resenas/{resenaId}/moderacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/publicarResena.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/resenas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/recalcularPuntaje.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/puntajes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/registrarEventoReputacion.json | 63 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/eventos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/revocarCertificado.json | 43 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/certificados/{codigo}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/revocarInsignia.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/insignias/{otorgadaId}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/sellarBloque.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/bloques (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/tomarSnapshotReputacion.json | 51 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/snapshots (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/verificarCadena.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publico/grupos/{grupoId}/verificacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/transparencia/verificarCertificado.json | 35 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /verificar/{codigo} (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/verificarSorteo.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publico/sorteos/{sorteoId}/verificacion (ok, vacio, adverso, rechazo) |
| packages/simulado/eslint.config.js | 8 | sí | Config ESLint |
| packages/simulado/package.json | 42 | sí | Manifiesto del mock: scripts contratos→ejemplos→inyectar→servidor Scalar en :4010 |
| packages/simulado/pruebas/catalogo-de-errores.spec.ts | 61 | sí | Rechazos usan códigos del catálogo |
| packages/simulado/pruebas/ejemplos-contra-esquema.spec.ts | 56 | sí | Cada ejemplo valida contra su esquema (requiere generado/) |
| packages/simulado/pruebas/qr-interno-vs-interoperable.spec.ts | 100 | sí | QR solo INTERNO; textos AP-CU12-07..10 |
| packages/simulado/scripts/contratos-a-json.mjs | 49 | sí | Convierte servicios/*/openapi/*.yaml a generado/*.json |
| packages/simulado/scripts/generar-ejemplos.mjs | 123 | sí | Crea ejemplos/<svc>/<op>.json desde el esquema y realinea rechazos al catálogo |
| packages/simulado/scripts/inyectar-ejemplos.mjs | 88 | sí | Fusiona contratos + ejemplos en generado/prism/todos.yaml |
| packages/simulado/scripts/servidor-mock.mjs | 14 | sí | Levanta @scalar/mock-server en 0.0.0.0:4010 |
| packages/simulado/src/backoffice-sistemas/adaptador.spec.ts | 31 | sí | Prueba que el adaptador resuelve |
| packages/simulado/src/backoffice-sistemas/adaptador.ts | 43 | sí | Adaptador simulado (retardo 30 ms) de 9 puertos |
| packages/simulado/src/backoffice-sistemas/datos.ts | 80 | sí | Arrays sintéticos (servicios, despliegues, accesos, etc.) |
| packages/simulado/src/backoffice-sistemas/index.ts | 35 | sí | Barril de datos sintéticos del backoffice/sistemas |
| packages/simulado/src/backoffice-sistemas/tipos.ts | 57 | sí | Tipos sintéticos de los 9 puertos |
| packages/simulado/src/catalogo.ts | 135 | sí | Construye catálogo de códigos AP-CUnn-nn desde YAML y docs |
| packages/simulado/src/ejemplos.ts | 47 | sí | ejemploDe(): lee el JSON de un escenario (node:fs) |
| packages/simulado/src/escenarios.ts | 19 | sí | Lista de escenarios y cabecera Prefer |
| packages/simulado/src/index.ts | 5 | sí | Barril del paquete |
| packages/simulado/src/muestra.ts | 162 | sí | Genera valor de muestra determinista desde un esquema |
| packages/simulado/src/randexp.d.ts | 10 | sí | Tipos mínimos de randexp |
| packages/simulado/src/referencias.ts | 38 | sí | Resuelve $ref locales |
| packages/simulado/src/tipos.ts | 48 | sí | Subconjunto de tipos OpenAPI |
| packages/simulado/tsconfig.json | 6 | sí | Config TS |
| packages/simulado/vitest.config.ts | 4 | sí | Config vitest (pruebas/ y src/**/*.spec.ts) |
| packages/tokens/.gitignore | 2 | sí | Ignora generado/ |
| packages/tokens/dinero/formatear.ts | 93 | sí | formatearMonto por cadenas (sin number) |
| packages/tokens/eslint.config.js | 8 | sí | Config ESLint |
| packages/tokens/package.json | 28 | sí | Manifiesto tokens: build a-css/a-dart/vectores |
| packages/tokens/pruebas/formatear-monto.spec.ts | 27 | sí | Pruebas del formato + propiedad sobre vectores |
| packages/tokens/pruebas/generados-coinciden.spec.ts | 46 | sí | CSS y Dart exponen mismos roles |
| packages/tokens/pruebas/roles-contraste.test.mjs | 127 | sí | Contraste WCAG por rol (node:test, NO lo corre vitest) |
| packages/tokens/pruebas/texto-de-ejemplo-contraste.test.mjs | 30 | sí | Contraste de placeholder (node:test, NO lo corre vitest) |
| packages/tokens/pruebas/tokens-contra-boveda.spec.ts | 143 | sí | tokens.json = estilos.css de la bóveda |
| packages/tokens/scripts/a-css.mjs | 83 | sí | Genera generado/tokens.css |
| packages/tokens/scripts/a-dart.mjs | 145 | sí | Genera generado/tokens.dart |
| packages/tokens/scripts/comun.mjs | 28 | sí | Lectura de tokens y helpers compartidos |
| packages/tokens/scripts/vectores.mjs | 46 | sí | Genera vectores/monto.json con LCG determinista |
| packages/tokens/tokens.json | 367 | sí | Fuente única de tokens de diseño |
| packages/tokens/tsconfig.json | 6 | sí | Config TS |
| packages/tokens/vectores/monto.json | 1 | no: 366 KB; verificado programáticamente (5006 casos, todos string, monedas BOB/USD, 2 decimales) | 5006 vectores de formato (366 KB, generado y versionado) |
| packages/tokens/vitest.config.ts | 2 | sí | Config vitest (solo pruebas/**/*.spec.ts) |
