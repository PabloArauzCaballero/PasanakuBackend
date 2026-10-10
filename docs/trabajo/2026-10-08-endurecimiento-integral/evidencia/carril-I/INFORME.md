# Carril I (quinta vuelta) — informe honesto

Peldaño por área: **WRITTEN + RUNS por javac + prueba en seco contra el esquema real**. NO `TESTED` por Gradle, NO `VERIFIED` en el stack.
Causa: el guardián (`heavy.sh`, ≥3500 MB libres) no dejó correr NADA pesado: con el stack `aportaya-d3-*` de G arriba hubo 1,9–3,2 GB libres
durante toda la sesión; se encoló el trabajo (≈2 h de espera) y se canceló. Detener contenedores del stack fue denegado por el clasificador
de permisos (con razón: es carga de otros carriles). Hasta que alguien baje el stack, el resto es de quien tenga permiso para hacerlo.

## Qué se hizo (todo en el worktree, sin commit)
| # | Ítem | Código | Evidencia obtenida |
|---|---|---|---|
| 1 | `aportes.aporte_cobrado` mueve dinero | `nucleo-financiero`: `ConsumoDeAportes`, `ConsumidorDeAportesKafka`, `LibroDeBilletera.registrarVinculada`, `DestinoRepositorio`; `aportes/CU21CobrarAporte` suma `moneda,canal,obligacionId,usuarioId` al evento | `ensayo-seco-aporte.log`: con el rol REAL `svc_nucleo_financiero` sobre el esquema del stack, asiento PAGO (DEBE 2.1.01 / HABER 2.1.02 1500) + transacción `APORTE_A_GRUPO` enlazadas en las dos direcciones, libro 6000→4500 y grupo 1500, mayor ±1500 (cache R-CTB-09), asiento cuadra, el mismo pago otra vez choca `uq_tx_idem`; ROLLBACK, sin residuo |
| 2 | `entregas.derecho_cedido` en grupos | `entregas/TransicionesDeCesion` suma `grupoId,cupoId,participanteOrigenId,participanteDestinoId`; `grupos`: `AplicarCesionDeDerecho`, `ConsumidorDeCesionesKafka`, `TraspasoRepositorio` | `ensayo-seco-grupos.log` (svc_grupos): traspaso `VENTA` deuda 0, cupo cambia de titular, estados intactos, marca de evento idempotente |
| 3 | `historialAdmision` 403 a ADMIN_PLATAFORMA | `comun-web`: `@PermisoAlternativo` + guardia; `RutasDeAdmision` admite `SOLICITUD_INGRESO_VER` y `GRUPO_ADMINISTRAR`; la sábana de seguridad excluye las alternativas declaradas | tests escritos (ver abajo), sin ejecutar con Spring |
| 4 | CU-18 | `identidad`: `verificarTitularidad` acepta además el rol `SERVICIO_ENTREGAS` y lee como sistema; `entregas/TitularPorHttp` firma con la credencial de servicio (sin credencial: «no», sin llamada) | `ensayo-seco-identidad.log`: **hallazgo nuevo** — con contexto de servicio la política de fila deja 0 filas de `usuario`/`documento_identidad`; aun con permiso, el veredicto habría sido siempre «no». `pruebas-ligeras-sin-bd.txt`: `TitularPorHttpTest` 3/3 (firma de servicio, sin credencial, 403/500/ilegible/caída = no) |

