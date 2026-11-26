# Plan — GU-C · Invitación, credencial y recuperación asistida del operador

- Fecha: 2026-11-25 · Repos afectados: PasanakuBackend (monorepo) · Rama: `claude/gu-c-credencial` (desde `dev` 9cd4d2c4) · PR a `dev`
- Predecesor: INVENTARIO del trabajo «Gobierno de usuarios» (rama `claude/modest-newton-4ckcid`, `docs/trabajo/2026-11-24-gobierno-de-usuarios/`), decisiones P-1, P-4 y P-5 de Pablo; ADR-038; CU-04 y CU-09.
- Resultado observable: (1) a un operador dado de alta le llega una invitación de un solo uso y con vencimiento a su correo institucional; con ella elige su clave y enrola su TOTP y puede entrar (ya no existe clave provisional); (2) un operador autenticado cambia su clave con `POST /cuenta/clave` (CU-09); (3) un operador que perdió su clave o su teléfono pide la recuperación, **otra** persona con `SEGURIDAD_ACCESO_RESTABLECER` la aprueba, pasa el enfriamiento y recién entonces recibe una invitación de un solo uso; (4) cada paso deja bitácora (`identidad.usuario_interno_*`) y pedido de aviso al afectado y al oficial de cumplimiento.
- Kill-test: un token de invitación usado dos veces, o vencido, o de otro propósito, abre la cuenta → no está hecho. Aprobar la propia solicitud funciona → no está hecho. El valor plano del token aparece en la base o en un log → no está hecho.

## Alcance

- IN: GU-02, GU-03, GU-04 y la fecha de nacimiento inventada del alta (GU-17, solo ese punto).
  - `docs/entidades/01_identidad_usuarios.puml` (+ DDL generado, semilla de política de token por `seeders/minimos/13-politicas-de-token.json`).
  - `servicios/identidad`: clases **nuevas** (`CuentaController`/`RutasDeCuenta`, `Invitaciones…`, `Recuperaciones…`, `trabajos/EmisionDeRecuperaciones`, puerto de correo de invitación) y cambios mínimos en `CU04Autenticar`/`SesionController` solo si hace falta; `identidad.yaml` solo con operaciones y schemas **nuevos** (tag `Cuenta`).
  - `apps/backoffice`: `rutas/acceso/*` (invitación, recuperar, cambiar clave, aprobaciones), enlace en `rutas/ingreso`.
  - `docs/CasosDeUso/CU-04` y `CU-09`; `postman/`, inventario de endpoints.
- OUT: lo de A, B, E, G (segregación, asignación CU-08, revocación, CU-134); el menú «Personal y accesos» (lote D); roles y cuentas de servicio (lote F). El alta (`GestionarUsuariosInternos.crear`) se toca solo para emitir la invitación (cambio mínimo, coordinado con B).
- Ambigüedades registradas (a confirmar con Pablo):
  - **A1 · plazo de enfriamiento.** ADR-038 y P-4 piden «enfriamiento» sin plazo. Supuesto: 24 h, configurable (`aportaya.acceso.recuperacion.enfriamiento`). No se infiere como parámetro de negocio: queda como parámetro con valor inicial a confirmar.
  - **A2 · vigencia de la invitación.** P-1 dice «con vencimiento». Supuesto: 72 h (política `INVITACION_OPERADOR`), una sola emisión viva por operador; reemitir invalida la anterior.
  - **A3 · quién pide la recuperación.** Supuesto: el propio operador lo pide sin sesión (por correo institucional) o una persona de seguridad en su nombre; el pedido no revela si el correo existe. La **verificación asistida** (prueba de vida y documento, ADR-038 §4.2) es un paso humano fuera de este lote: se registra en el motivo de la aprobación.
  - **A4 · enfriamiento posterior a reinscribir el factor** (ADR-038 §5: tras reenrolar, la cuenta no ejecuta `requiere_mfa` por un plazo). Supuesto: fuera de este lote (necesita cambio en la validación de step-up); se declara **No cubierto**.
  - **A5 · aviso por correo.** B emite `identidad.aviso_de_acceso_solicitado` (solo ids) y todavía no hay consumidor en `notificaciones`. Supuesto: este lote emite el mismo pedido y envía directamente solo la invitación; los demás avisos dependen de ese consumidor.

## H1 — Contrato y esquema

