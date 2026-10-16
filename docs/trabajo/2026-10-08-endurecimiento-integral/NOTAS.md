# Contexto reanudable — implementación de los tres planes

Solicitud del usuario: implementar completamente los tres planes guardados en Descargas. No se autoriza simular un cierre ni reducir el alcance. Las capacidades de dinero real requieren evidencia jurídica/comercial externa; no inventarla. Continuar con todos los cortes técnicos y accesorios.

## Bases

- Backend: rama `pablo/troncal/security-cves-f04`, HEAD `24deb3ebc31290bd378cdcab77fbb1a1e3bfece0`, inicialmente limpio.
- Frontend: rama `codex/mobile-design-handoff-2026-10-07`, HEAD `f744b719e9c066e2c33bc58a7e8aea73bcde00e1`. Cambios preexistentes: cuatro capturas `apps/web/capturas/catalogo-*`, `apps/web/public/legal/estado-regulatorio.md`, `.claude/hooks/`, `.claude/rules/`, ejemplo `packages/simulado/ejemplos/identidad/verContenidoDeFoto.json`. Preservar.
- Planes copiados a `docs/trabajo/2026-10-08-endurecimiento-integral/PLAN.md` de ambos repos antes del código; originales intactos en Descargas.
- Java 21, Python 3.14, Node/Yarn y Docker disponibles. Flutter no aparece en PATH; existe `.dart_tool` y build móvil, buscar SDK antes de asumir ausencia.
- PostgreSQL existente `aportaya-postgres` en 5433; otros contenedores ajenos no tocar. Testcontainers crea su propio PostgreSQL 16 aplicando `sql/aplicar.sql`. No ejecutar reconstrucción destructiva sobre base compartida.
- Gradle jOOQ por defecto conecta 127.0.0.1:5433; generación estaba vigente. `gradlew.bat` funciona.

## Reglas y skills aplicadas

Leído `PasanakuPromptManager/AGENTS.md`, router, reglas 91/98, control de recursos, skills de disciplina inicial, dinero, privacidad, arquitectura, Python, orquestación y causa raíz. No existe CLAUDE.md raíz en los dos repos principales.

Un único runner pesado a la vez. Subagente único permitido para búsquedas amplias por instrucción explícita de `context-thrift` y después lote aislado por `agent-orchestration`. Agente `/root/inventario_principal` finalizó inventario y está implementando únicamente C2 en `C:/Users/Usuario/Downloads/Pasanaku-Laboratorio-Financiero`. No ejecuta suites ni toca repos. Principal debe revisar y verificar su entrega. C3 aún no iniciado; se puede asignar después al mismo agente, sin dos agentes simultáneos.

## Brechas confirmadas por inspección

- CU68Postular registra solicitud y puntaje; rechaza automáticamente por reputación/morosidad. CU68AceptarIngreso es stub no conectado. Falta proposta admin, resolución BO humana y activación correlacionada.
- CU20CrearGrupo permite organizador opcional tomado del cliente. MapeoDeAltaDeGrupo permite autogestión. Debe vincular sesión a organizador habilitado y asignarlo creador/admin.
- CU90PostularOrganizador tiene proceso humano y evita autoaprobación; BO necesita bandeja accionable completa.
- Invitación: identidad emite token 32 bytes; grupos conserva tokenId pero no entrega un secreto utilizable. Aceptación de invitación por ID no comprueba vencimiento/destinatario/secreto completamente. Requiere consumo seguro y solicitud posterior, no membresía directa.
- Sorteo CU60Sortear existe con compromiso/revelación, pero consulta roster nuevo al revelar. Congelar snapshot y evitar manipulación por cambio.
- CU22LiquidarEntrega deduce del principal y bloquea bolsa incompleta. CU23CubrirIncumplimiento usa fondo mutual limitado. Falta respaldo corporativo reservado por ciclo.
- Mercado F10–F13: solo permuta/traspaso, falta oferta/precio/reserva/liquidación del derecho.
- F16/F17/F20/F21: no se encontró módulo DPF/fondo/posiciones/rescate/intereses/comisión de éxito en fuentes productivas inspeccionadas.
- F19 reutilizar tarifas CU30–36, no duplicar motor tarifario.
- F18 transferencia por alias/grupo existe, QR de usuario requiere implementación.
- Mock frontend Prism es contractual sin estado; script `humo` oculta fallos mediante `|| true`.
- Fuente SQL es PlantUML en `docs/entidades/*.puml` + `scripts/modelo.py` y `generar_ddl.py`; no editar solo SQL generado ni usar Flyway inventado.

## Primer corte realizado: recarga y proveedor externo simulado

Archivos nuevos en `herramientas/proveedor_simulado`: modelo SQLite, servidor HTTP loopback autenticado con claves distintas operación/control, recibos HMAC, persistencia, reloj, idempotencia, respuesta perdida, README y once pruebas Python. El proveedor acepta tipos adicionales como vocabulario; no afirmar que ya estén cableados esos negocios.

