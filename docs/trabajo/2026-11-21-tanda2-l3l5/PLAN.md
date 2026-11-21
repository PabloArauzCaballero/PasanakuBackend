# Plan — Tanda 2, lote L3L5 (grupos, organizador y servicios de apoyo)

- Fecha: 2026-11-21 · Repo: `PabloArauzCaballero/PasanakuBackend` · Base: `origin/dev` 50cf3be9
- Predecesor: plan de correcciones `docs/trabajo/2026-11-21-plan-correcciones/` (PLAN.md, anexos L3 y L5)
- Resultado observable: (1) la suite unitaria de `grupos` deja de fallar de forma intermitente por el arranque
  de Mockito; (2) en los servicios del lote, nadie actúa sobre un recurso que no es suyo ni con identidad
  tomada del cuerpo; (3) desaparecen el organizador profesional y las aprobaciones eliminables.
- Kill-test: reproducir el timeout de `AdmisionDelGrupoSustitucionTest` con la CPU repartida; pedir con el token
  del creador el retiro de otro integrante y que se cree la fila.

## Alcance
- IN: `servicios/{grupos,organizador,auditoria,notificaciones,publicidad,tarifas,transparencia,inversiones,erp}/**`,
  sus `docs/entidades/*.puml`, `contratos/**` y `pactos/**`; filas de esos servicios en `docs/Restricciones.md`;
  `scripts/verificar_seguridad.py`; esta carpeta.
- OUT: `plataforma/**`, `buildSrc/**`, `build.gradle*` raíz, `.github/**`, `apps/**`, `sql/` a mano, servicios
  de otros lotes. Lo que lo necesite queda `A MEDIAS` con el cambio exacto descrito.
- Ambigüedades: las del anexo L3 ya decididas en DECISIONES tanda 7/9 mandan sobre el supuesto del anexo.

## H1 — Estabilidad de la suite y seguridad (F1) del lote

