---
tags:
  - arquitectura
  - adr
  - dinero
  - garantia
titulo: "ADR-051 — Garantía del pozo, comisión y autogestión"
estado: aceptada
fecha: 2026-10-11
---

# ADR-051 — Garantía del pozo, comisión y autogestión

> Tres reglas del producto contradecían documentación y código anteriores. Este ADR cierra
> las seis decisiones que la propuesta del carril F4 dejó **abiertas** (D-02, D-03, D-04, D-12,
> D-15, D-16), con **antes / después** y **consecuencias financieras** de cada una.

> **Constancia de aceptación (veraz).** Aceptado por delegación expresa del dueño del proyecto
> (Pablo), 2026-10-11 («lo más recomendado, pero cerralo»); **pendiente de ratificación formal por
> Producto, Finanzas y Legal**. Esta aceptación **no** es una firma de esas áreas ni de ninguna otra
> persona, no aprueba ninguna tarifa ni capital, y **no habilita dinero real**: cada capacidad con
> fondos de terceros sigue condicionada al dossier H15
> (`docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F/H15-dossier-habilitacion-real.md`).
> Los importes de los ejemplos son **sintéticos** (12 integrantes, Bs 500, pozo Bs 6.000).

Origen: propuesta `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F4/E-H1-03-ADR-051-propuesta-garantia-comision-autogestion.md`
(H1.S1.M3), que se conserva como expediente. Hermano de [[ADR-052 Decisiones adoptadas por delegación del dueño|ADR-052]],
que registra las otras ocho decisiones delegadas el mismo día.

## Contexto

La regla vigente del producto (PLAN del endurecimiento §1.1 y §1.2) fija tres cosas:

1. **Recibir mesa es recibir el pozo completo del turno; la empresa aporta el faltante** (regla 7).
2. **Quien está habilitado crea el grupo y queda administrador** en la misma operación, sin segunda
   aprobación humana del grupo (reglas 2 y 3).
3. **La comisión de servicio se cobra separada del principal**, y toda ganancia para quien administra
   el grupo sigue prohibida (RN-18; regla 91.5).

La documentación de entidades y de casos de uso es anterior y dice otra cosa, y el código cambió a
pedazos: conviven **dos caminos de entrega** y **dos mecanismos de cobertura**. Sin un documento que
ordene cuál manda, cada lane resuelve la contradicción a su manera y el dinero del participante depende
del camino que se invoque. La propuesta separó lo que el repositorio ya decidía de lo que quedaba abierto;
este ADR toma las seis aperturas con el criterio que el dueño pidió: **la opción más recomendada y la más
conservadora según lo que el repositorio ya decide**.

## Decisión

Se adoptan, para el modo simulado y como diseño de producción **sujeto a ratificación**, estas seis:

| ID | Pregunta | Decisión adoptada |
|---|---|---|
| D-02 | Naturaleza y exigibilidad del respaldo | El respaldo es un **compromiso de la propia empresa**, financiado con capital propio y **reservado por ciclo antes de activar el grupo**. No se modela como seguro ni como afianzamiento de un tercero. La calificación jurídica definitiva la ratifica Legal. |
| D-03 | Capital, caja y topes | **Un solo camino de cobertura**: la reserva empresarial. Cobertura **todo o nada** por turno; si no hay capacidad **se deniega la activación** y nunca se recorta el pozo de un grupo ya comprometido. Cuánto capital se reserva **no** se decide acá. |
| D-04 | Comisión: cargo separado o deducción | **Un solo camino de entrega: el del pozo completo.** El principal se acredita íntegro; la comisión es un **cargo separado**, con su propia línea. Toda suma que se cobre a quien recibe se muestra **línea por línea**, con su referencia. **Sin comisión del organizador** (RN-18). |
| D-12 | ¿Grupos sin organizador humano? | **No.** Todo grupo lo crea y administra un organizador habilitado por backoffice. El «organizador digital» deja de ser una vía de alta (puede seguir automatizando tareas de un grupo con organizador humano). |
| D-15 | ¿Coexisten fondo mutual y reserva? | **No**: la reserva empresarial es el único respaldo; el fondo mutual (`CU23CubrirIncumplimiento`) queda como **legado** y no cubre faltantes de pozo. La deuda del incumplidor **se conserva** (exposición viva, derecho de recuperación). |
| D-16 | Tope monetario por operación de billetera/QR | **Ningún número nuevo en el código.** Rige el límite por nivel de debida diligencia del catálogo (`limite_operativo_billetera`, hoy `PROVISIONAL`) y, ante duda, **el menor**. El valor definitivo lo confirma Legal/Cumplimiento contra la norma vigente. |

## Motivo