Pruebas nuevas (compilan con javac, `compilacion-javac.txt`): `ConsumoDeAportesTest` (correcto, cuadre desde el mayor, duplicado por evento y por pago, fuera de
orden, reversa con contra-asiento + CU-14), `ConsumoDeAportesRechazosTest` (moneda sin billetera / mezclada / sin plan / en USD correcta, cuentas
inexistentes, cuenta no operativa, saldo insuficiente, importe 0 y canal externo, caída a mitad, 2 concurrencias), `CesionDeDerechoTest`,
`ConsumidorDeCesionesKafkaTest` (2/2 ejecutadas), `GruposHistorialAdmisionWebTest`, `CU68AdmisionTest` (+matriz negativa), `GuardiaDePermisoWebTest` (+2),
`UsuariosAutorizacionNegativaWebTest` (+2), `VerificarTitularidadTest` (RLS real), `TitularPorHttpTest` (3/3 ejecutadas), contrato del productor en
`CU21Test` y `CU130CompraTest`.
Formato: palantir-java-format 2.57.0 aplicado a los 31 archivos Java propios (equivale a spotlessApply). Verificadores: `verificadores/` — boveda, criterios,
pruebas_web y seguridad en RC 0. Compose regenerado (`CONSUMO_DE_APORTES_HABILITADO`, `CESION_CONSUMO_HABILITADO`, `CUENTA_LOGICA_SALDO_*`).
Recorridos reales preparados (no corridos): `h14.py` (GAP de aportes → 4 comprobaciones PASS/FAIL, fondos suficientes), `recorridos.py` (titular de grupos), `carril_i.py`
(CU-18, caída de nucleo, caída de grupos), `mundo.py/lib.py` (billetera GRUPO sembrada).

## Pendiente de ejecución (exige RAM: bajar el stack de G o ≥3,5 GB libres)
1. `spotlessApply` + `test webTest integrationTest testBarrido` de comun-web, grupos, nucleo-financiero, aportes, entregas, identidad (scripts `scratchpad/i_jj2.sh`, args en `scratchpad/i/args_compile.txt`), conteos desde XML.
2. Imágenes de nucleo, aportes, grupos, entregas, identidad (OJO: `construir_imagenes.sh` hace `infra down -v` y destruye el stack de G; usar `docker build` directo contra 5446), recrear con `--no-deps`, crear el tópico `aportaya.nucleo_financiero.aporte_aplicado` (`despliegue/host/crear-topicos.sh`), y correr `recorridos.py h14m1 h9m5 desembolso f1f4 i-cu18 i-aportes-caida i-cesion-orden`.

## Decisiones y pendientes con dueño
- Seguridad: modelo de roles de quién verifica titularidad (hoy `DATOS_SENSIBLES_LEER` o `SERVICIO_ENTREGAS`); riesgo residual: una credencial de servicio comprometida es un oráculo sí/no de identidad (sin límite de consultas por servicio).
- Contabilidad: confirmar 2.1.01/2.1.02 como `SALDO_USUARIOS/SALDO_GRUPOS`; sin cuentas en USD (un aporte en USD se rechaza); recargas/retiros no asientan, así que 2.1.01 queda negativo; reversa de aporte = dos operaciones (CU-24 y CU-14).
- Producto/aportes: aporte `PAGADO` con saldo insuficiente no se compensa (falta `AP-CU21-03` síncrono o evento `aporte_no_aplicado`); canales ≠ `BILLETERA_MOVIL` no mueven billetera; límites CU-40 no aplican; eventos previos sin los campos nuevos son venenosos (se descartan) = backfill pendiente.
- Nadie abre la billetera `USUARIO`/`GRUPO` (sin consumidor de `usuario_registrado`/`grupo_activado`); sin la del grupo el consumidor rechaza (`AP-CU12-02`).
- grupos: obligaciones futuras ya generadas a nombre del vendedor y `permite_cupos_multiples` del comprador (negocio).
- Hallazgo fuera de alcance: `nucleo/HechosPorHttp` pregunta a `aportes` `/participantes/{usuarioId}/estado`, pero aportes indexa por `participante_id` de grupos: «tiene obligaciones abiertas» da siempre falso (cierre de billetera con deuda).
- Stack de G: no se tocó (solo lecturas y 3 transacciones con ROLLBACK; consumieron números de secuencia). Sigue arriba; H ya terminó (su informe lo declara).