Archivos nuevos de núcleo: `ProveedorDeRecargas` (puerto), `RecargasProveedorSimulado` (HttpClient con timeout y verificación de firma/referencia/tipo/frescura; modo deshabilitado por defecto y rechazo de simulación en producción), `RecargasConProveedor` (coordina cotización y proveedor fuera de transacción).

`BilleteraController` usa coordinador al solicitar/confirmar. El costo lo resuelve tarifas; campo de costo enviado por usuario no determina el cargo. CU10 requiere titular para solicitar/consultar/acreditar, confirma importe y referencia, recibe confirmación explícita, bloquea orden y cuenta, compara solicitud reutilizada, conserva referencia externa y puede recuperar respuesta original del movimiento para reintentos. `OrdenRecargaRepositorio` expone bloqueo y campos existentes transaccion_id/referencia_externa.

Tests existentes CU10 conservan aserciones; actualizados los contextos para usar titular real del fixture y confirmaciones explícitas de prueba. Nuevo `ConfirmacionesDePrueba`, nuevo CU10ProveedorTest y adaptador HTTP test. No se borró ningún test ni se redujo aserción.

## Evidencia obtenida

- `python -m unittest herramientas.proveedor_simulado.test_proveedor -v`: 11 tests, OK, 0.755s.
- `gradlew.bat :servicios:grupos:test`: BUILD SUCCESSFUL pero UP-TO-DATE; solo baseline de infraestructura, no nueva ejecución demostrada.
- `gradlew.bat :servicios:nucleo-financiero:integrationTest --tests '*CU10*'`: BUILD SUCCESSFUL, 34s. XML guardado resumido en `evidencia/recargas-integracion-baseline.json`: CU10Test 13, CU10RechazosTest 9, CU10ConcurrenciaTest 1; 23/23 sin fallos.
- `gradlew.bat :servicios:nucleo-financiero:spotlessApply :servicios:nucleo-financiero:test --tests '*RecargasProveedorSimuladoTest' :servicios:nucleo-financiero:webTest --no-parallel`: BUILD SUCCESSFUL, 26s. Contar XML para evidencia exacta del HTTP y web.
- Nueva suite CU10ProveedorTest primero no compiló porque constructor TransactionInterceptor estaba deprecado y el proyecto usa -Werror. Corregido con constructor vacío + setters. No se suprimió warning.
- En curso al escribir esta nota: ejecución de CU10ProveedorTest tras esa corrección; leer la sesión de herramienta antes de continuar con otro runner.

## Próximos pasos del corte de recargas

1. Leer resultado CU10ProveedorTest y corregir causa si falla.
2. Añadir pruebas adversas de entrada al mock (tipo/escenario no textual, cuerpo desmesurado) y guardas faltantes; no permitir TypeError sin respuesta controlada.
3. Completar guardas y comparación de idempotencia de instrumento/medio cuando corresponda; revisar retención fiscal/costo antes de afirmar cumplimiento integral.
4. Verificar checkstyle/arquitectura del módulo; CU10 creció cerca del tope de tamaño, dividir consulta si el gate lo exige sin reducir controles.
5. Ejercitar servidor Python + cliente/backend de verdad con escenario persistente y documentación de configuración. No hay aún E2E de toda la app ni webhook de entrada propio; la confirmación se obtiene de respuesta externa firmada consultada por servidor.
6. Continuar siguientes cortes principal: habilitación/creador y admisión humana; tokens; reserva corporativa/entrega; mercado; inversiones; UI y gates integrales. El corte de recarga no completa el plan principal.

## Progreso

### Corte H5: frontera de admisión humana

Autorizado por la solicitud de implementación completa y por la regla explícita del usuario: manda backoffice. Agregado: solicitud_ingreso con propuesta administrativa y resolución humana separadas. Dueño: grupos. Transacción local: bloquear grupo y solicitud, verificar actor/versión, insertar decisión inmutable, actualizar solicitud y reservar cupos si se aprueba; emitir outbox en el mismo commit. Hechos remotos se consultan fuera de la transacción; ninguna escritura cruzada. Fallo local revierte todo; rechazo humano es resultado persistido, no excepción que lo revierta. Reintento exige misma clave y contenido. Pruebas: actor ajeno, autoaprobación, propuesta obsoleta, decisión contraria al algoritmo, cupo concurrente, historial inmutable.

Recargas: prueba nueva detectó saldo histórico cero; corregido en fuente docs/Restricciones.md mediante BEFORE INSERT bajo bloqueo y regeneración SQL. Las 29 pruebas CU10 pasaron después del arreglo; proveedor Python 12/12. No se reescribieron movimientos históricos existentes.

