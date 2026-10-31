# Reporte — El sorteo exige la primera cuota pagada (R-GRP-20): lado `aportes` + precondición en `grupos`

> **AVANCE: 13 / 13 — 100 %.**

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/sorteo-primera-cuota-20261030` desde `origin/test` (worktree `Pasanaku-wt-sorteo`)
- Peldaño de evidencia alcanzado: **`TESTED` + regresión de módulo** en los dos servicios (`grupos` 513/513, `aportes` 236/236, contra PostgreSQL real con Testcontainers y dobles declarados para lo remoto). **No es `VERIFIED`**: el camino real Kafka → `aportes` y `grupos` → HTTP → `aportes` con los dos procesos levantados no se ejercitó, y el evento que dispara todo lo emite el carril de `pasanaku-c1`, que todavía no lo publicó. Ver «No cubierto».
- Carril hermano: `pasanaku-c1` (`iniciarGrupo`, emisión del evento, `puedeSortear`, app). Coordinación en `evidencia/coordinacion-c1.md`.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `CU21AbrirPrimeraCuota`: al llegar `grupos.grupo_iniciado` abre la obligación `APORTE_PERIODICO`/`PENDIENTE` de la cuota 1 por cupo, reutilizando `RegistroDelCalendario`; dedup por `evento_consumido` en la misma transacción | `./gradlew :servicios:aportes:integrationTest --tests '*CU21PrimeraCuotaTest'` | PASS 8/8 · `evidencia/aportes-resultados-xml.txt` |
| H1.S1.M2 | `ConsumidorDeGrupoIniciadoKafka` (tema `aportaya.grupos.grupo_iniciado`, flag `GRUPO_INICIADO_CONSUMO_HABILITADO`, por omisión encendido) con parseo estricto del sobre | `./gradlew :servicios:aportes:test --tests '*ConsumidorDeGrupoIniciadoKafkaTest'` | PASS 3/3 · ídem |
| H1.S1.M3 | Carga inválida (sin cupos, aporte en cero, período ≠ 1) rechazada sin tocar la base ni marcarse consumida | casos «invalido*» de `CU21PrimeraCuotaTest` | PASS 3/3 · ídem |
| H2.S1.M1 | `aportes.yaml`: `GET /aportes/grupos/{grupoId}/primera-cuota` (`consultarPrimeraCuota`, schema `PrimeraCuota`, `additionalProperties: false`) | `./gradlew :servicios:aportes:compileJava` | PASS · `evidencia/aportes-suite-2.txt` |
| H2.S1.M2 | `PrimeraCuotaRepositorio` + `ConsultarPrimeraCuota` + método del controlador (`BILLETERA_VER`); el record vive en `dominio/PrimeraCuotaDelGrupo` por la regla de arquitectura web ↛ infraestructura | casos «consultaTrasUnCobro» y «consultaSinObligaciones» + `ArquitecturaTest` | PASS · `evidencia/aportes-resultados-xml.txt` |
| H2.S1.M3 | `PrimeraCuotaWebTest`: 200 organizador, 200 vacío, 401 sin sesión, 403 sin permiso, 400 id inválido | `./gradlew :servicios:aportes:webTest --tests '*PrimeraCuotaWebTest'` | PASS 5/5 · ídem |
| H3.S1.M1 | Puerto `PrimerAporteDelCupo` + adaptador `PrimerAportePorHttp` (`ClienteConCircuito`, fallo cerrado) | `./gradlew :servicios:grupos:test --tests '*PrimerAportePorHttpTest'` | PASS 6/6 · `evidencia/grupos-resultados-xml.txt` |
| H3.S1.M2 | `CU60Sortear.comprometer`: consulta a `aportes` **antes** de la transacción; `AP-CU60-11` con `detalle.cuposPendientes`, `AP-CU60-12` si `aportes` no responde | `CU60Test` «rechazaSinPrimeraCuota», «rechazaSinRespuestaDeAportes» | PASS · `evidencia/grupos-suite-2.txt` |
| H3.S1.M3 | `BaseDeCU60` con doble de `aportes` («todos pagaron» por omisión); la suite previa de CU-60 intacta | `./gradlew :servicios:grupos:integrationTest --tests 'bo.aportaya.grupos.CU60*'` | PASS 26/26 (`CU60Test` 16, `CU60SnapshotTest` 10) |
| H3.S1.M4 | `AP-CU60-05` mira la versión **vigente** del reglamento (`AceptacionVigenteRepositorio`, `detalle.cuposSinFirma`) | `CU60Test` «aceptaFirmaDeLaVersionVigente», «rechazaFirmaDeVersionAnterior» | PASS · `evidencia/grupos-suite-2.txt` |
| H4.S1.M1 | CU-60 documenta `AP-CU60-05/11/12`; README de `aportes` documenta consumidor y endpoint | `grep -c "AP-CU60-1[12]" "docs/CasosDeUso/CU-60 Sortear los turnos.md"` | PASS → 4 |
| H4.S1.M2 | Contrato del evento acordado y confirmado por `pasanaku-c1` | `SendMessage` | PASS · `evidencia/coordinacion-c1.md` |
| H4.S1.M3 | Este reporte | — | en disco |

**Regresión de módulo (regla 80.1.6) y gate de formato:**

```text
$ ./gradlew :servicios:grupos:spotlessCheck :servicios:grupos:test :servicios:grupos:webTest :servicios:grupos:integrationTest
BUILD SUCCESSFUL in 2m 45s · exit=0 · grupos XML: tests=513 fallos+errores=0        (evidencia/grupos-regresion-2.txt)

