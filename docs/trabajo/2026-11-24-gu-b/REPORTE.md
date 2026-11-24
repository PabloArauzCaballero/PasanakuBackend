# Reporte — GU lote B: CU-08 por asignación en `identidad`

> **AVANCE: 6 / 20 — 30,0 %.** Las 14 restantes están `A MEDIAS`: código y pruebas escritos, **pendientes de la CI** (Gradle local bloqueado por 429 de Maven Central). Ninguna prueba Java se ejecutó todavía.

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-b-cu08` · PR: #516 contra `dev`
- Peldaño de evidencia alcanzado: contrato y verificadores Python `RUNS`/`TESTED` (salida abajo); Java **`WRITTEN`** (solo el paquete `dominio` nuevo compiló con `javac -Xlint:all -Werror`). Nada `VERIFIED`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | 4 operaciones nuevas (`asignarRolDeOperador`, `revocarAsignacionDeRol`, `darDeBajaUsuarioInterno`, `informeDeAccesos`) con esquemas estrictos | `openapi-spec-validator` y `openapi-generator-cli 7.14.0 validate` | PASS — «No validation issues detected» (`evidencia/verificadores.txt`) |
| H1.S1.M2 | `evidenciaMfa` (y `motivo` en alta) opcionales; `PUT …/roles` `deprecated: true`; sin campos removidos | `git diff` del YAML | PASS — diff solo aditivo |
| H1.S1.M3 | Postman regenerado, inventario alineado, 4 ejemplos del simulado curados y validados contra su esquema | `generar_postman.py`, `inventario_endpoints.py --check`, validación JSON Schema 2020 de los ejemplos | PASS (`evidencia/verificadores.txt`) |
| H1.S4.M3 | Forma v1 del evento de bitácora y del pedido de aviso documentada en CU-08 § «Contrato de eventos» | `verificar_boveda.py` | PASS — EXIT=0 |
| H1.S7.M1 | CU-08 actualizado: rutas reales, errores `AP-CU08-09..11` (+`08` reservado a A), baja/suspensión, 15 escenarios gherkin nuevos trazados a pruebas | `verificar_criterios.py` | PASS — «Sin divergencias» |
| H1.S7.M2 | Verificadores locales del encargo en 0 | ver `evidencia/verificadores.txt` | PASS — 10/10 EXIT=0 |

## A medias
### H1.S2.M1–M3 — Reglas de gobierno (step-up, sobre sí mismo, nunca sin administrador)
- Qué anda: `ReglasDeGobierno`, `EvidenciaDeStepUpJwt` (JWT real en todos los perfiles, `jti` de un solo uso), `ContinuidadDeAdministracion` y `AccesosRepositorio.titularesVigentesDelPermiso` (personas ACTIVAS con asignación vigente) escritos; átomos de dominio compilan.
- Qué no anda / no se sabe: nada ejecutado.
- Qué falta exactamente: que la CI corra `EvidenciaDeStepUpJwtTest`, `GobiernoDeAccesosAtomosTest`, `CU08GobiernoTest`, `RutasDeAccesosWebTest`.
- Dónde quedó: `servicios/identidad/.../aplicacion/ReglasDeGobierno.java`, `infraestructura/EvidenciaDeStepUpJwt.java`, `infraestructura/AccesosRepositorio.java`; rama `claude/gu-b-cu08`.

### H1.S3.M1–M4 — Asignar/revocar por asignación, idempotencia, PUT por diferencia
- Qué anda: `RutasDeAccesos` (extendida por `UsuariosInternosController`), `GobiernoDeAccesos` (idempotencia por contenido, ámbito = ámbito del rol, destinatario operador ACTIVO), `RotacionDeRolesInternos` por diferencia. Firmas confirmadas contra la interfaz generada localmente con `openapi-generator 7.14.0` y las plantillas propias.
- Qué falta: CI de `RutasDeAccesosWebTest`, `RutasDeAccesosInformeWebTest`, `CU08GobiernoTest`, `CU08BajaYVencimientoTest.reemplazoPorDiferencia`.
- Dónde quedó: `web/RutasDeAccesos.java`, `aplicacion/GobiernoDeAccesos.java`, `infraestructura/RotacionDeRolesInternos.java`.

### H1.S4.M1–M2 — Bitácora y aviso
- Qué anda: `BitacoraDeAccesos` emite en la transacción del acto el hecho (`identidad.rol_*`, `identidad.usuario_interno_*`) y `identidad.aviso_de_acceso_solicitado` (afectado + oficiales de cumplimiento vigentes, solo ids). Usado por asignar, revocar, vencer, alta, suspender, reactivar, baja, desbloquear y el PUT deprecado.
- Qué falta: CI de `CU08GobiernoTest.asignaYDejaRastro` y de los conteos de eventos en las demás. El consumidor en `auditoria` es del lote E; el de `notificaciones` no tiene dueño.
- Dónde quedó: `aplicacion/BitacoraDeAccesos.java`, `dominio/HechoDeAcceso.java`.

### H1.S5.M1–M2 — Baja y suspensión
- Qué anda: `CU08DarDeBaja` y `RetiroDeAccesos` escritos; suspender revoca todo, reactivar vuelve sin roles.
- Qué falta: CI de `CU08BajaYVencimientoTest` (baja, baja idempotente, suspensión).
- Dónde quedó: `aplicacion/CU08DarDeBaja.java`, `aplicacion/RetiroDeAccesos.java`, `aplicacion/GestionarUsuariosInternos.java`.

### H1.S6.M1–M2 — Vencimiento e informe
- Qué anda: `CU08VencerAccesos` + `trabajos/VencimientoDeAccesos` (`@EnableScheduling` agregado), `CU08InformeDeAccesos` + `InformeDeAccesosRepositorio`.
- Qué falta: CI de `vencimientoDiario` e `informeDeAccesos`.

### H1.S7.M3 — Push, PR y CI verde
- Qué anda: push hecho, PR #516 abierto contra `dev`.
- Qué falta: CI en verde (diagnosticar y corregir lo que falle).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Enganche de `RolesParaPersonalInterno.admite` en `GobiernoDeAccesos.asignar` | TODO | Que el lote A publique la clase (acordado por mensaje con A) |
| Consumidor de `identidad.aviso_de_acceso_solicitado` en `notificaciones` + `evento_notificable` `ACCESO_DE_OPERADOR_CAMBIADO` | TODO | Asignar dueño (sesión principal) |
| Tabla de evidencias step-up consumidas en `identidad` | TODO | Decisión de modelo (regla 97) |

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
- **Orden de despliegue**: alta, suspensión/reactivación y desbloqueo exigen step-up desde este PR; el backoffice actual no lo envía hasta el lote D → esos botones fallarán con 422 `AP-CU08-09` en `test` si B se promueve antes que D.
- `PUT …/roles` (deprecado) sigue sin step-up por compatibilidad: es una vía de cambio de roles sin segundo factor hasta que se retire.
- Evidencia step-up reutilizable una vez por réplica dentro de su vida (5 min).
- `motivo_revocacion` se recorta a 120 y el de sesión a 80; el texto completo queda en el evento.

## Decisiones y ambigüedades
1. Ruta `/usuarios-internos/...` en lugar de `/accesos/...` — confirmar con Pablo.
2. Revocar con `POST …/revocacion` en lugar de `DELETE` con cuerpo — confirmar con Pablo.
3. Suspender revoca todos los roles; reactivar vuelve sin roles — confirmar con Pablo.
4. Baja → estado `BAJA_VOLUNTARIA` (el enum no tiene baja de empleado) — confirmar con Pablo.
5. El ámbito de la asignación debe ser el del rol (si no, `AP-CU08-02`) — supuesto de seguridad, confirmar con Pablo.
6. Permiso de las rutas nuevas: `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_GESTIONAR` (para que `INTERNAL_IDENTITY_ADMIN` opere, P-2b); la regla «nunca sin administrador» cuenta solo `ACCESOS_ADMINISTRAR`.
7. Consumo del `jti` en memoria — decidir si se agrega tabla.
