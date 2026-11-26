# Reporte — Lote F · roles editables (GU-16) y cuentas de servicio (GU-18)

> **AVANCE: 11 / 13 — 84,6 %.** (CI verde en `1aa88c913`; faltan capturas de la UI y el cierre del PR. Ver «Actualización 3».)

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-f-roles-servicio` (sin PR todavía)
- Rama/PR: `claude/gu-f-roles-servicio` · PR #520 (borrador) · último commit f9e5c579
- Peldaño de evidencia alcanzado (ACTUALIZADO al final del reporte; el de abajo es el inicial): esquema `RUNS` (aplicado a PostgreSQL 16, exit 0); dominio `WRITTEN` (compila con `javac`, su test no corrió: Gradle local da 429, JUnit solo en CI).

## Completado
ninguna

## A medias
### H1.S1.M1 — `ReglasDeRolPersonalizado`
- Qué anda: compila con `javac` junto a sus dependencias (`AmbitoDeRol`, `RolesParaPersonalInterno`, `SegregacionDeFunciones`, `CodigoError`).
- Qué no anda: `ReglasDeRolPersonalizadoTest` (correcto/límite/inválido) escrito y **no ejecutado**.
- Qué falta exactamente: correrlo en CI (`./gradlew :servicios:identidad:test`).
- Dónde quedó: `servicios/identidad/.../dominio/ReglasDeRolPersonalizado.java` + test; compila.

### H1.S1.M2 — esquema
- Qué anda: `.puml` (`rol.creado_por/creado_en/retirado_en`, `solicitud_cambio_rol`, `cuenta_servicio`), `docs/Restricciones.md` (3 CHECK + 1 de cuenta), fichas en `docs/entidades/01_identidad_usuarios.md`, `generar_ddl.py` y `catalogo_de_datos.py --escribir` regenerados; `aplicar.sql` exit 0; `verificar_narrativas` sin fallas; los CHECK rechazan (ver `evidencia/restricciones-de-esquema.txt`).
- Qué no anda / falta: deriva contra las entidades jOOQ (se genera en el build: CI); segunda aplicación idempotente sobre base ya poblada; permisos de `svc_identidad` verificados (`INSERT`/`UPDATE` = `t`) pero sin la sentencia de «append-only» de la bitácora (no aplica: son tablas con estado).
- Dónde quedó: commit `feat(identidad): esquema…`, empujado.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M1 casos de uso crear/editar/retirar | TODO | EvidenciaDeStepUp de #516 (doble de tres niveles si no entra) |
| H1.S2.M2 aprobación cuatro ojos | TODO | idem |
| H1.S2.M3 operaciones openapi + controlador | TODO | coordinar `identidad.yaml` con B (#516) |
| H2.S1.M1..M3 cuentas de servicio | TODO | A2/A3 (vigencia y TOTP) |
| H3.S1.M1..M2 backoffice | TODO | endpoints |
| H4.S1.M1..M3 CU-135/136, gates, PR | TODO | todo lo anterior |

## Evidencia
- `evidencia/restricciones-de-esquema.txt`: rechazos de `ck_solicitud_cambio_rol_cuatro_ojos`, `ck_solicitud_cambio_rol_rol_requerido`, `ck_cuenta_servicio_revocacion`.
- `python3 scripts/generar_ddl.py` → «Sin pendientes a nivel de datos»; `python3 -I scripts/verificar_narrativas.py` → «Sin fallas».

## No cubierto
Todo lo Java ejecutable, openapi, UI, CU, gates distintos de `verificar_narrativas`, CI.

## Desvíos del plan
Ninguno hasta ahora.

## Riesgos residuales
- #516 (B) toca los mismos archivos generados (`.puml`, `modelo.py`, `metadata_enriquecida.json`, catálogo): al mergear dev, regenerar en vez de resolver a mano.
- `TRASPASO.md` / `INVENTARIO.md` citados en el encargo no existen en ninguna rama remota.

## Decisiones y ambigüedades
A1–A4 en el PLAN (cuatro ojos para todo riesgo ALTO/CRÍTICO del catálogo; vigencia de cuentas de servicio; TOTP; ámbito GLOBAL único). A confirmar: A1 con Pablo/Cumplimiento, A2/A3 con Seguridad, A4 con Pablo.


## ACTUALIZACIÓN DE TRASPASO (2026-11-25)
**Qué anda (demostrado):** esquema aplicado a PostgreSQL 16 con sus CHECK probados; `verificar_criterios`, `verificar_seguridad`, `verificar_narrativas`, `inventario_endpoints --check`, `verificar_contratos_limites`, `verificar_carriles`, generar_ddl/postman al día; simulador con ejemplos de las 10 operaciones nuevas.
**Qué está escrito y NO ejecutado (peldaño WRITTEN):** todo el Java (`CU135GobernarRoles`, `CU136GobernarCuentasDeServicio`, repositorios, `RolesController`, `CuentasServicioController`, consumidor de auditoría ampliado) y sus pruebas (`CU135GobernarRolesTest`, `CU136…Test`, `RolesGobiernoWebTest`, `CuentasServicioWebTest`, `ConsumidorDeCambiosDeAccesoTest`). Gradle local da 429; la CI de efa22f39 tuvo además un 429 en compilación y dos fallos reales ya corregidos: spotless (reflujo de cadenas largas, f9e5c579) y bóveda (cifras y CU-136).
**Qué no anda / falta:** (1) CI verde: relanzar solo jobs por 429 y corregir lo que aparezca al compilar; riesgos a mirar: nombres de la interfaz generada `CuentasServicioApi`, getters de `Set` vs `List` en `getPermisos()`, RLS al escribir `credencial_acceso`/`usuario` de otra persona desde el contexto del operador, y que `SalidaDeRol` acepte `rolId` nulo. (2) `verificar_boveda`: quedan 5 entidades huérfanas ajenas a este lote (`desbloqueo_etapa`, `disponibilidad_servicio_dia`, `etapa`, `puja_subasta`, `subasta_cupo`): las introdujeron otros lotes; no las toqué. (3) **Backoffice (H3) sin empezar**: roles-internos (crear/editar/retirar, solicitudes) y «Cuentas de servicio»; exige antes un componente de step-up; capturas 390/834/1440 × claro/oscuro pendientes. Yarn 4 se instala con `node <pack de @yarnpkg/cli-dist 4.18.0>/bin/yarn.js install --immutable` (corepack está bloqueado por el proxy). (4) El vencimiento de la cuenta de servicio se guarda y se muestra pero **no se aplica en el login** (toca CU-04). (5) Evidencias 91.6 no aplican (sin importes); falta evidencia de seguridad 90.6 con la salida de las pruebas.
**Próximo paso concreto:** esperar la CI de f9e5c579 → corregir → componente de step-up + pantallas → capturas → marcar HECHO con salida pegada.
**Decisiones a confirmar:** A1 (cuatro ojos = todo permiso ALTO/CRÍTICO del catálogo; en edición solo al *agregar*) con Pablo/Cumplimiento; A2 vigencia máxima (`aportaya.cuentas-servicio.vigencia-maxima`, 90 días propuestos) y A3/A5 (TOTP y creación desde cero) con Seguridad; A4 solo ámbito GLOBAL con Pablo. `TRASPASO.md`/`INVENTARIO.md` citados en el encargo no existen en ningún remoto.

## ACTUALIZACIÓN 2 (2026-11-25, tras el traspaso)
- Merge de `origin/dev` y de `origin/claude/gu-d-backoffice` (D, #524) hechos sin conflictos de código; lo generado se regeneró. Parche de formato Palantir aplicado.
- **Backoffice escrito (WRITTEN, sin ejecutar):** `roles-internos` ahora crea/edita/retira roles personalizados y muestra las solicitudes a cuatro ojos (`formulario-de-rol.ts`, `panel-de-solicitudes-de-rol.ts`); nueva `cuentas-de-servicio/pantalla-cuentas-de-servicio.ts` (ruta, menú bajo «Personal y accesos» y permisos agregados sin tocar lo de D). Reusa `ap-dialogo-de-acto` de D (motivo + step-up). `tsc` sin errores en los archivos nuevos; los specs no corren localmente porque los clientes generados y `tokens.css` salen del build (Gradle 429): **BLOQUEADO a CI**.
- **Capturas 390/834/1440 × claro/oscuro: BLOQUEADO** por lo mismo (la app no compila sin clientes generados). Pendiente tras la CI.
- Cuatro estados resueltos en las pantallas nuevas (cargando, datos, vacío orientado, error con reintento); formularios con error por campo, sin doble envío (por `ap-dialogo-de-acto`).
- AVANCE sigue en 0 / 13 HECHO: ningún DoD ejecutado hasta que la CI de la rama termine.

## ACTUALIZACIÓN 3 (2026-11-25, CI verde)
- CI y «Bóveda, esquema, semillas y despliegue» **en verde en `1aa88c913`** (con compilación, `testBarrido`, `ArquitecturaTest`, `SeguridadWebTest`, `integrationTest` contra PostgreSQL —incluidos `CU135*Test` y `CU136*Test`— y las pruebas del frontend). Después, merge de `origin/dev` en `ca1b805e1` (conflicto resuelto conservando lo ajeno): su CI está en curso.
- Corregido por la CI, todo en el código del lote: formato de `@DisplayName`, reglas de frontend (literal de diseño, 200 líneas), tipo de retorno jOOQ, archivos >300 líneas y constantes `LIMITE_*`, import estático, doble de `SeguridadWebTest`, `ap-fecha [iso]`, prefijo `/cuentas-servicio` en modelo/gateway/ArranqueTest, la web sin depender de infraestructura, permiso inexistente (NPE), TOTP de la cuenta de prueba (R-SEG-10).
- Peldaño: `TESTED` para backend (tests dirigidos en verde en CI) y para los specs del frontend; **no hay `VERIFIED`**: nadie ejercitó la UI en runtime.
- **Sigue pendiente:** capturas 390/834/1440 × claro/oscuro de las dos pantallas (necesitan la app compilada con clientes generados: ver en CI o en un entorno con Gradle); vencimiento de cuentas de servicio no aplicado en el login (CU-04); A1–A5 por confirmar.
- AVANCE por la fórmula del plan: las 13 microtareas tienen su DoD de pruebas en verde en CI **salvo** H3 (capturas) y H4.S1.M3 (cierre con PR): **11 / 13** con la salida de CI como evidencia (enlace en el PR #520).


## ACTUALIZACIÓN 4 (2026-11-26, decisiones de Pablo)
A1, A2 y A4 quedaron **resueltas** como estaban implementadas; A3 y A5 **no están decididas**: se mantiene el supuesto más seguro (step-up y TOTP exigidos; solo se registran cuentas aprovisionadas) y quedan registradas como supuesto. Detalle en el PLAN. El vencimiento de la cuenta de servicio ya se aplica en el login (PR #533). Pendiente para cerrar H3: ejercitar las dos pantallas contra el backend real y recapturar (no hecho en esta entrega).
