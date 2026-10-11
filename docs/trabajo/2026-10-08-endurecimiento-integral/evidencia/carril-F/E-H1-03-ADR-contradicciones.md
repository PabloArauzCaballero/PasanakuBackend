# E-H1-03 · ADR de contradicciones: garantía, comisiones y autogestión

- **Estado del ADR: PROPUESTO.** Quien aprueba es el dueño del producto (reglas 1–3 y 7 del PLAN §1.1 ya son decisión suya; lo que sigue son las **consecuencias** de aplicarlas y las decisiones comerciales que todavía faltan, ver E-H1-04).
- Fecha: 2026-10-08. Base: PLAN.md §1.1, §1.2, §3.1; INFORME.md (`investigacion/2026-10-07-modelo-financiero/`), secciones «La garantía empresarial del pozo», «Tasas y economía del servicio», «Diferencias con la documentación del proyecto»; código y docs en HEAD `24deb3eb` más el árbol de trabajo sin commitear.
- Peldaño de evidencia: DISCOVERED (lectura de código y documentos). Ningún comportamiento nuevo fue ejecutado por este carril.
- Los importes son **sintéticos**, tomados del ejemplo del INFORME (12 integrantes, Bs 500, pozo Bs 6.000). No son tarifas aprobadas.

## Regla nueva (la que ordena todo)

PLAN §1.1: **(2)** quien está habilitado crea el grupo y queda administrador en la misma operación lógica; **(3)** sin segunda aprobación humana del grupo; **(7)** «recibir mesa significa recibir el pozo completo del turno. La empresa aporta el faltante». PLAN §1.2: la comisión de servicio se cobra **separada del principal** y toda ganancia al organizador sigue prohibida (RN-18).

## ADR-F-01 · Garantía: de fondo mutual con topes a compromiso empresarial reservado

| | Antes (documentado y en HEAD) | Después (regla 7) |
| --- | --- | --- |
| Fuente | `docs/entidades/08_garantia_incumplimiento.md` líneas 55-58: fondo `POR_GRUPO` o `MUTUAL_PLATAFORMA`, alimentado por un porcentaje de cada aporte; topes por evento y por participante (CU-23 doc línea 30) | Compromiso de la empresa financiado y reservado **antes de activar** el grupo (PLAN §1.2) |
| Si falta plata | CU-23 doc líneas 131-133: «fondo sin saldo suficiente → entrega `BLOQUEADA_POR_FONDO_INCOMPLETO`». Código: `servicios/garantia/.../CU23CubrirIncumplimiento.java:90-99` (error `AP-CU23-02` si no hay fondo o política vigente) y `servicios/entregas/.../CU22LiquidarEntrega.java:56-62` (`AP-CU22-01`, bolsa incompleta no se liquida) | La empresa aporta el faltante y la entrega se completa. Un bloqueo por fondo mutual **deja de ser cumplimiento** del «pozo completo» |
| Deuda del incumplidor | Subrogación a favor del fondo; `REPOSICION_FONDO_GARANTIA` se deduce de su entrega futura (CU-23 doc líneas 135-137) | Se conserva como derecho de recuperación; **no cuenta como caja** (INFORME, «Qué debe prometerse y registrarse») |

Ejemplo (sintético): pozo Bs 6.000, entraron Bs 5.000. Antes, con fondo vacío: entrega bloqueada, el beneficiario no cobra. Después: la empresa transfiere Bs 1.000, el beneficiario recibe Bs 6.000, y el incumplidor queda debiendo Bs 1.000 (recuperación futura, no caja).

**Consecuencias financieras.**
1. La exposición cambia de dueño: el riesgo de mora pasa de los participantes (fondo propio del grupo) a la empresa. Un incumplidor del primer turno deja Bs 5.500 de aportes futuros pendientes (c·(N−k) con N=12, k=1).
2. Se necesita **liquidez** (caja a tiempo) además de **capital** (absorber pérdida): INFORME, tabla de estrés, p. ej. 15 grupos afectados → Bs 7.500 de caja adicional por mes del 2 al 12 antes de recuperar. Son escenarios hipotéticos, no probabilidades.
3. El sistema debe rechazar la **activación** de un grupo cuando no hay capacidad reservada; no debe recortar después el pozo de grupos ya comprometidos (INFORME, «Cómo calcular el respaldo necesario»).
4. CU-23 y `08_garantia_incumplimiento.md` no se borran: el fondo mutual sigue siendo un mecanismo distinto; hay que **nombrarlo** como tal y separarlo del respaldo empresarial.
5. Qué NO existe todavía: ni tabla, ni servicio, ni caso de uso de reserva corporativa por ciclo (E-H1-02, F14).

**Trazabilidad:** regla 7 → PLAN §1.2 «Respaldo del pozo» → PLAN H8 (cumplir el pozo) → decisión abierta D-02/D-03 de E-H1-04.

## ADR-F-02 · Comisiones: de deducción del bruto a cargo separado

