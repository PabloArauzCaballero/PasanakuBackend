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
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Tablas nuevas en `.puml` + fichas: `politica_medida_cuenta`, `expediente_medida_cuenta`, `evidencia_medida_cuenta` (append-only), `historial_estado_medida_cuenta` (append-only), `apelacion_medida_cuenta` | DDL generado con las 5 tablas | `python3 scripts/generar_ddl.py` exit 0 y `ls sql/10_tablas/01_identidad_usuarios/` las lista | TODO |
| H3.S1.M2 | CHECK ampliados: `bloqueo_cuenta.motivo` + `CUENTA_COMPROMETIDA`; `registro_acceso_datos.tipo_dato` + `FICHA_PARTICIPANTE`, `CORREO` | ALTER emitido para bases existentes | `grep CUENTA_COMPROMETIDA sql -r` y `grep FICHA_PARTICIPANTE sql -r` | TODO |
| H3.S1.M3 | Restricciones R-SEG nuevas (segregación por trigger, un expediente abierto por causal, una apelación, evidencia inmutable) en `Restricciones.md` | Verificador en verde | `verificar_boveda.py` → `TODO OK` | TODO |
| H3.S1.M4 | Semillas: permisos `PARTICIPANTES_*` + roles; política de plataforma D-3, idempotente | 2ª corrida sin cambios | `psql -f sql/aplicar.sql && psql -f sql/60_semillas/sembrar.sql` dos veces, conteos iguales | TODO |
| H3.S1.M5 | Regeneración de derivados (bóveda, metadata, catálogo) y deriva en cero | Sin diferencias tras regenerar | `generar_ddl`, `generar_semillas`, `catalogo_de_datos --escribir`, `git diff --exit-code sql/` tras segunda corrida | TODO |

### H3.S2 — Contrato `identidad.yaml` (tag Participantes, solo operaciones nuevas)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S2.M1 | Operaciones y esquemas de búsqueda, ficha, revelación, medida, descargo, decisión, apelación y resolución, con `AP-CU134-01..11` | Contrato válido y con límites | `verificar_contratos_limites.py` exit 0 | TODO |

### H3.S3 — Backend `identidad`
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S3.M1 | Directorio, ficha enmascarada y revelación con `registro_acceso_datos` | Escenarios gherkin de lectura en verde | `./gradlew :servicios:identidad:test --tests 'CU134*'` | TODO |
| H3.S3.M2 | Proponer medida (cautelar, idempotencia, sesiones revocadas, notificación por outbox) | Escenarios de propuesta en verde | ídem | TODO |
| H3.S3.M3 | Descargo, decisión por otra persona y firmeza | Escenarios de decisión en verde | ídem | TODO |
| H3.S3.M4 | Apelación por una tercera, resolución a favor por vencimiento, prescripción y reversión (trabajo diario) | Escenarios de apelación en verde | ídem | TODO |
| H3.S3.M5 | Consulta interna «¿tiene medida vigente?» para `entregas` | Contrato de servicio a servicio probado | ídem | TODO |

### H3.S4 — Entrega retenida (D-11) en `entregas`
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S4.M1 | Contrato mínimo y doble en tres niveles (correcto, límite, inválido/caída) de la consulta a `identidad` | Pruebas del doble en verde | `./gradlew :servicios:entregas:test --tests '*MedidaVigente*'` | TODO |
| H3.S4.M2 | Retener la entrega con medida vigente y liberarla al levantarse, con idempotencia y evidencia de la regla 91.6 | Duplicado sin doble efecto | ídem + evidencia 91.6 | TODO |

### H3.S5 — Modo restringido del suspendido (D-6)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S5.M1 | `SUSPENDIDO` entra con alcance restringido (ver medida, descargo, apelación, pagar, reclamar) | Escenario de CU-04 ajustado en verde | `./gradlew :servicios:identidad:test --tests 'CU04*'` | TODO |

### H3.S6 — Backoffice
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S6.M1 | Entrada «Participantes» (coordinada con lote D) + directorio con cuatro estados | Specs en verde | specs del backoffice | TODO |
| H3.S6.M2 | Ficha enmascarada con «Revelar» y expediente con línea de tiempo y acciones por permiso | Specs en verde + capturas en 3 viewports y 2 temas | specs + `evidencia/` | TODO |

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