Los planes aún mantienen microtareas TODO hasta mapear DoD completos. No informar porcentajes estimados. C1 tiene 86 microtareas; C2 y C3 34 cada uno. La implementación técnica total continúa activa.


### Actualizaci?n: creaci?n, autoridad humana y permisos

Creaci?n: CU20 exige organizador habilitado resuelto desde sesi?n v?a contrato organizador; registra alta_grupo inmutable e idempotente y participante creador es_organizador en la misma transacci?n. Se probaron reintento, cambio de payload y rol real svc_grupos. SQL fuente corrige funci?n de contrato con referencia cualificada y SECURITY DEFINER de alcance limitado, y RLS para que organizador lea su habilitaci?n y admin gestione solicitudes propias.

CU68: algoritmo deja reputaci?n/concentraci?n como recomendaciones. Propuesta admin y resoluci?n BO usan bloqueo, revisi?n esperada, clave idempotente y motivo; BO puede contradecir admin/algoritmo; no autoaprobaci?n. Aceptaci?n crea participante pendiente de firma y reserva cupos. decision_ingreso y alta_grupo append-only. Nueve pruebas CU68AdmisionTest y pruebas CU20/68 anteriores pasaron, junto con web. Barrido de grupos, organizador y n?cleo financiero pas? tras extraer rutas del controlador (l?mite de 300 l?neas).

Evidencias: grupos-permisos-verificacion.log, grupos-organizador-verificacion.log, alta-permisos-barrido.log (integraci?n pas?, luego barrido detect? tama?o), barrido-web.log (correcci?n verificada). No existe tarea checkstyleMain en este build; se usa testBarrido.

### Actualizaci?n: invitaciones

Frontera: identidad posee token_verificacion y nuevo alcance_invitacion; grupos posee invitaci?n y solicitud. Nonce de 32 bytes, secreto HMAC con clave externa INVITACIONES_CLAVE, hash protegido y recuperaci?n del mismo enlace sin texto plano persistido. Scope grupo/tel?fono/emisor, clave de emisi?n vinculada a payload. Consumo exige sesi?n del tel?fono, grupo, secreto, vigencia, intentos y bloqueo at?mico; la misma clave recupera recibo, otra clave no repite. Revocaci?n por emisor. Emisi?n/canje remotos fuera de TX de grupos; CanjearInvitacion confirma aceptaci?n + postulaci?n + outbox en una TX local, conservando recibo para recuperar ca?das. No activa membres?a. Nuevos contratos canje y revocaci?n. No se ha implementado UI de enlaces todav?a.

Primera verificaci?n: 9 pruebas identidad y 14 CU69 grupos pasaron. Nuevo CU69CanjeTest: 6 pruebas pasaron (rollback, recuperaci?n, concurrencia real, rol servicio, sin membres?a). Tras a?adir instante de consumo a recibo, pruebas antiguas detectaron fixture con nanosegundos frente al reloj de microsegundos; fixture corregida para usar Reloj, sin cambiar aserciones. Runner de gates en curso: invitacion-gates.log. Pendientes en este corte: verificar web/barrido y ampliar l?mite de emisi?n/auditor?a del token; KYC real en mapeo sigue pendiente.

### Carriles accesorios

C2 ra?z verific? 40 pruebas, 14 fixtures, CLI run/compare, copia aislada sin red y revisi?n en Chromium desktop/mobile/gr?fico. Evidencia en Downloads/Pasanaku-Laboratorio-Financiero/docs/trabajo/2026-10-08-laboratorio-financiero/VALIDACION.md. 34 tareas HECHO en copia de plan; originales Downloads preservados.

C3 agente termin? implementaci?n escrita en Downloads/Pasanaku-Kit-Evidencias-Operacion. Ra?z ejecut? suite: 57 descubiertas, 56 pasaron, 1 omitida por falta de permiso Windows para crear symlink. CLI correcto=3 (origen no verificable), conjunto=2 (defectos sembrados), paquete=0. Pendientes copia aislada, inspecci?n visual y acta de escritorio; no inventar taller humano. Agente finaliz?, no tiene proceso pendiente.

### Cierre 2026-10-08 (tras el corte por falta de RAM)

El estado vigente esta en `REPORTE.md` y en `PLAN.md` (35/86 microtareas HECHO, calculado con `python .claude/hooks/plan_status.py`). Los proximos pasos 1 a 5 del corte de recargas quedaron cerrados por el carril A (nucleo-financiero 40/25/280/2 y proveedor Python 27/27). El paso 6 (siguientes cortes) lo cubrieron los carriles B a E, con las salvedades de `REPORTE.md` (dobles sin productor real en nucleo, mock sin backend integrado, H15/H16 bloqueados por evidencia externa). Regla de recursos para seguir: todo proceso pesado va por `bash C:/Users/Usuario/.claude/ram-guard/heavy.sh <comando>` (un solo proceso pesado a la vez, >=3500 MB libres).
