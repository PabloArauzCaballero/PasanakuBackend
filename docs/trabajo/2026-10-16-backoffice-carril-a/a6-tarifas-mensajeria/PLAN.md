# Plan — A6 · tarifario y mensajería para el backoffice (carril A, subagente)

- Fecha: 2026-10-16 · Repos afectados: monorepo (servicios `tarifas` y `notificaciones`) · Predecesor: `docs/trabajo/2026-10-15-backoffice-financiero/PLAN.md` §A6 (segunda viñeta)
- Rama: `codex/backoffice-a6-tarifas-mensajeria` desde `origin/codex/backoffice-dominio-20261015` (`70747767`)
- Resultado observable: quien tiene `TARIFARIO_PUBLICAR` o `AUDITORIA_LEER` ve en el backoffice los tarifarios con sus versiones, vigencia, preaviso y última simulación; quien tiene `AUDITORIA_LEER` ve las plantillas de mensaje con sus versiones y los intentos de envío con su estado de entrega, sin contenido ni destinatario.
- Kill-test: `GET /tarifas/tarifarios`, `GET /notificaciones/plantillas` o `GET /notificaciones/envios` devuelven 404, o la respuesta de envíos contiene `destinatario`/`contenido`.

## Descubrimiento (hechos con ruta)

- Rutas existentes: `servicios/tarifas/src/main/resources/openapi/tarifas.yaml` ya expone `POST /tarifas/tarifarios` (publicar, CU-34), `POST /tarifas/tarifarios/{id}/vigencia` (poner vigente, CU-34), `GET /tarifas/vigentes/{codigo}`, `POST /tarifas/precios/resolver` (CU-36). **No hay listado** de tarifarios ni de versiones.
- `servicios/notificaciones/.../notificaciones.yaml`: `POST /notificaciones`, webhook, supresión. **No hay** listado de plantillas ni de envíos.
- La UI (`apps/backoffice/src/app/rutas/**`) no llama hoy a `/tarifas`, `/comisiones` ni `/notificaciones` (grep sin coincidencias): no hay contrato de UI previo que respetar.
- Tablas: `catalogo.tarifario` (estado `ARCHIVADO|BORRADOR|EN_PREAVISO|SUSTITUIDO|VIGENTE`, `moneda_base`, `vigente_desde/hasta`, `dias_preaviso`), `tarifas.cambio_tarifario` (UQ `tarifario_nuevo_id`), `tarifas.simulacion_tarifa` (`delta_ingreso_estimado NUMERIC(16,2)` **sin moneda**); `notificaciones.plantilla_mensaje`, `version_plantilla` (`cuerpo`, `asunto`: contenido), `envio_notificacion` (`destinatario`, `contenido_enviado`: datos personales/contenido; `costo NUMERIC(10,4)` + `moneda`), `evento_entrega_mensaje` (`payload_crudo`).
- Permisos sembrados (`sql/60_semillas/10-roles-y-permisos.sql`): `TARIFARIO_PUBLICAR`, `AUDITORIA_LEER`. No existe permiso de mensajería.
- Patrón a copiar: `servicios/entregas/.../web/DesembolsosController.java` (`@Permiso` + `@PermisoAlternativo`), `aplicacion/ConsultarDesembolsos.java`, `infraestructura/BandejaDeDesembolsosRepositorio.java`, tests `CU28DesembolsosTest`, `web/DesembolsosBandejaWebTest`.
- Códigos existentes: CU-34 01..04, CU-80 01..04, CU-83 01..05 → nuevos: `AP-CU34-05`, `AP-CU80-05`, `AP-CU83-06` (`FILTRO_DE_LISTADO_INVALIDO`).

## Alcance