**Por qué estas y no otras.** Cada elección sale de algo que el repositorio ya decide, no de una
inferencia nueva:

- *Un camino de entrega:* `CU22EntregarPozoCompleto` ya implementa la regla 7 («el importe a entregar es
  SIEMPRE el pozo», A6) y es el que tienen cubiertas las pruebas de la tercera vuelta
  (`CU133PozoCompletoTest`, `…RechazosTest`, `…ConcurrenciaTest`). La regla 91.3.4 exige que ninguna
  deducción exista sin su línea.
- *Un camino de cobertura:* `CU23CubrirConRespaldo` ya es todo o nada, idempotente por turno y devuelve
  `INSUFICIENTE` sin cubrir parte (`CU23CubrirConRespaldo.java:87,144`). Dos fuentes para el mismo
  faltante significan que la contabilidad no cierra (la propuesta lo dejó anotado como consecuencia).
- *Sin comisión del organizador y organizador no es caja:* RN-18 y regla 91.5; no hay opción distinta.
- *Sin grupo autogestionado:* `MapeoDeAltaDeGrupo.java:38-44` resuelve el organizador desde la sesión y
  rechaza con `AP-CU20-02`; `CU20CrearGrupo.java:88-90` lo exige de nuevo. Es la regla en dos capas.
- *Sin tope inventado:* la regla 97.5.4 prohíbe inferir parámetros financieros.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| **B para la comisión: deducción del principal con bruto/neto** | Hace que «recibís el pozo completo» deje de ser literal y contradice PLAN §1.2. Se descarta; ver el antes/después abajo. |
| **Mantener las dos entregas hasta que Finanzas elija** | Es lo que dejó la propuesta; el dinero del participante seguía dependiendo de quién configure la operación, no del sistema. |
| **Borrar ya `CU22LiquidarEntrega` y `CU23CubrirIncumplimiento`** | Rompe contratos publicados (regla 98.1.3). Se declaran legado y se retiran por un cambio versionado, no por este ADR. |
| **Dejar coexistir fondo mutual y reserva «nombrados aparte»** | Dos fuentes para un mismo faltante; sin regla de prioridad no hay cuadre. |
| **Mantener «organizador digital» como vía de alta** | Un grupo sin responsable identificable impide exigir verificación reforzada antes de administrar plata ajena. Si Producto lo quiere, es una decisión explícita nueva. |
| **Fijar en el código un tope de Bs/USD por operación** | Sería un parámetro regulatorio inventado (regla 97.5.4); además el informe del modelo financiero remite al reglamento vigente del BCB, que hay que confirmar. |
| **Tratar el respaldo como seguro o afianzamiento** | Exige un tercero (aseguradora/afianzadora) que no existe en el repositorio; sería inventar un proveedor. |

## Consecuencias

### 1. Garantía del pozo (D-02, D-03, D-15) — antes / después

| | Antes (documentado) | Después (decidido) |
|---|---|---|
| Quién respalda | Fondo mutual `POR_GRUPO` o `MUTUAL_PLATAFORMA`, con topes por evento y por participante (`docs/entidades/08_garantia_incumplimiento.md`, `CU-23`) | **Reserva empresarial por ciclo**, pedida antes de activar el grupo: `CU23ReservarRespaldo`, `CU23CubrirConRespaldo` (`servicios/garantia/…/aplicacion/`) |
| Si falta plata | Entrega `BLOQUEADA_POR_FONDO_INCOMPLETO`; el beneficiario no cobra | `CU22EntregarPozoCompleto` pregunta el recaudo a `aportes` y, si falta, la reserva cubre **todo o nada**, con una cobertura viva por turno e idempotente |
| Cobertura insuficiente | El fondo vacío frena la entrega | Resultado `INSUFICIENTE`: no se cubre una parte; se abre incidente y **se conserva la deuda** |
| Deuda del incumplidor | Subrogación a favor del fondo; reposición descontada de su entrega futura | Derecho de recuperación de la empresa (`CU23RecuperarRespaldo`); **no es caja** y **no se descuenta del principal** de la entrega |
| Camino viejo | — | `CU23CubrirIncumplimiento` queda **legado**: no se invoca para faltantes de pozo |

Ejemplo sintético: pozo Bs 6.000, confirmados Bs 5.000, faltante Bs 1.000. Antes, con fondo vacío: la
entrega queda bloqueada. Después: la reserva transfiere Bs 1.000, el beneficiario recibe Bs 6.000 y el
incumplidor queda debiendo Bs 1.000 como recuperación, no como caja.

**Consecuencias financieras.**