### H1.S0 — Test intermitente `grupos · AdmisionDelGrupoSustitucionTest`
**CA:** Dado el runner con la CPU repartida, cuando corre el corredor unitario de `grupos`, entonces
`armar()` no excede los 5 s por el arranque de Mockito, sin subir ningún límite.
**DoD:** reproducción del fallo antes y corrida en verde después, bajo la misma carga, con salida pegada.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| T2.M0 | Mockito como `-javaagent` y su arranque único pagado al abrir la sesión de JUnit (`LauncherSessionListener`) en `grupos` | Con 1 núcleo + 3 procesos ocupados, la clase pasa 3/3 (antes: `armar() timed out after 5 seconds`) | `taskset -c 0 ./gradlew --no-daemon :servicios:grupos:test --tests '*AdmisionDelGrupoSustitucionTest' --rerun` ×3 + `:servicios:grupos:test webTest` | HECHO |
| T2.M0b | Mismo arreglo para el resto de servicios (en `buildSrc` + `comun-pruebas`) | Ninguna JVM de prueba se auto-adjunta | grep `self-attaching` vacío en `build/test-results` | HECHO (coordinación, #444 en `dev`; incluye #423) |
| T2.M1 | Causa raíz del intermitente `inversiones · AliadoSimuladoFlujosContratoTest` (pedido de la coordinación) | Bajo carga, 0 fallos (antes 29/60) sin reintentos ni timeouts | Diagnóstico A/B bajo carga + `contractTest` ×3 con carga | HECHO (#453 en `dev`) |

### H1.S1 — F1 del anexo L3 (servicio `grupos`)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F1.L3.M04 | Retiro de un grupo solo para quien llama (SEG-001) | El creador pidiendo el retiro de otro → 403/404, sin fila | `./gradlew :servicios:grupos:webTest --tests '*GruposRetiroWebTest'` (matriz negativa) | A MEDIAS (app HECHO; RLS en base pendiente: bloque genérico compartido) |
| F1.L3.M06 | Estados vivos del grupo únicos (DEFECTO-002) | Participante de grupo `EN_CURSO` cuenta ≥ 1 | `EstadosContraElModeloTest` + consulta vieja/nueva por `psql` | HECHO |
| F1.L3.M07 | Resolver acuerdo con precondición en el `UPDATE` (DEFECTO-003) | Dos votos decisivos → un solo `acuerdo_resuelto` | Carrera con dos conexiones `psql` + `ResolucionDeAcuerdoTest` | HECHO |
| F1.L3.M05 | Traspaso de cupo con KYC real (SEG-004) | KYC no aprobado → 422; identidad caída → 503 | `TraspasoKycDelEntranteWebTest` (tres niveles + reintento) | A MEDIAS (caída → 422 fail-closed, no 503) |
| F1.L3.M03 | Destino de transferencia por identificador único (DINERO-002) | Homónimos → 422 | Toca `nucleo-financiero` e `identidad`: solo la parte de `grupos` | TODO |
| F1.L3.M08 | Importes con moneda y `CHECK >= 0` en `grupos` (DINERO-003) | `INSERT` negativo falla en la base | `generar_ddl.py` + psql | TODO |
| F1.L3.M09 | Secreto de invitación fuera del path | — | Toca `apps/movil`: fuera de alcance | TODO |

### H1.S2 — F1 del anexo L5 en los servicios del lote y F1.S2.M3 (BOLA)
| ID | Microtarea | Servicio | Estado |
|---|---|---|---|
| F1.S2.M3 | Autorización por propiedad del recurso en cada endpoint del lote + matriz en `verificar_seguridad.py` | todos | TODO |
| F1.L5.M26 | Avisos internos filtrados por destinatario | notificaciones | HECHO (app); RLS: A MEDIAS |
| F1.L5.M27 | Retirar la consulta de supresión con el contacto en la URL | notificaciones | HECHO |
| F1.L5.M23 | Webhook de mensajería derivado de la carga firmada, con ventana | notificaciones | HECHO (contra doble del proveedor) |
| F1.L5.M21 | Certificado de reputación con datos del servidor | transparencia | HECHO (contra doble de identidad) |
| F1.L5.M22 | Sello de la cadena con permiso propio | transparencia | TODO |
| F1.L5.M20 | Sorteo con entropías reveladas | grupos/transparencia | TODO |
| F1.L5.M28 | Tarifario: actor de la sesión y tipo de cambio derivado | tarifas | A MEDIAS (actor HECHO; tipo sin fuente) |
| F1.L5.M13/M14/M15 | Comisión: devolución con cuatro ojos del token, sin descuentos, sin marcado manual | (relocalizar) | TODO |
| F1.L5.M29 | Publicidad: propiedad de la cuenta, impresión única y tope | publicidad | TODO |
| F1.L5.M09 | `Idempotency-Key` en mutaciones de los servicios de apoyo | lote | TODO |

## H2 — Retiro (F2) en los servicios del lote
| ID | Microtarea | Estado |
|---|---|---|
| F2.M6 | Eliminar el organizador profesional (F2.L3.M11–M14) | TODO |
| F2.M8 | Eliminar las aprobaciones eliminables en el lote (F2.L3.M10, M15) | TODO |

Cada fila se detalla con CA y DoD propios al abrirla (regla 50), antes de escribir código.

## H3 — Registro central de parámetros financieros en `tarifas` (F3.S8.M2/M6, pedido de la coordinación; después de #448)
**CA:** Dado un parámetro financiero sin versión aprobada, cuando una operación lo necesita, entonces falla con un error
que nombra lo que falta; una versión la propone una persona y la aprueba otra (los dos del token), y un grupo en curso
nunca ve una versión posterior a su inicio.
**DoD:** los de cada microtarea; propuesta en [PROPUESTA-F3.S8-parametros.md](./PROPUESTA-F3.S8-parametros.md)
(rama `correcciones/tanda2-l3l5-f3-parametros`).
**Estado:** TODO — el modelo espera el contrato que L2 deja en `docs/trabajo/2026-11-21-tanda2-l2/` (no está en `dev`).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| F3.S8.M2a | Modelo en `11_tarifas_comisiones.puml` + filas de `tarifas` en `Restricciones.md`, alineado al contrato de L2 | Cada `CHECK` (cuatro ojos, `>= 0`, unicidad) rechaza su violación en la base | `generar_ddl.py` + aplicar + deriva + `psql` (PR chico) | TODO |
| F3.S8.M2b | Proponer, aprobar (cuatro ojos del token) y consultar vigentes; falla explícita si falta | Aprobar uno mismo → rechazo; sin versión → 422 con el código | unitarias + web con matriz negativa | TODO |
| F3.S8.M6 | Congelamiento al iniciar el grupo y consulta por grupo | El mismo `grupo_iniciado` dos veces congela una vez; una versión nueva no cambia el grupo | prueba de duplicado + `psql` | TODO |
| F3.S8.M4 | Pantalla del backoffice | Cuatro estados, error por campo, historial | prueba visual 3 viewports × 2 temas + E2E | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Pruebas con Testcontainers no corren sin Docker | `CU*Test`, `*RepositorioTest` quedan `NO COMPROBADO` | Invariantes por `psql` contra la base local en 5433; comando listo para la máquina de Pablo |
| 429 de Maven Central | Builds fallan al resolver | Reintento; la caché avanza en cada intento |