- IN: tres listados de lectura con cursor keyset, sus contratos OpenAPI, tests de integración (base real) y web, fichas CU-34/80/83, postman de ambos servicios.
- OUT: simulación de impacto como operación propia, edición de plantillas, enrutamiento, consentimiento, cualquier cambio de esquema, `sql/`, `plataforma/`, otros servicios.
- Ambigüedades registradas:
  1. *Moneda de la simulación*: `simulacion_tarifa.delta_ingreso_estimado` no tiene moneda. Supuesto: viaja como cadena **sin moneda** y declarado; no se infiere de `moneda_base`. Confirmar con: dueño del modelo de tarifas (bóveda).
  2. *Permiso de mensajería*: no hay uno sembrado. Supuesto: solo `AUDITORIA_LEER` (auditoría operativa). Confirmar con: dueño del proyecto / seguridad.
  3. *Filtro por omisión*: no hay definición de qué estados son "excepción" en tarifarios, plantillas o envíos. Supuesto: sin filtro devuelve **todos**. Confirmar con: operación.

## H1 — Tarifario listable para el backoffice (CU-34)
**CA:** Dado un tarifario publicado y su anterior, cuando Plataforma o Auditoría abren el tarifario, entonces ven ambas versiones con estado, vigencia, preaviso y última simulación, de la más reciente a la más antigua.
**DoD:** `./gradlew :servicios:tarifas:spotlessCheck :servicios:tarifas:test :servicios:tarifas:webTest :servicios:tarifas:integrationTest :servicios:tarifas:testBarrido :servicios:tarifas:contractTest --no-build-cache --continue` en verde · `verificar_criterios.py` y `verificar_seguridad.py` en verde.
**Estado:** HECHO

### H1.S1 — Contrato, lectura y página
**CA:** `GET /tarifas/tarifarios` pagina por `(vigenteDesde, id)` DESC, filtra por `codigo` y `estados`, exige `TARIFARIO_PUBLICAR` o `AUDITORIA_LEER`.
**DoD:** compila; tests de H1.S2 en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato `listarTarifarios` + `AP-CU34-05` en yaml | el generador produce `TarifasApi.listarTarifarios` | `./gradlew :servicios:tarifas:compileJava --no-build-cache` → BUILD SUCCESSFUL | HECHO |
| H1.S1.M2 | `HistorialDeTarifariosRepositorio` + `ConsultarTarifarios` + método en `TarifasController` | compila con spotless aplicado | `./gradlew :servicios:tarifas:spotlessApply :servicios:tarifas:compileJava --no-build-cache` → BUILD SUCCESSFUL | HECHO |

### H1.S2 — Pruebas en tres niveles
**CA:** correcto (ve las dos versiones con cambio y simulación), límite (recorre de a una hasta cerrar sin cursor), inválido (estado desconocido/cursor a medias → 422 `AP-CU34-05`; 403/401/400).
**DoD:** `CU34TarifariosTest` (integración) y `TarifariosListadoWebTest` en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `CU34TarifariosTest` contra base real, escenarios en la ficha CU-34 | 0 failures | `./gradlew :servicios:tarifas:integrationTest --tests '*CU34TarifariosTest' --no-build-cache` | HECHO |
| H1.S2.M2 | `TarifariosListadoWebTest` 200/200 alt/403/401/422/400 + mocks en web tests existentes | 0 failures | `./gradlew :servicios:tarifas:webTest --no-build-cache` | HECHO |
| H1.S2.M3 | Ficha CU-34: código, tabla, párrafo y escenarios | `verificar_criterios.py` sin fallas de CU-34 | `python3 scripts/verificar_criterios.py --servicio tarifas` → exit 0 | HECHO |

## H2 — Mensajería auditable sin datos personales (CU-80 / CU-83)
**CA:** Dado un envío fallido y uno entregado, cuando Auditoría abre los intentos de envío, entonces ve estado, intentos, último evento del proveedor y costo con moneda, sin destinatario ni contenido; y ve las plantillas con su versión vigente sin cuerpo ni asunto.
**DoD:** regresión de `notificaciones` (mismo comando que H1 con `${s}=notificaciones`) en verde · gates en verde.
**Estado:** HECHO

