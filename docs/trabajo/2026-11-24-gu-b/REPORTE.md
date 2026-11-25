# Reporte — GU lote B: CU-08 por asignación en `identidad`

> **AVANCE: 20 / 20 — 100,0 %.** PR #516 mergeado en `dev` (90774d90) con la CI en verde.

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-b-cu08` · PR: #516 contra `dev`
- Peldaño de evidencia alcanzado: `TESTED` en CI (unitarias, web y de integración contra PostgreSQL real en verde) + esquema aplicado en PostgreSQL 16. No `VERIFIED`: no se ejercitó contra un despliegue con backoffice y auditoría reales.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | 4 operaciones nuevas (`asignarRolDeOperador`, `revocarAsignacionDeRol`, `darDeBajaUsuarioInterno`, `informeDeAccesos`) con esquemas estrictos | `openapi-spec-validator` y `openapi-generator-cli 7.14.0 validate` | PASS — «No validation issues detected» (`evidencia/verificadores.txt`) |
| H1.S1.M2 | `evidenciaMfa` (y `motivo` en alta) opcionales; `PUT …/roles` `deprecated: true`; sin campos removidos | `git diff` del YAML | PASS — diff solo aditivo |
| H1.S1.M3 | Postman regenerado, inventario alineado, 4 ejemplos del simulado curados y validados contra su esquema | `generar_postman.py`, `inventario_endpoints.py --check`, validación JSON Schema 2020 de los ejemplos | PASS (`evidencia/verificadores.txt`) |
| H1.S4.M3 | Forma v1 del evento de bitácora y del pedido de aviso documentada en CU-08 § «Contrato de eventos» | `verificar_boveda.py` | PASS — EXIT=0 |
| H1.S7.M1 | CU-08 actualizado: rutas reales, errores `AP-CU08-09..11` (+`08` reservado a A), baja/suspensión, 15 escenarios gherkin nuevos trazados a pruebas | `verificar_criterios.py` | PASS — «Sin divergencias» |
| H1.S7.M2 | Verificadores locales del encargo en 0 | ver `evidencia/verificadores.txt` | PASS — 10/10 EXIT=0 |
| H1.S2.M1–M3, H1.S3.M1–M4, H1.S4.M1–M2, H1.S5.M1–M2, H1.S6.M1–M2, H1.S7.M3 | Step-up con `jti` en tabla, nadie sobre sí mismo, nunca sin administrador; asignar/revocar idempotentes; PUT por diferencia; bitácora y aviso con actor/origen; baja en `BAJA_LABORAL` y suspensión que revoca; vencimiento diario e informe; PR mergeado | CI de 53710c43: `./gradlew integrationTest` (identidad: CU08GobiernoTest, CU08BajaYVencimientoTest, EvidenciaStepUpConsumidaRepositorioTest, RutasDeAccesos*WebTest) | PASS — run 45513084333, job «Los cinco corredores» success |

## A medias
Ninguna. Las 14 microtareas que esperaban la CI pasaron a HECHO con esta evidencia: CI run 45513084333 (53710c43): «Los cinco corredores» y «Formato, reglas propias y compilacion» success; PR #516 mergeado en dev (90774d90). Fallas de CI encontradas y corregidas en el camino (todas TEST_BUG o de gate, ninguna de producto): literal `"CORREO"` en el barrido de salidas sensibles; fechas como texto en SQL plana; otorgante cacheado de un escenario deshecho; asignación sintética otorgada en el futuro; prueba de humo esperando 52 tablas selladas (ahora 53).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Consumidor de `identidad.aviso_de_acceso_solicitado` en `notificaciones` (+ `evento_notificable` `ACCESO_DE_OPERADOR_CAMBIADO` y plantilla) | TODO | Asignar dueño (sesión principal / Pablo) |
| Migrar el backoffice a las rutas nuevas y enviar `evidenciaMfa` en alta, suspensión/reactivación y desbloqueo | TODO | Lote D. Hasta entonces esos botones responden 422 `AP-CU08-09`: no promover a `test` antes que D |
| Retirar `PUT /usuarios-internos/{id}/roles` (deprecado, sin step-up) | TODO | Que ningún cliente lo llame (lote D) |
| Regla de `AP-CU08-12` (FILTRO_DE_LISTADO_INVALIDO) en CU-08: hoy es solo una fila de la tabla de errores, sin flujo alternativo ni criterio gherkin | TODO | Lote A o la sesión principal: redactar el flujo alternativo y su escenario |
| Ambigüedades sin confirmar (ver «Decisiones») | DECISION_REQUIRED | Pablo |

## Evidencia
```text
$ python3 scripts/verificar_criterios.py
casos de uso implementados y verificados: 107
Sin divergencias entre la boveda y el codigo.
EXIT=0
$ java -jar openapi-generator-cli-7.14.0.jar validate -i servicios/identidad/src/main/resources/openapi/identidad.yaml
No validation issues detected.
$ ./gradlew :servicios:identidad:generarServidorOpenApi
Could not GET 'https://repo.maven.apache.org/maven2/commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.pom'. Received status code 429 from server: Too Many Requests
```
Salida completa de los diez verificadores: `evidencia/verificadores.txt`.

## No cubierto
- Toda la capa Java fuera de `dominio`: sin compilar ni ejecutar localmente. Depende de la CI.
- El step-up contra el emisor real (`/sesiones/desafios` → evidencia → acto) de punta a punta: solo hay prueba unitaria del adaptador con decodificador doble y pruebas de integración con un doble de `EvidenciaDeStepUp` (regla 65, declarado en `BaseDeGobiernoCU08`).
- Que `auditoria` y `notificaciones` consuman los eventos (no existen los consumidores en este lote).
- Reemplazo de `jti` entre réplicas.
- Prueba visual: no aplica (sin UI en este lote).

## Desvíos del plan
- Rutas bajo `/usuarios-internos` en vez de `/accesos` (gateway).
- `AccesosController` → `RutasDeAccesos` abstracta (el generador ignora `tags`).
- Códigos `AP-CU08-09..11` por choque con `AP-CU08-08` del lote A.
- Las pruebas de integración corren en transacciones que se deshacen (la regla del último administrador cuenta toda la base compartida; además el lote A inserta `ACCESOS_ADMINISTRAR` sin `ON CONFLICT`).

## Riesgos residuales
- Orden de despliegue: B ya está en `dev`; el backoffice sin step-up falla en alta/suspensión/desbloqueo hasta el lote D.
- `PUT …/roles` deprecado sigue sin step-up mientras exista.
- `motivo_revocacion` se recorta a 120 y el de sesión a 80; el texto completo queda en el evento de bitácora.
- `FixturaDeIdentidad.otorgante()` cachea un id entre pruebas: cualquier prueba nueva que lo use dentro de una transacción que se deshace repite el fallo que se corrigió acá (deuda de la fixtura, no tocada por estar fuera de alcance).
- El simulado tiene deriva previa ajena (ejemplos sin generar de garantía, tarifas, aportes y TOTP) que no se incluyó.

## Decisiones y ambigüedades

### Decididas por Pablo (2026-11-24, vía sesión principal)
1. **Suspensión**: queda como se implementó — suspender revoca todos los roles y reactivar vuelve sin roles (se reasignan a cuatro ojos).
2. **Baja**: estado NUEVO `BAJA_LABORAL` (no `BAJA_VOLUNTARIA`), agregado por el modelo (`docs/entidades/01_identidad_usuarios.puml` → `generar_ddl.py`), con `CK_AMPLIADOS` para bases ya aplicadas. Evidencia: `evidencia/esquema-baja-laboral-y-step-up.txt`.
3. **Step-up**: el `jti` consumido va a la tabla `identidad.evidencia_step_up_consumida` (`UNIQUE (jti)`, `vence_en`, append-only), no a memoria. Doble uso probado en SQL real (2 INSERT → 1 fila) y en `EvidenciaStepUpConsumidaRepositorioTest`.
4. **Orden**: #516 entra a `dev` con CI verde; no se promueve a `test` hasta que el lote D esté en `dev`.

### Acordadas con los otros lotes
- Códigos: `AP-CU08-08` y `AP-CU08-12` del lote A; `09`–`11` de B. `RolesParaPersonalInterno.admite` conectado en `GobiernoDeAccesos.asignar`.
- Eventos para el consumidor de auditoría del lote E: tipos `identidad.(rol|usuario_interno)_*`, carga con `actorId`, `actorRol`, `origen` (`PANEL_ADMIN` / `TAREA_PROGRAMADA`) y `motivo`.

### Sin confirmar todavía
1. Ruta `/usuarios-internos/...` en lugar de `/accesos/...` (el gateway no rutea `/accesos`).
2. Revocar con `POST …/revocacion` en lugar de `DELETE` con cuerpo.
3. El ámbito de la asignación debe ser el del rol (`AP-CU08-02`).
4. Permiso de las rutas nuevas: `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_GESTIONAR`; «nunca sin administrador» cuenta solo `ACCESOS_ADMINISTRAR`.
5. Dueño del consumidor de `identidad.aviso_de_acceso_solicitado` en `notificaciones`.