- El riesgo de mora **cambia de dueño**: pasa de los participantes a la empresa. Un incumplidor del
  primer turno deja `c · (N − k)` de aportes futuros pendientes (N = 12, k = 1, c = Bs 500: Bs 5.500).
  **Exposición conservada** significa que ese monto sigue registrado como deuda exigible; no se condona
  ni se absorbe como pérdida silenciosa.
- Hace falta **liquidez** (caja a tiempo), no solo capital: la reserva debe estar constituida **antes** de
  activar el grupo. Sin fila de capacidad, el sistema deniega la activación.
- La cobertura es **todo o nada**: un grupo cuya reserva no alcanza no recibe entrega parcial.
- Con una sola fuente de cobertura, el reporte financiero deja de necesitar distinguir dos orígenes para
  el mismo faltante; las coberturas históricas del fondo mutual se conservan, append-only, como legado.
- **Lo que este ADR no resuelve:** el monto de capital, el porcentaje de reserva y la naturaleza jurídica
  final (D-02/D-03 en su parte numérica y legal). La tabla de capacidad sigue **sintética**
  (`apto_produccion = false`).

### 2. Comisión de servicio (D-04) — antes / después

| | Antes (documentado) | Después (decidido) |
|---|---|---|
| Regla | `COMISION_PLATAFORMA` es una **deducción** de la bolsa: `neto = bruto − deducciones` (`CU-22`, `docs/entidades/04_entregas_fondo.md`) | **Cargo separado** del principal: el beneficiario recibe el pozo íntegro y la comisión es una línea propia |
| Línea por línea | Las deducciones se calculaban en un solo importe neto | Cada importe que sale del flujo del beneficiario (comisión, impuesto, costo de transferencia) es **una línea** con tipo, importe y referencia al hecho que la origina (regla 91.3.4) |
| Caminos | `CU22LiquidarEntrega` (deduce) y `CU22EntregarPozoCompleto` (no deduce) publicados a la vez | **Uno solo**: `CU22EntregarPozoCompleto`. `CU22LiquidarEntrega` queda **legado** |
| Organizador | Prohibido cobrar (RN-18) | Sin cambio: **no hay comisión del organizador** ni tránsito de dinero por su cuenta |

Cómo se cobra el cargo (diseño adoptado, **no implementado todavía**; ver «Lo que falta en el código»):

1. El pozo se acredita **completo** al beneficiario (asiento 1: débito cuenta del grupo, crédito billetera
   del beneficiario).
2. El cargo por servicio se asienta como **operación aparte** (asiento 2: débito billetera del beneficiario,
   crédito `PLATAFORMA_INGRESOS`; y el impuesto, si corresponde, a `PLATAFORMA_IMPUESTOS_POR_PAGAR`), con
   su propia clave de idempotencia. El principal nunca se reduce.
3. **Cargo impago.** La entrega **no se bloquea** por el cargo. Si el débito del cargo no puede asentarse en
   la misma operación, queda un **cargo pendiente** (derecho de cobro de la plataforma, con su línea),
   exigible contra el saldo posterior del beneficiario según lo que aceptó en el tarifario congelado.
   Nunca se descuenta del principal después.
4. El comprobante muestra: pozo recibido · cada cargo con su motivo · saldo resultante.

Ejemplo sintético: pozo Bs 6.000. Antes (B): neto Bs 5.982 si la comisión fuera 0,3 % (Bs 18). Después (A):
el beneficiario recibe Bs 6.000 y, aparte, ve «Cargo por servicio Bs 18» con su referencia al tarifario
congelado. El saldo final puede coincidir; lo que cambia es que el principal es literal, el cargo es
reembolsable por su propio camino (CU-33, nota de crédito) y el impago tiene dueño.

**Consecuencias financieras.** Con el 1 % ilustrativo del informe del modelo financiero sobre Bs 72.000
de volumen por ciclo, el ingreso es Bs 720; en A se cobra aparte y los Bs 6.000 no cambian. A 2 % de
incumplimiento hipotético el margen del 1 % ya es negativo: **precio y respaldo empresarial se acoplan** y
se revisan juntos (Finanzas). La tarifa, los impuestos y la factura (CU-32) **no quedan decididos acá**.

### 3. Autogestión (D-12) — antes / después

| | Antes | Después |
|---|---|---|
| Regla | «El organizador está habilitado **o el grupo es autogestionado**» (`CU-20`); «administra una persona habilitada **o el sistema solo**» (`docs/entidades/02_grupos_turnos.md`) | Quien crea el grupo es un organizador **habilitado por backoffice** y queda administrador en la misma operación |
| Efecto | Cualquier usuario autenticado abría un grupo «de la plataforma» | Quien no está habilitado no crea grupos (`AP-CU20-02`); todo grupo tiene un responsable identificable |