### H2.S1 — Contrato, lectura y página
**CA:** `GET /notificaciones/plantillas` (cursor `(vigenteDesde, versionId)` DESC) y `GET /notificaciones/envios` (cursor `(encoladoEn, id)` DESC), ambos con `AUDITORIA_LEER`.
**DoD:** compila; tests de H2.S2 en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Contrato `listarPlantillas`, `listarEnvios`, `AP-CU80-05`, `AP-CU83-06` | el generador produce ambos métodos | `./gradlew :servicios:notificaciones:compileJava --no-build-cache` → BUILD SUCCESSFUL | HECHO |
| H2.S1.M2 | Repositorios + `ConsultarMensajeria` + métodos del controlador | compila con spotless aplicado | `./gradlew :servicios:notificaciones:spotlessApply :servicios:notificaciones:compileJava --no-build-cache` | HECHO |

### H2.S2 — Pruebas en tres niveles
**CA:** correcto, límite (cursor hasta página vacía), inválido (422/403/401/400); la respuesta no contiene `destinatario`, `contenido`, `cuerpo`, `asunto`.
**DoD:** `CU83EnviosTest`, `CU80PlantillasTest`, `MensajeriaAuditoriaWebTest` en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H2.S2.M1 | `CU83EnviosTest` (envíos despachados por CU-83 real) | 0 failures | `./gradlew :servicios:notificaciones:integrationTest --tests '*CU83EnviosTest' --no-build-cache` | HECHO |
| H2.S2.M2 | `CU80PlantillasTest` | 0 failures | `./gradlew :servicios:notificaciones:integrationTest --tests '*CU80PlantillasTest' --no-build-cache` | HECHO |
| H2.S2.M3 | `MensajeriaAuditoriaWebTest` + mocks en web tests existentes | 0 failures | `./gradlew :servicios:notificaciones:webTest --no-build-cache` | HECHO |
| H2.S2.M4 | Fichas CU-80 y CU-83 | `verificar_criterios.py` sin fallas | `python3 scripts/verificar_criterios.py --servicio notificaciones` → exit 0 | HECHO |

## H3 — Cierre
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H3.M1 | Regresión `tarifas` | 0 failures en los XML | comando de regresión por servicio | HECHO |
| H3.M2 | Regresión `notificaciones` | 0 failures en los XML | comando de regresión por servicio | HECHO |
| H3.M3 | Gates `verificar_criterios`, `verificar_boveda`, `verificar_seguridad` | exit 0 | `python3 scripts/...` | HECHO |
| H3.M4 | Postman regenerado (solo tarifas/notificaciones) | diff limitado a esos archivos | `python3 scripts/generar_postman.py && git status --short postman` | HECHO |
| H3.M5 | Commit + push (el REPORTE.md no se pudo escribir desde el subagente: el arnés lo bloquea; su contenido va en la entrega al agente principal) | rama en origin | `git push -u origin codex/backoffice-a6-tarifas-mensajeria` | HECHO |

## DECISION_REQUIRED (no se implementa)
- **Simulación de impacto como operación propia**: la ficha CU-34 pide "simulación obligatoria antes de aprobar" (`simularSobreHistoria`), pero no existe la clase ni el contrato; el controlador manda `escenario`/`resultado` como `"{}"`. Implementarla es regla de negocio nueva (qué historia, qué ventana). Confirmar con: negocio/tarifas.
- **Enrutamiento, consentimiento y edición de plantillas** (resto de la viñeta A6): exigen casos de uso de escritura que no existen.

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Otros agentes corren Testcontainers en paralelo | fallos de entorno espurios | re-correr en serie antes de clasificar |
| `catalogo.tarifario` no se limpia entre pruebas | conteos contaminados | filtrar siempre por un `codigo` único por prueba |
| Controlador de notificaciones crece | `testBarrido` (≤300 líneas) | mapeo a una clase `MapeoDeMensajeria` en `web` |
