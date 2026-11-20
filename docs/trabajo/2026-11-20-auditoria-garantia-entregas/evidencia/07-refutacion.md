# Fase 7 — Refutación de S1 y S2 (agente principal)

Cada S1 fue reabierto en su archivo por el agente principal (no por el subagente que lo encontró, regla 70.4.8).
Comandos ejecutados y resultado resumido; la salida literal relevante está citada en la ficha de cada hallazgo.

| Hallazgo | Comprobación | Explicación que lo desmentiría y resultado |
|---|---|---|
| RESPALDO-001 | `sed -n` sobre `CU22EntregarPozoCompleto.java:67-85`, `RespaldoPorHttp.java:56`, `GarantiaController.java:172-175`, `AsientoPropuesto.java:47-48`; `grep -rhoE 'aportaya\.garantia\.[a-z_]+'` | ¿Bandera que lo apague? `grep -rn ConditionalOn servicios/entregas/src/main` → nada; `application.yml:46` «cubre el faltante con la reserva empresarial». ¿Nadie lo llama? Ninguna pantalla; lo invoca un operador por API. **Resiste (S1, camino vivo expuesto).** |
| RESPALDO-002 | `grep -n 'Permiso\|public ResponseEntity' GarantiaController.java`; `grep -n 'AsientoPropuesto\.'` | ¿Consumidor del asiento? Sí, `ConsumidorDeAsientosKafka` escucha los 4 tópicos `respaldo_*`. **Resiste.** |
| RESPALDO-003 | `grep -n monto_cubierto_empresa fondeo_entrega.sql`; `sed -n 2585,2590p restricciones.sql` | ¿Columna muerta? La regla R-DES-03 la usa en la ecuación del pozo. **Resiste.** |
| SEG-001 | `sed -n 40,120p CU23RecuperarRespaldo.java`; `grep -n EntradaRecuperacion RespaldoWeb.java` | ¿Valida el pago contra aportes? No: usa `e.pago()` del cuerpo; tope = lo cubierto. **Resiste.** |
| SEG-002 | `grep -rniE 'organizador|esMiembro|pertenece|usuarioId\(\)' servicios/garantia/src/main`; `GuardiaDePermiso.java:55-80`; `restricciones.sql:1430-1462` | ¿Autorización en RLS (ADR-007/021)? Las tablas sin `usuario_id` no reciben política; las con `usuario_id` solo admiten rol privilegiado/sistema/titular. ¿Filtro? La guardia solo compara permiso. **Resiste.** |
| SEG-003 | `sed -n 140,165p IncumplimientosController.java`; `grep -n 'quedaPendiente\|aprobadaPor\|estado' CU23CubrirIncumplimiento.java` | ¿Precondición de firmeza en otro lado? En 71-125 solo se valida existencia del expediente, cobertura previa y fondo ACTIVO. **Resiste.** |
| SEG-004 | `sed -n 55,75p GarantiaController.java`; `grep -n 'grupoCerrado\|deudasVivas' CU29DevolverFondo.java` | ¿Se coteja con grupos? No. **Resiste.** |
| DEBIDO-PROCESO-001 | `grep -nE 'estado|FIRME|descargo|plazo|subrog' CU26EjecutarAval.java`; búsqueda de consumidores de `aval_ejecutado` | ¿Trigger de firmeza? R-GAR-04 controla tope, no firmeza. **Resiste.** |
| SEG-005 | `sed -n 150,198p CU28EmitirDesembolso.java`; `grep -n Permiso DesembolsosController.java` | ¿Webhook firmado en otro servicio? No hay adaptador de proveedor en entregas; el consumidor de nucleo-financiero recibe el asiento de este endpoint. **Resiste.** |
| DINERO-001 | `grep -A14 'boolean marcarEntregada' EntregaRepositorio.java`; `grep -n monto_efectivamente_entregado restricciones.sql entrega_fondo.sql` | ¿CHECK que ate el monto al neto? No existe. **Resiste.** |
| SEG-006 | `sed -n 84,125p CU28EmitirDesembolso.java`; `sed -n 2560,2580p restricciones.sql` (`fn_des_validar_cuenta_destino`) | ¿El trigger verifica dueño? Solo estado y enfriamiento. **Resiste.** |
| SEG-007 | `sed -n 98,110p CU18RegistrarCuentaDestino.java` | ¿Solo en perfil de prueba? Está en el código principal sin condición de perfil. **Resiste.** |
| DINERO-003 | `grep -n 'Permiso\|public ResponseEntity' EntregasController.java`; `grep -n EFECTIVO_ORGANIZADOR entrega_fondo.sql entregas.yaml` | ¿Endpoint retirado? Sigue con `@Permiso("ENTREGA_EJECUTAR")`. **Resiste.** |
| SEG-008 (S2) | `sed -n 145,176p CU18RegistrarCuentaDestino.java` | ¿Verificación externa? No. **Resiste.** |
| DINERO-002 (S2) | `sed -n 106,111p CU28EmitirDesembolso.java` | **Resiste.** |
| APROB-001/003, ACOPLE-001, CONTRATO-002, DEBIDO-PROCESO-003, DATOS-002, DINERO-010 (S2) | reaperturas listadas en cada ficha (`grep -c REFERENCES …` → 76/25; `ls contratos/pactos/`; `sed -n 2768,2779p`; `sed -n 24,30p 02-politicas.sql`; `sed -n 686,697p garantia.yaml`) | **Resisten.** |
| HUECO-001 (S2) | `grep -rnE 'garantia\.(incumplimiento_cubierto|aval_ejecutado|fondo_devuelto…)'` fuera de garantía → vacío | **Resiste.** |
| FLUJO-QR-001 (S2) | `grep -rln KafkaListener servicios/entregas/src/main` → vacío | **Resiste.** |
| DEFECTO-002, DINERO-004 (código), DINERO-005, DINERO-006, DINERO-007, RESILIENCIA-001/002, SEG-009, TRAZA-001, DEBIDO-PROCESO-002, FLUJO-FALTANTE-001 (S2) | No reabiertos línea a línea o no demostrables sin ejecutar | **Bajados a «probable»/«sospecha»** en su ficha, con la comprobación que falta. |

**Bajas y ajustes por refutación**
- DINERO-006 (doble cobertura por `turnoId`): de S1 (subagente) a **S2 / sospecha**; queda sin efecto si se retira el respaldo.
- RESPALDO-010 (textos públicos): de S1 (subagente) a **S2**, porque no mueve dinero.
- APROB-001 (cinco actos humanos en la entrega): de S1 (subagente) a **S2**, porque es una «aprobación residual que bloquea el flujo nuevo» (§10). Los defectos de dinero de esos pasos van aparte, como S1 (SEG-005, DINERO-001).
- APROB-010 (traspaso por votación): de S2 a **S3**, porque la pantalla pertenece a grupos (L3) y acá solo se registra.
- DATOS-001 → renombrado **DEFECTO-002** (la causa es un defecto de RLS); DUP-INT-001 → **DUP-X-001** (la duplicación es entre servicios).

**Pasada inversa.** Las clases sin hallazgo en las tres primeras tandas (FLUJO-QR, FLUJO-FALTANTE, REESCRITURA, PISADA, PARCHE, FRAGIL, CLEAN, TRAZA, REG) se revisaron en la tanda 4 y dieron un hallazgo cada una. DDD, ARQ, RENDIMIENTO, CONFIG, DUP-INT y OTRO quedaron sin hallazgo propio; qué se miró está en §8 del informe.