**Consecuencias financieras.** Cada grupo tiene un responsable con habilitación vigente, base para exigir
verificación reforzada antes de administrar plata ajena. **No cambia RN-18**: administrar no se cobra ni se
custodia. Los grupos «de organizador digital» **dejan de crearse**; si el negocio los quiere, es una decisión
nueva y explícita.

### 4. Límite por operación (D-16)

Sin un tope fijo en el código. La operación se limita por el catálogo por nivel (`limite_operativo_billetera`,
`seeders/minimos/03-limites-operativos.json`, marcado `PROVISIONAL`). **Consecuencia:** mientras Legal no
confirme el instrumento y el salario vigente, ninguna operación de billetera/QR con dinero real queda
habilitada por este ADR.

### Lo que falta en el código (no lo hace este ADR; lo hace el dueño de cada servicio)

| Pieza | Dueño | Estado |
|---|---|---|
| Cargo de comisión como operación separada tras el pozo completo (hoy `CU22EntregarPozoCompleto` no cobra comisión) + cargo pendiente | `entregas` + `tarifas` + `nucleo-financiero` | **No implementado** |
| Retirar de producción `CU22LiquidarEntrega` y `CU23CubrirIncumplimiento` (marcar legado, versionar el retiro) | `entregas`, `garantia` | **No implementado**; sigue publicado |
| Reconciliar el seed `seeders/minimos/04-tarifario.json` («deducido de la entrega», «Se descuenta de la bolsa…») con el cargo separado | `tarifas` (fuera de `docs/`) | **Pendiente**: el seed todavía dice «deducido» |
| Prueba que cierre la comisión (hoy no hay) | `entregas` | **Pendiente** |
| La columna `es_autogestionado` y el comentario del trigger R-ORG-02 (`docs/Restricciones.md`, «grupo autogestionado: no hay organizador que deba contrato») siguen en el modelo; retirarlos es un cambio de esquema | lane de modelo/esquema | **Pendiente**; este ADR no toca `docs/entidades/*.puml` |

## Cómo se verifica

- Garantía: `CU133PozoCompletoTest`, `CU133PozoCompletoRechazosTest`, `CU133PozoCompletoConcurrenciaTest`
  (`servicios/entregas`) y `CU131CoberturaTest`, `CU131ConcurrenciaTest`, `CU131RecuperacionTest`
  (`servicios/garantia`): todo o nada, una cobertura viva por turno, recuperación sin caja.
- Autogestión: rechazo `AP-CU20-02` en `servicios/grupos` sin organizador habilitado.
- Comisión: **no hay prueba que cierre D-04 todavía**; la decisión queda registrada y la prueba pertenece al
  trabajo de implementación de arriba. Mientras no exista, el estado de la comisión es «decidida, sin código».
- Documentos: `python scripts/verificar_boveda.py` (el ADR está indexado, tiene las 6 secciones y estado
  válido) y `python scripts/verificar_criterios.py` terminan con RC 0.

Esta aceptación **no ejecutó pruebas del backend**: las referencias de código son lecturas del repositorio en
el HEAD de trabajo; la evidencia de ejecución está en los reportes de H8.S1.M3 y M4.

## Ratificación pendiente

| Decisión | Quién ratifica | Qué ratifica exactamente |
|---|---|---|
| D-02 | Dirección + Legal + Finanzas | Calificación jurídica del respaldo y su exigibilidad frente al participante |
| D-03 | Finanzas + órgano de gobierno | Capital, caja y tope agregado de cobertura; el número, no la regla |
| D-04 | Producto + Finanzas + área tributaria | Tarifa, impuestos, factura y tratamiento del cargo pendiente |
| D-12 | Producto | Que no existan grupos sin organizador humano |
| D-15 | Producto + Finanzas | El retiro definitivo del fondo mutual y qué pasa con sus coberturas históricas |
| D-16 | Legal + Cumplimiento | Tope por operación contra la norma vigente |

No se nombra a ninguna persona: el PLAN §12 propone áreas, y ninguna fue designada ni consultada.

## Referencias

- `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F4/E-H1-03-ADR-051-propuesta-garantia-comision-autogestion.md`
  (propuesta), `…/carril-F/E-H1-04-decisiones-comerciales-abiertas.md` y `…/carril-F/H15-dossier-habilitacion-real.md`.
- [[ADR-049 Doble aprobación de retiros|ADR-049]] — el mismo estilo de decisión con dinero.
- [[ADR-039 Sin efectivo · la plataforma no opera dinero físico|ADR-039]] — la plataforma no opera dinero físico.
- [[CU-20 Crear grupo y congelar tarifario]] · [[CU-22 Liquidar y entregar el fondo]] · [[CU-23 Cubrir un incumplimiento con el fondo]].