$ ./gradlew :servicios:aportes:spotlessApply :servicios:aportes:spotlessCheck :servicios:aportes:test :servicios:aportes:webTest :servicios:aportes:integrationTest
BUILD SUCCESSFUL in 44s · exit=0 · aportes XML: tests=236 fallos+errores=0          (evidencia/aportes-regresion-3.txt)
```

## A medias

ninguna

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Emisión de `grupos.grupo_iniciado` por outbox en `CU69IniciarGrupo` | **De `pasanaku-c1`** (H3 de su plan), confirmado | Que c1 lo publique; sin el evento, ningún grupo llega a tener la cuota 1 y el sorteo responde `AP-CU60-11` siempre |
| `puedeSortear` en el detalle del grupo y botón/re-firma en la app | De `pasanaku-c1` | ídem |
| Abrir el período 1 (`PROGRAMADO → ABIERTO`) para que `CU21CobrarAporte` acepte el cobro de la cuota 1 | `DECISION_REQUIRED` si cambia una regla; c1 dijo que lo verifica y, si hace falta, se lo pregunta a Pablo | Decisión de Pablo |
| PR a `dev` y `test` | TODO | Integrar junto con la rama de c1 (toca `CU60Sortear` del lado de ellos no, pero sí `grupos.yaml` y el evento); un PR solo de esta rama deja el sorteo bloqueado en `AP-CU60-11` hasta que llegue el evento |
| Comentario del 422 en `grupos.yaml` (`AP-CU60-11/12`) | De `pasanaku-c1` (el archivo es suyo) | — |

## Evidencia

- `evidencia/aportes-suite-1.txt` — primera corrida: `ArquitecturaTest` rojo por web → infraestructura (causa del movimiento del record a `dominio`).
- `evidencia/aportes-suite-2.txt` — corrida dirigida en verde.
- `evidencia/aportes-resultados-xml.txt` — conteos por clase desde los XML de JUnit (8 + 5 + 3 + 5).
- `evidencia/grupos-suite-1.txt` — 24/25: el caso de versión del reglamento no lanzaba (fixture sin participantes).
- `evidencia/grupos-suite-2.txt` + `grupos-resultados-xml.txt` — 32/32 dirigidos.
- `evidencia/grupos-regresion.txt` + `grupos-arranque-causa.txt` — regresión 513 con 3 fallos de arranque: «No default constructor found» en `PrimerAportePorHttp` (dos constructores).
- `evidencia/grupos-regresion-2.txt` — 513/513 tras `@Autowired`.
- `evidencia/aportes-regresion.txt` + `aportes-webtest-causa.txt` — 33 fallos en slice tests: `ConsultarPrimeraCuota` sin doble.
- `evidencia/aportes-regresion-2.txt` (23 fallos, parche parcial) y `aportes-regresion-3.txt` (236/236).
- `evidencia/coordinacion-c1.md` — reparto, contrato del evento y decisiones de Pablo recibidas vía c1.

## No cubierto

- **Kafka real**: el consumidor se probó por su `leer()` y su caso de uso; nadie publicó un mensaje real en `aportaya.grupos.grupo_iniciado` (el productor no existe todavía). Como el README ya dice de su hermano: «NO verificado con Kafka y los dos procesos reales».
- **HTTP real entre `grupos` y `aportes`**: el adaptador se probó contra un servidor del JDK con el cuerpo del contrato; no contra `aportes` levantado. Regla 98.8.4 («apagar el contenedor cuenta; mockearlo no»): no se hizo.
- **Traza `correlationId` cruzando servicios** (98.8.5): no se observó.
- **Prueba de contrato productor ↔ consumidor** (98.1.4): no hay pacto en `contratos/pactos/` para `grupo_iniciado` ni para `primera-cuota`; se definió el contrato en el plan y c1 lo confirmó por mensaje, no por test.
- Autorización a nivel de objeto en `GET /primera-cuota`: `BILLETERA_VER` sin chequeo de pertenencia al grupo (igual que `contarMorosos`); solo expone UUIDs de cupo.
- `scripts/generar_compose.py` y `docker-compose.coolify.yml` no declaran `GRUPO_INICIADO_CONSUMO_HABILITADO`; el flag nace encendido por `matchIfMissing`.
- Los dos commits de docs del trabajo anterior (`R-GRP-20`, análisis de comisión) se trajeron por cherry-pick; su contenido no cambió.

## Desvíos del plan

- El record de respuesta de la consulta se movió de `infraestructura` a `dominio` (`PrimeraCuotaDelGrupo`) porque `ArquitecturaTest` prohíbe web → infraestructura. No estaba previsto.
- `PrimerAportePorHttp` necesitó `@Autowired` explícito (dos constructores) — detectado por la regresión, no por el test dirigido.
- Seis `WebTest` existentes de `AportesController` recibieron el doble `ConsultarPrimeraCuota` (el controlador ganó una dependencia). Cambio mínimo, en la forma del precedente `ConsultarRecaudoDiario`.
- `CU60Test`: el caso de versión del reglamento se reescribió en dos (vigente → procede; cerrada → `AP-CU60-05`) sembrando `participantesConCupo` + `reglamento_grupo` + `aceptacion_reglamento`, porque la fixtura `cuposOcupados` no asigna participante.
- Pablo pidió lanzar varios agentes en paralelo; la regla 70.1.2 fija un subagente simultáneo salvo autorización del `CLAUDE.md` del proyecto (no la hay). Se usó un solo agente de descubrimiento y paralelismo de llamadas propias. Conflicto registrado (regla 00).

## Riesgos residuales

- **Hallazgo fuera de alcance, no corregido**: `SorteoRepositorio.todosAceptaronElReglamento` aceptaba cualquier aceptación y los tests previos de CU-60 lo pasaban en vacío (cupos sin participante). CU-60 ya no lo usa; el método sigue existiendo por si otro lo usa (c1 avisado).
- Hasta que c1 emita el evento, un grupo iniciado **no puede sortear** (`AP-CU60-11` con todos los cupos). Es el comportamiento pedido (plata prometida no habilita), pero en TEST hoy no hay productor: desplegar esta rama sola bloquea el sorteo.
- Si el calendario de feriados del año del vencimiento no está cargado, el consumidor lanza `AP-CU21-09` y Kafka reintenta hasta agotar: la cuota 1 no se abre y nadie se entera salvo por la bitácora. Mismo riesgo que CU-21 hoy.
- «Período 1» en `aportes` = obligación con menor `fecha_vencimiento`; si algún día se abre un período fuera de orden, la consulta apuntaría mal. Alternativa documentada en el plan: que `grupos` pase el `periodoId`.

## Decisiones y ambigüedades

| Qué | Decidido por | Estado |
|---|---|---|
| «Acreditada» = obligación `PAGADO` (la obligación no tiene estado `ACREDITADO`) | Supuesto de este trabajo | Confirmar: Pablo |
| El evento trae todo (período, cupos, montos) en vez de credencial de servicio | Este trabajo, por el hallazgo del agente (el consumidor no tiene token) | Confirmado por c1 |
| `BILLETERA_VER` para la consulta (lo tienen ORGANIZADOR y PARTICIPANTE) | Este trabajo, precedente `contarMorosos` | Revisar en gate `authz` |
| `AP-CU60-12` para «aportes sin respuesta» (fallo cerrado) | Este trabajo | Adoptado por c1 (usa 13+) |
| La primera cuota nace en `iniciarGrupo`; versión N+1 del reglamento con re-firma | Pablo, vía c1 | Implementado del lado del sorteo; emisión pendiente en c1 |
| Un solo subagente pese al pedido de paralelismo | Regla 70.1.2 | Registrado como conflicto |
