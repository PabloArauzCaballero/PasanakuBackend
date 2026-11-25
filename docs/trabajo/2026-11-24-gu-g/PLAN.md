# Plan — Lote G · CU de gobierno de participantes (solo documento)

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (solo `docs/` y `planes/`) · Predecesor: [INVENTARIO de gobierno de usuarios](https://github.com/pabloarauzcaballero/pasanakubackend/blob/claude/modest-newton-4ckcid/docs/trabajo/2026-11-24-gobierno-de-usuarios/INVENTARIO.md), decisión P-11
- Rama: `claude/gu-g-cu-participantes` · PR contra `dev` (no se mergea)
- Resultado observable: Pablo puede leer en `docs/CasosDeUso/` un CU **borrador** que especifica el directorio y la ficha del participante en el backoffice y la suspensión/bloqueo de su cuenta por seguridad con debido proceso, con toda regla no respaldada marcada «PROPUESTA — requiere aprobación de Pablo» y las preguntas abiertas listadas, sin que la bóveda deje de validar.
- Kill-test: `python3 scripts/verificar_boveda.py` no termina en `TODO OK`, o el CU presenta como hecho una regla económica/plazo sin fuente.

## Alcance
- IN: un CU nuevo (`CU-134`); su fila en `_CasosDeUso.md` y `total_casos`; las cifras «112 casos de uso» en `planes/` que el verificador exige actualizar; PLAN/REPORTE/evidencia de este lote.
- IN (agregado durante la ejecución, regla 20.6.6): la línea «Ver también» recíproca en CU-07, 17, 27, 44, 52, 55 y 93 (`verificar_boveda.py` exige reciprocidad), y el catálogo de datos regenerado (`docs/catalogo-de-datos.md`, `servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json`), que la CI exige al día tras agregar un CU que nombra tablas.
- OUT: código, OpenAPI, DDL, semillas, `docs/Restricciones.md` (las restricciones nuevas quedan **propuestas** dentro del CU, no se dan de alta hasta que Pablo apruebe), entidades nuevas del modelo, archivos de los lotes A/B/E.
- Ambigüedades registradas:
  - Número del CU: el siguiente libre en la bóveda es CU-133, pero `CU133*` y `CU136*` ya se usan como prefijo de pruebas en `servicios/entregas` (y `verificar_criterios.py` toma la clase `CU<NN>` como "implementado"). Supuesto: se usa **CU-134**. Confirmar con Pablo.
  - Servicio dueño: `identidad` (es dueño de `usuario.estado`, `bloqueo_cuenta`, `restriccion_usuario`). Las sanciones de participante viven en `garantia` (`sancion` exige `registro_id` de incumplimiento). Supuesto: el expediente de seguridad es de `identidad`. Confirmar con Pablo.
  - Toda regla económica (qué pasa con aportes/entregas en curso) queda como DECISION_REQUIRED.

## H1 — CU borrador de gobierno de participantes listo para aprobación
**CA:** Dado el CU-134 en la bóveda, cuando Pablo lo lee, entonces encuentra causales cerradas, expediente, notificación, plazo de descargo, decisión motivada, FIRME, apelación por otra persona, efectos sobre grupos/dinero, permisos y segregación, datos mínimos con rastro de lectura, y cada regla sin fuente marcada PROPUESTA con opciones.
**DoD:** `verificar_boveda.py` → `TODO OK`; `verificar_criterios.py` → exit 0 (CU-134 pendiente); `verificar_seguridad.py` y `verificar_carriles.py` exit 0; PR a `dev` con CI en verde.
**Estado:** HECHO

### H1.S1 — Relevamiento de lo existente
**CA:** Dado el pedido, cuando se escribe el CU, entonces cada pieza existente se referencia y no se duplica.
**DoD:** tabla de hallazgos en el REPORTE con rutas.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Relevar estados, tablas y CU existentes (CU-04, 07, 08, 17, 25, 27, 44, 52, 55, 70, 93; `usuario`, `bloqueo_cuenta`, `restriccion_usuario`, `sancion`, `apelacion_sancion`, `politica_sancion`, `registro_acceso_datos`; R-SEG, R-GAR, R-ORG-05) | Hallazgos con ruta en el REPORTE | `grep -n "ck_usuario_estado\|ck_bloqueo_cuenta_motivo\|ck_restriccion_usuario_tipo" sql/10_tablas/01_identidad_usuarios/*.sql` → los CHECK citados | HECHO |

### H1.S2 — Escribir el CU
**CA:** Dado el CU-134, cuando se lo valida, entonces tiene todas las secciones de la plantilla y los wikilinks resuelven.
**DoD:** `python3 scripts/verificar_boveda.py` → `TODO OK`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Escribir `CU-134 …md` con la plantilla de `caso-de-uso`, propuestas marcadas y preguntas para Pablo | El archivo existe y no hay wikilink roto | `python3 scripts/verificar_boveda.py \| grep -E "wikilink\|CU-134\|TODO OK\|FALLA"` | HECHO |
| H1.S2.M2 | Agregar la fila al índice `_CasosDeUso.md` y subir `total_casos` a 113 | Índice y total coherentes | `python3 scripts/verificar_boveda.py \| tail -3` → `TODO OK` | HECHO |
| H1.S2.M3 | Actualizar las cifras «112 casos de uso» que el verificador marque en `planes/` | Sin cifras desfasadas | `python3 scripts/verificar_boveda.py \| grep -A2 "CIFRAS"` → OK | HECHO |

### H1.S3 — Gates y entrega
**CA:** Dado el PR, cuando corre la CI, entonces queda en verde.
**DoD:** salidas pegadas en `evidencia/` y checks del PR en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Correr los verificadores de bóveda locales del workflow `boveda-y-esquema.yml` | Todos exit 0 | `verificar_seguridad.py; verificar_boveda.py; verificar_carriles.py; verificar_criterios.py` → exit 0 | HECHO |
| H1.S3.M2 | REPORTE.md con preguntas para Pablo | Tres secciones + preguntas | `python3 .claude/hooks/report_gate.py --self-test` y lectura del archivo | HECHO |
| H1.S3.M3 | Commit, push y PR a `dev` con CI en verde | PR abierto, checks verdes | estado de checks del PR | HECHO |

## H2 — CU-134 pasa de borrador a aprobado
**CA:** Dado que Pablo aprobó el lote G el 2026-11-24 («Aprobado todo lo de G ya mismo», comunicado por la sesión principal), cuando se lee CU-134, entonces figura APROBADO con fecha y fuente, cada D-n tiene su opción aprobada, y lo que el borrador no proponía (números de D-3, D-11) sigue `DECISION_REQUIRED`.
**DoD:** `verificar_boveda.py` → `TODO OK`; catálogo de datos sin diferencias; PR a `dev`.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | CU-134: estado APROBADO, «Decisiones aprobadas», PROPUESTA→APROBADO, pasos 7 y 18 según D-2/D-10/D-12 | Ninguna «PROPUESTA — requiere aprobación» queda en el CU | `grep -c "requiere aprobación de Pablo" CU-134…md` → 0 y `verificar_boveda.py` → `TODO OK` | HECHO |
| H2.S1.M2 | Fila del índice y nota del rango sin «borrador» | Índice dice «Aprobado por Pablo el 2026-11-24» | `grep -c "Aprobado por Pablo el 2026-11-24\|fue aprobado por Pablo" _CasosDeUso.md` → 2 | HECHO |
| H2.S1.M3 | PR a `dev` con CI en verde | PR abierto y checks verdes | checks del PR | EN CURSO |

## H3 — Implementación de CU-134 (Ola 3)
**CA:** Dado un operador con permiso, cuando busca un participante, abre su ficha, propone una medida, el afectado se descarga, otra persona decide y una tercera resuelve la apelación, entonces cada paso queda en la base con rastro, plazos guardados en días hábiles, segregación verificada en la base, y la entrega de quien tiene medida vigente queda retenida (contra un doble de `entregas` si no expone el contrato).
**DoD:** DDL regenerado sin deriva; `./gradlew :servicios:identidad:test` en verde con pruebas contra Postgres real; `verificar_boveda.py`, `verificar_criterios.py` (CU-134 implementado: escenarios → pruebas), `inventario_endpoints.py --check`, `verificar_contratos_limites.py` en 0; backoffice compila y sus specs pasan; capturas de las pantallas; PR a `dev` con CI verde.
**Estado:** EN CURSO
**Desvío registrado:** D-3 aprobó guardar los plazos en `politica_sancion` (esquema `garantia`); la regla 98.1 prohíbe que `identidad` lea esa base. Se guardan en una política propia de `identidad` (`politica_medida_cuenta`), con los mismos valores, versionada y editable sin código. Así se resuelve también A-2: la prescripción queda en meses y la duración máxima en días, cada una en su columna.

### H3.S1 — Modelo y esquema
**Estado:** HECHO — evidencia `evidencia/H3.S1-*.txt` (aplicar dos veces, restricciones en tres niveles contra Postgres 16, semillas idempotentes, deriva en cero)
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Tablas nuevas en `.puml` + fichas: `politica_medida_cuenta`, `expediente_medida_cuenta`, `evidencia_medida_cuenta` (append-only), `historial_estado_medida_cuenta` (append-only), `apelacion_medida_cuenta` | DDL generado con las 5 tablas | `python3 scripts/generar_ddl.py` exit 0 y `ls sql/10_tablas/01_identidad_usuarios/` las lista | HECHO |
| H3.S1.M2 | CHECK ampliados: `bloqueo_cuenta.motivo` + `CUENTA_COMPROMETIDA`; `registro_acceso_datos.tipo_dato` + `FICHA_PARTICIPANTE`, `CORREO` | ALTER emitido para bases existentes | `grep CUENTA_COMPROMETIDA sql -r` y `grep FICHA_PARTICIPANTE sql -r` | HECHO |
| H3.S1.M3 | Restricciones R-SEG nuevas (segregación por trigger, un expediente abierto por causal, una apelación, evidencia inmutable) en `Restricciones.md` | Verificador en verde | `verificar_boveda.py` → `TODO OK` | HECHO |
| H3.S1.M4 | Semillas: permisos `PARTICIPANTES_*` + roles; política de plataforma D-3, idempotente | 2ª corrida sin cambios | `psql -f sql/aplicar.sql && psql -f sql/60_semillas/sembrar.sql` dos veces, conteos iguales | HECHO |
| H3.S1.M5 | Regeneración de derivados (bóveda, metadata, catálogo) y deriva en cero | Sin diferencias tras regenerar | `generar_ddl`, `generar_semillas`, `catalogo_de_datos --escribir`, `git diff --exit-code sql/` tras segunda corrida | HECHO |

### H3.S2 — Contrato `identidad.yaml` (tag Participantes, solo operaciones nuevas)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S2.M1 | Operaciones y esquemas de búsqueda, ficha, revelación, medida, descargo, decisión, apelación y resolución, con `AP-CU134-01..11` | Contrato válido y con límites | `verificar_contratos_limites.py` exit 0 | HECHO — aplicado en `identidad.yaml` con `evidenciaMfa`; `verificar_contratos_limites.py` e `inventario_endpoints.py --check` exit 0 |

### H3.S3 — Backend `identidad`
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S3.M1 | Directorio, ficha enmascarada y revelación con `registro_acceso_datos` | Escenarios gherkin de lectura en verde | `./gradlew :servicios:identidad:test --tests 'CU134*'` | A MEDIAS — código y prueba escritos (`CU134ConsultarParticipante`, `CU134GestionarMedida`, `RutasDeParticipantes` (servida por `VerificacionesController`), `CU134Test`); `verificar_criterios.py` lo da por implementado; falta la corrida real de `CU134Test` en la CI (build local bloqueado) |
| H3.S3.M2 | Proponer medida (cautelar, idempotencia, sesiones revocadas, notificación por outbox) | Escenarios de propuesta en verde | ídem | A MEDIAS — código y prueba escritos (`CU134ConsultarParticipante`, `CU134GestionarMedida`, `RutasDeParticipantes` (servida por `VerificacionesController`), `CU134Test`); `verificar_criterios.py` lo da por implementado; falta la corrida real de `CU134Test` en la CI (build local bloqueado) |
| H3.S3.M3 | Descargo, decisión por otra persona y firmeza | Escenarios de decisión en verde | ídem | A MEDIAS — código y prueba escritos (`CU134ConsultarParticipante`, `CU134GestionarMedida`, `RutasDeParticipantes` (servida por `VerificacionesController`), `CU134Test`); `verificar_criterios.py` lo da por implementado; falta la corrida real de `CU134Test` en la CI (build local bloqueado) |
| H3.S3.M4 | Apelación por una tercera, resolución a favor por vencimiento, prescripción y reversión (trabajo diario) | Escenarios de apelación en verde | ídem | A MEDIAS — código y prueba escritos (`CU134ConsultarParticipante`, `CU134GestionarMedida`, `RutasDeParticipantes` (servida por `VerificacionesController`), `CU134Test`); `verificar_criterios.py` lo da por implementado; falta la corrida real de `CU134Test` en la CI (build local bloqueado) |
| H3.S3.M5 | Consulta interna «¿tiene medida vigente?» para `entregas` | Contrato de servicio a servicio probado | ídem | A MEDIAS — código y prueba escritos (`CU134ConsultarParticipante`, `CU134GestionarMedida`, `RutasDeParticipantes` (servida por `VerificacionesController`), `CU134Test`); `verificar_criterios.py` lo da por implementado; falta la corrida real de `CU134Test` en la CI (build local bloqueado) |

### H3.S4 — Entrega retenida (D-11) en `entregas`
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S4.M1 | Contrato mínimo y doble en tres niveles (correcto, límite, inválido/caída) de la consulta a `identidad` | Pruebas del doble en verde | `./gradlew :servicios:entregas:test --tests '*MedidaVigente*'` | A MEDIAS — puerto `MedidaDeCuenta` y adaptador `MedidaDeCuentaPorHttp` (falla cerrado) con `MedidaDeCuentaPorHttpTest` en tres niveles contra una identidad de ensayo; falta la corrida en la CI |
| H3.S4.M2 | Retener la entrega con medida vigente y liberarla al levantarse, con idempotencia y evidencia de la regla 91.6 | Duplicado sin doble efecto | ídem + evidencia 91.6 | A MEDIAS — `CU28EmitirDesembolso` rechaza con AP-CU28-08 y la entrega sigue AUTORIZADA (consulta fuera de la transacción, en `DesembolsosController`); criterio nuevo en CU-28 con su prueba; falta la corrida en la CI |

### H3.S5 — Modo restringido del suspendido (D-6)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S5.M1 | `SUSPENDIDO` entra con alcance restringido (ver medida, descargo, apelación, pagar, reclamar) | Escenario de CU-04 ajustado en verde | `./gradlew :servicios:identidad:test --tests 'CU04*'` | A MEDIAS — Qué anda: hoy `SUSPENDIDO` no obtiene token (`PuedeAutenticarse`, `CU04Autenticar.java:132`): falla cerrado, más estricto que D-6. Qué no anda: no hay claim de alcance restringido ni lista blanca de rutas. Contrato del colaborador (registrado, regla 65): CU-04 emite `alcance=RESTRINGIDO` para `SUSPENDIDO`; `GuardiaDePermiso` (plataforma/comun-web, compartida por todos los servicios) admite solo rutas marcadas como permitidas en restricción. Tres niveles del contrato, declarados contra el doble «emisor actual» (sin build local): correcto — `SUSPENDIDO` recibe token restringido y lee su medida → hoy NO (sin token); límite — ruta fuera de la lista blanca → 403 `RESTRINGIDO` → hoy inalcanzable; inválido — `BLOQUEADO` o `CERRADO` → sin token → hoy SÍ (`AP-CU04-02`). Qué falta: el cambio aditivo en CU-04 y en `GuardiaDePermiso`, coordinado con el lote C (avisado) porque toca la emisión de tokens y la guardia común; probarlo en CI. Dónde quedó: sin código en esta rama |
| H3.S5.M2 | `plataforma`: anotación `@PermitidoEnRestriccion` y `GuardiaDePermiso` deniega (403 del contrato) a un token con `alcance=RESTRINGIDO` en todo endpoint no anotado | Tres niveles: restringido en ruta anotada pasa; restringido en ruta no anotada 403; token sin el claim se comporta como hoy | `./gradlew :plataforma:comun-web:test --tests '*GuardiaDePermiso*'` (en CI) | TODO — asignado a G por la sesión principal el 2026-11-25 (C no lo toma) |
| H3.S5.M3 | `identidad`: CU-04 y la renovación emiten `alcance=RESTRINGIDO` para `SUSPENDIDO` y `BLOQUEADO_SEGURIDAD` en vez de rechazar; el resto de estados no habilitados sigue rechazado | CU04Test: suspendido entra restringido; `ELIMINADO` sigue con AP-CU04-02 | `./gradlew :servicios:identidad:test --tests 'CU04*'` (en CI) | TODO — sin tocar la revocación (lote E) |
| H3.S5.M4 | Anotar la lista cerrada: ficha/medida propia, descargo, apelación (identidad), pagar aportes, reclamar | WebTest por servicio: restringido llega a esas rutas | WebTests de cada controlador (en CI) | TODO |

### H3.S6 — Backoffice
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S6.M1 | Entrada «Participantes» (bajo Cumplimiento › Riesgo y seguridad: Cumplimiento ya tiene 8 pestañas, tope del test del menú) + directorio con cuatro estados | Specs en verde | `npx ng test --include='src/app/nucleo/menu*.spec.ts' --include='src/app/rutas/cumplimiento/**/*.spec.ts'` | HECHO — 42 archivos / 311 tests en verde (`evidencia/h3-s6-ng-test.txt`); typecheck y lint exit 0 (`evidencia/h3-s6-typecheck-lint.txt`). Clientes Angular del tarball, no commiteados |
| H3.S6.M2 | Ficha enmascarada con justificación previa y medidas con línea de tiempo | Specs en verde + capturas en 3 viewports y 2 temas | `playwright test e2e/visual-participantes.e2e.ts --workers=1` | HECHO (VERIFIED_FUNCTIONAL contra backend SIMULADO, declarado) — specs 576/576 en verde; 24 capturas 3 viewports × 2 temas × (directorio datos/vacío/error + ficha), consola sin errores, inspeccionadas (`evidencia/h3-s6-capturas.txt`). No verificado contra `identidad` real. Las capturas corrigieron códigos crudos y el color del enlace |
| H3.S6.M3 | Acciones con segundo factor: revelar dato, proponer, decidir y resolver apelación | Specs en verde + capturas del diálogo | specs del backoffice | TODO — los endpoints existen (`identidad.yaml`, `evidenciaMfa`); falta la pantalla y el paso de step-up del backoffice |

### H3.S7 — Regresión y gates
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S7.M1 | Regresión de `identidad` y `entregas`, verificadores, PR con CI verde | Todo en 0 | salidas pegadas | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Presentar como regla algo inventado | Pablo aprueba algo sin fuente | Todo lo no respaldado va «PROPUESTA — requiere aprobación de Pablo» con opciones |
| El verificador exige entidades/restricciones existentes | CI rojo | Solo wikilinks a entidades existentes; lo nuevo en `código` y marcado propuesto |
| Deber de reserva (CU-44) choca con "notificar al afectado" | Tipping-off | Causal de inteligencia financiera fuera de este CU: se deriva a CU-44 |