**CA:** Dado el modelo, cuando se regenera el DDL, entonces existen la solicitud de recuperación con aprobador distinto del titular (impuesto por la base), los propósitos de token nuevos y su política, sin deriva ni otro diff.
**DoD:** `python3 scripts/generar_ddl.py` sin diff posterior · `openapi-generator validate` sin problemas · verificadores en 0.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Tabla `solicitud_recuperacion_acceso` en el `.puml` (CHECK aprobador ≠ titular, estados, UNIQUE de clave de idempotencia, índice nombrado) | `generar_ddl.py` produce la tabla y los índices | `python3 scripts/generar_ddl.py && git diff --stat sql/` solo muestra la tabla | HECHO |
| H1.S1.M2 | Propósitos `INVITACION_OPERADOR` y `REINSCRIPCION_FACTOR` en el enum del `.puml` y política en el seeder JSON | el SQL de semilla los trae con TTL | `python3 scripts/generar_semillas.py` (o el generador que corresponda) sin diff extra | HECHO |
| H1.S2.M1 | Operaciones `/cuenta/*` y schemas nuevos en `identidad.yaml` (tag `Cuenta`) sin tocar lo ajeno | el validador no reporta problemas | `openapi-generator validate -i servicios/identidad/src/main/resources/openapi/identidad.yaml` | A MEDIAS |
| H1.S2.M2 | CU-04 y CU-09 al día (flujo de invitación, recuperación a cuatro ojos, códigos) | `verificar_criterios` y `verificar_boveda` en 0 | `python3 scripts/verificar_criterios.py; python3 scripts/verificar_boveda.py` | HECHO |

## H2 — Invitación del operador (GU-02, P-1)

**CA:** Dado un operador recién dado de alta, cuando abre el enlace de su invitación vigente y completa clave + código TOTP válido, entonces su cuenta queda con clave propia, factor confirmado y la invitación consumida; reusar, vencer o falsear el token falla con el mismo error genérico.
**DoD:** pruebas dirigidas de identidad en verde en CI (dobles de correo en tres niveles) · `verificar_seguridad` 0.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Alta sin clave provisional ni fecha inventada: la credencial nace inutilizable y el alta emite la invitación | el alta no devuelve ni persiste una clave usable | test `AltaDeOperadorInvitacionTest` (CI) | A MEDIAS |
| H2.S1.M2 | `InvitacionesDeCuenta` (emitir: solo hash con pepper, propósito único, vencimiento, invalida la anterior) | el valor plano no está en la tabla; la anterior queda `INVALIDADO` | test del repositorio contra base real (CI) | A MEDIAS |
| H2.S1.M3 | Puerto `CorreoDeInvitacion` + impl local y Gmail; el enlace lleva el token en el fragmento | local registra sin token; Gmail arma el mensaje | test de plantilla y del doble (correcto/límite/inválido) | A MEDIAS |
| H2.S2.M1 | `POST /cuenta/invitacion/consultar` y `/factor` (público, con límite de tasa) | token válido → alcance y semilla una vez; inválido → `AP-CU09-04` genérico | `CuentaInvitacionWebTest` (CI) | A MEDIAS |
| H2.S2.M2 | `POST /cuenta/invitacion/completar` (atómico: clave + factor + consumo) | segunda llamada con el mismo token → rechazo; clave débil → `AP-CU09-02` sin consumir el token | test de idempotencia y de carrera (CI) | A MEDIAS |

## H3 — Cambio de clave (GU-03, CU-09)

**CA:** Dado un operador autenticado, cuando manda su clave actual y una nueva válida, entonces cambia, el historial la conserva, se revocan todas sus sesiones y puede volver a entrar solo con la nueva; con la actual mala recibe `AP-CU09-01`.
**DoD:** `CuentaClaveWebTest` y `CU09CambiarCredencialTest` en verde (CI).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | `POST /cuenta/clave` sobre `CU09CambiarCredencial` (operador: corta todas las sesiones, R-SEG-11) | 200 con sesiones cerradas; sin token → 401; clave actual mala → 401 `AP-CU09-01` | test web (CI) | A MEDIAS |
| H3.S1.M2 | Bandera `requiereCambio` coherente: se limpia al cambiar y la invitación la deja en falso | tras cambiar, `requiere_cambio=false` | consulta de verificación en el test | A MEDIAS |

## H4 — Recuperación asistida a cuatro ojos (GU-04, P-4, ADR-038)