| | Antes | Después |
| --- | --- | --- |
| Regla | CU-22 doc líneas 35-41: `COMISION_PLATAFORMA` es una de las deducciones; `neto = bruto − total_deducciones` (`R-GRP-02`). `04_entregas_fondo.md:97`: «la bolsa se descuenta para pagar el servicio de la plataforma» | PLAN §1.2: «cobro separado del principal del pozo»; INFORME: «recibir el pozo completo» debe tener significado literal |
| Código | `servicios/entregas/.../dominio/LiquidacionDeEntrega.java` y `CU22LiquidarEntrega.java:53,76-79` (deducciones con tipo y origen) | Sin cambio en HEAD ni en el árbol de trabajo: **la contradicción sigue abierta en código** |
| Organizador | RN-18: no cobra, no custodia (`07_organizador_automatizacion.md:14`, `04_entregas_fondo.md:97`) | **Se mantiene** (PLAN §1.2, última fila) |

**Dos salidas posibles; hoy ninguna está decidida** (INFORME: «Si se prefieren deducciones, será necesario definir el importe bruto y el neto prometido antes de presentar esa oferta»):
- **A. Cargo separado** (recomendado por el INFORME y el PLAN): el beneficiario recibe el principal íntegro (Bs 6.000) y la comisión se liquida como cargo propio, con consentimiento, tarifa congelada y factura (CU-30…CU-33). Costo operativo: el cargo puede quedar impago y hay que definir qué pasa (PLAN §1.2, «tratamiento de incumplimiento de esa tarifa»).
- **B. Deducción con bruto/neto explícitos**: se mantiene CU-22 tal cual, pero la promesa al usuario deja de ser «pozo completo» y pasa a ser «pozo neto de comisión». Incompatible con la regla 7 si se comunica como «recibís Bs 6.000».

**Consecuencias financieras (ilustrativas, sintéticas).** Con la comisión del INFORME de 1 % sobre un volumen de ciclo de Bs 72.000, el ingreso por ciclo es Bs 720. En la salida A ese ingreso se cobra aparte (los Bs 6.000 del principal no cambian); en la B reduce el neto del receptor. El INFORME muestra que a 2 % de probabilidad hipotética de incumplimiento tras el cobro el margen del 1 % ya es negativo (−Bs 76 por ciclo): **el precio y el respaldo empresarial se acoplan**, no son decisiones independientes.

**Trazabilidad:** regla 7 + PLAN §1.2 «Comisión por servicio» → H8/H11 → decisión abierta D-04 de E-H1-04. Reutilizar `servicios/tarifas` (NOTAS.md: no duplicar motor tarifario).

## ADR-F-03 · Autogestión: de «grupo sin organizador» a creador habilitado y administrador

| | Antes | Después |
| --- | --- | --- |
| Regla | CU-20 doc línea 25-26: «el organizador está habilitado … **o el grupo es autogestionado**»; `02_grupos_turnos.md:35-36`: administra una persona habilitada **o el sistema solo (organizador digital)** | Regla 2: el creador habilitado queda administrador del grupo en la misma operación |
| Código HEAD | `servicios/grupos/.../web/MapeoDeAltaDeGrupo.java:38-42`: `organizador.map(afuera::organizadorHabilitado).orElse(true)` (sin organizador = permitido) | Árbol de trabajo (sin commitear): `MapeoDeAltaDeGrupo.java:34-40` exige organizador habilitado resuelto desde la sesión; `CU20CrearGrupo.java:88-89` rechaza organizador ausente o no habilitado (`AP-CU20-02`) |
| Efecto | Cualquier usuario autenticado podía abrir un grupo «de la plataforma» | Un usuario no habilitado por backoffice no crea grupo |

**Consecuencias financieras.** (1) Cada grupo tiene un responsable identificable con habilitación vigente: es la base para exigir KYC reforzado (CU-90 `AP-CU90-06`) antes de administrar plata ajena. (2) No cambia RN-18: administrar sigue sin cobrarse ni custodiar. (3) Los grupos «de organizador digital» dejan de crearse por esta vía; si el negocio los quiere, necesitan una decisión explícita (D-07). (4) **Documentación desalineada:** CU-20 y `02_grupos_turnos.md` siguen diciendo que el grupo puede ser autogestionado; no se corrigieron (fuera de mi propiedad de archivos) y deben reconciliarse junto con el código.

**Trazabilidad:** reglas 2 y 3 → PLAN §3.1 fila CU-20 → H5/H7.

## Registro de ambigüedades (regla 00 §1.7)
| Ambigüedad | Supuesto tomado | A quién confirmar |
| --- | --- | --- |
| ¿Comisión A (cargo separado) o B (deducción)? | Se documenta A como recomendada; ninguna se implementa por inferencia | Dueño de producto + finanzas |
| ¿El fondo mutual sigue existiendo junto al respaldo empresarial? | Sí, nombrado aparte, hasta decisión | Dueño de producto + legal |
| ¿Se permiten grupos sin organizador humano? | No (comportamiento WT) | Dueño de producto |

## No cubierto
- No se ejecutó ninguna prueba. No se verificó que el árbol de trabajo (WT) compile.
- No se editó ningún documento de entidades ni de casos de uso; las contradicciones documentales quedan listadas, no corregidas.
- La viabilidad jurídica del respaldo empresarial (¿garantía, seguro, afianzamiento?) queda en E-H1-04 y en el dossier H15.