**CA:** Dado un operador sin clave o sin teléfono, cuando pide la recuperación, otra persona con `SEGURIDAD_ACCESO_RESTABLECER` la aprueba y vence el enfriamiento, entonces recibe una invitación de un solo uso que restablece su clave o reinscribe su factor; el titular no puede aprobar la suya (lo impone la base) y todo cambio revoca sesiones y confianza de dispositivos.
**DoD:** pruebas dirigidas en CI · prueba de autoaprobación contra base real · bitácora y aviso emitidos.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `POST /cuenta/recuperaciones` (público, 202 siempre, sin enumerar; idempotente por solicitud viva) | misma respuesta exista o no el correo; segunda solicitud viva no duplica | test web (CI) | A MEDIAS |
| H4.S1.M2 | `GET /cuenta/recuperaciones` + `POST …/aprobacion` y `…/rechazo` (permiso, motivo, ≠ titular) | autoaprobación → `AP-CU09-09` y la base la rechaza aunque se evite la app | test con base real (CI) | A MEDIAS |
| H4.S1.M3 | Trabajo `EmisionDeRecuperaciones` (SKIP LOCKED, idempotente, solo tras el enfriamiento) | antes del plazo no emite; dos corridas emiten una vez | test del trabajo con reloj fijo (CI) | A MEDIAS |
| H4.S2.M1 | `completar` para alcance CLAVE y FACTOR (FACTOR exige la clave actual; no reactiva el otro secreto) | cada alcance cambia solo lo suyo y corta sesiones y confianza | test web (CI) | A MEDIAS |

## H5 — Bitácora y avisos (P-5)

**CA:** Dado cualquier acto de H2–H4, entonces se escribe en la misma transacción un evento `identidad.usuario_interno_*` con `version, accion, actorId, actorRol, origen, afectadoId, motivo, ocurridoEn` y el pedido `identidad.aviso_de_acceso_solicitado` al afectado y a los oficiales de cumplimiento, sin datos personales en la carga.
**DoD:** test de contrato de las cargas (CI) · `verificar_seguridad` 0.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | `BitacoraDeCuenta` (eventos y aviso por outbox) | la carga trae solo ids y los campos de la forma v1 | `BitacoraDeCuentaTest` (CI) | A MEDIAS |

## H6 — Backoffice (regla 95)

**CA:** Dado el backoffice, entonces existen las pantallas de invitación (clave + enrolamiento TOTP con QR), recuperar acceso, cambiar mi clave y aprobaciones de recuperación, cada una con cargando/datos/vacío/error, errores por campo, sin doble envío, foco visible, tema claro y oscuro, AA.
**DoD:** specs de componente en verde · capturas 390/834/1440 × claro/oscuro con `animations: 'disabled'`, inspeccionadas · consola y red sin errores nuevos.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Cliente tipado de `/cuenta/*` + simulado con ejemplos curados | el simulado responde los tres niveles | spec del cliente | A MEDIAS |
| H6.S1.M2 | Pantalla de invitación (token del fragmento, clave, QR TOTP, código) | errores por campo; sin doble envío; token fuera de la URL tras leerlo | spec + capturas | A MEDIAS |
| H6.S1.M3 | Pantalla «No puedo entrar» + enlace desde el ingreso | respuesta neutra siempre | spec + capturas | A MEDIAS |
| H6.S1.M4 | Pantalla «Cambiar mi clave» | error por campo, sesión cerrada tras cambiar | spec + capturas | A MEDIAS |
| H6.S1.M5 | Pantalla de aprobaciones de recuperación | lista paginada con 4 estados, aprobar/rechazar con motivo | spec + capturas | A MEDIAS |

## H7 — Gates y entrega

**CA:** Dado el PR, entonces CI verde y los verificadores en 0.
**DoD:** `verificar_criterios`, `verificar_boveda`, `verificar_seguridad`, `verificar_carriles`, `verificar_contratos_limites`, `inventario_endpoints --check`, `generar_ddl`/`generar_postman` sin diff, `openapi validate`; CI `ci.yml` verde.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S1.M1 | Verificadores locales en 0 | todos 0 | salidas en `evidencia/verificadores.txt` | HECHO |
| H7.S1.M2 | PR a `dev` con CI verde y aviso a la sesión principal | CI verde | check-runs | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Java no compila localmente (429 de Maven Central) | Errores de compilación solo visibles en CI | Código por imitación de lo existente; un commit por pieza; CI como juez |
| `identidad.yaml`, `.puml` y `GestionarUsuariosInternos` compartidos con B | Conflictos de merge | Solo agregados nuevos; merge de `dev` seguido; regenerar DDL/postman tras cada merge |
| B todavía no está en `dev` (`BitacoraDeAccesos`, step-up) | No reusar su helper | Doble propio con la misma forma v1 (regla 65); se unifica cuando B entre |
