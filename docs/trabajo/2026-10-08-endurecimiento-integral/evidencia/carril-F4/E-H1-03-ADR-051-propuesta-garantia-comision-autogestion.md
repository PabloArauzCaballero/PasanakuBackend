---
tags:
  - arquitectura
  - adr
  - dinero
  - garantia
titulo: "ADR-051 — Garantía del pozo, comisión y autogestión: de la contradicción a lo decidido"
estado: propuesta
fecha: 2026-10-10
---

# ADR-051 — Garantía del pozo, comisión y autogestión: de la contradicción a lo decidido

> Tres reglas nuevas del producto contradicen documentación y código anteriores. Este ADR
> pone **antes / después** de cada una, nombra las **consecuencias financieras** y separa lo
> que el repositorio **ya decide** de lo que es **decisión de negocio** y sigue **abierto**.

> **Dónde vive.** Queda acá, como propuesta, y NO en `docs/Arquitectura/`: `verificar_boveda.py` exige que
> toda decisión de esa carpeta esté `aceptada` (y cuenta los ADR en `planes/00 Plan maestro.md`). Cuando el
> dueño de producto lo acepte, se promueve como `ADR-051` con ese estado, su fila en `_Arquitectura.md` y la
> cifra del plan maestro actualizada.

Carril F4 del endurecimiento (H1.S1.M3). Estado **propuesta**: quien lo aprueba es el dueño de
producto con finanzas y legal. No se inventó ningún parámetro: los importes de los ejemplos son
**sintéticos** (12 integrantes, Bs 500, pozo Bs 6.000) y no son una tarifa aprobada.

## Contexto

La regla vigente del producto (PLAN del endurecimiento §1.1 y §1.2) fija tres cosas:

1. **Recibir mesa es recibir el pozo completo del turno; la empresa aporta el faltante** (regla 7).
2. **Quien está habilitado crea el grupo y queda administrador** en la misma operación, sin segunda
   aprobación humana del grupo (reglas 2 y 3).
3. **La comisión de servicio se cobra separada del principal**, y toda ganancia para quien
   administra el grupo sigue prohibida (RN-18).

La documentación de entidades y de casos de uso es anterior y dice otra cosa. El código cambió a
pedazos: hay caminos nuevos y caminos viejos que **conviven**. Eso es lo que hay que dejar a la vista.

## Decisión

Se propone **dejar por escrito** lo que el repositorio ya hace —pozo completo con reserva empresarial por
ciclo, entrega sin deducciones sobre el principal en ese camino, y creador de grupo = organizador
habilitado— y **no decidir** lo comercial (§4). Ningún cambio de código sale de este ADR.

## Motivo

La documentación y el código dicen cosas distintas sobre quién respalda el pozo, si la comisión se
descuenta y quién puede abrir un grupo. Sin un documento que lo ordene, cada lane resuelve la
contradicción a su manera y el dinero del participante depende de qué camino se invoque.

## Alternativas descartadas

- **Borrar el camino viejo ya** (`CU22LiquidarEntrega`, `CU23CubrirIncumplimiento`): rompe contratos
  publicados (regla 98.1) y decide D-04 y D-15 por inferencia.
- **Elegir A o B para la comisión en este ADR**: es decisión de negocio y fiscal; no se infiere.

## 1. Garantía del pozo

| | Antes (documentado) | Después (lo que el código ya hace) |
|---|---|---|
| Quién respalda | Fondo mutual `POR_GRUPO` o `MUTUAL_PLATAFORMA`, con topes por evento y por participante (`docs/entidades/08_garantia_incumplimiento.md`, `CU-23`) | Reserva **empresarial por ciclo**, pedida antes de activar el grupo: `CU23ReservarRespaldo`, `CU23CubrirConRespaldo` (`servicios/garantia/.../aplicacion/`) |
| Si falta plata | Entrega `BLOQUEADA_POR_FONDO_INCOMPLETO`; el beneficiario no cobra | `CU22EntregarPozoCompleto.fondear` (`servicios/entregas/.../aplicacion/CU22EntregarPozoCompleto.java`) pregunta el recaudo a `aportes` y, si falta, la reserva cubre **todo o nada** (A4) con una cobertura viva por turno e idempotente |
| Cobertura insuficiente | El fondo vacío frena la entrega | Resultado `INSUFICIENTE` (`CU23CubrirConRespaldo.java:87,144`): no se cubre una parte; se abre incidente y se **conserva la deuda** |
| Deuda del incumplidor | Subrogación a favor del fondo; reposición descontada de su entrega futura | Sigue como derecho de recuperación (`CU23RecuperarRespaldo`); **no es caja** |
| Camino viejo | — | `CU23CubrirIncumplimiento` (fondo mutual, `AP-CU23-02` si no hay fondo) **sigue en el código** y sigue expuesto |

Ejemplo sintético: pozo Bs 6.000, confirmados Bs 5.000 → faltante Bs 1.000. Antes, con fondo
vacío: la entrega queda bloqueada. Después: la reserva transfiere Bs 1.000, el beneficiario recibe
Bs 6.000 y el incumplidor queda debiendo Bs 1.000 como recuperación, no como caja.

**Consecuencias financieras.**

- El riesgo de mora **cambia de dueño**: pasa de los participantes a la empresa. Un incumplidor del
  primer turno deja `c · (N − k)` de aportes futuros pendientes (N = 12, k = 1: Bs 5.500).
- Hace falta **liquidez** (caja a tiempo), no solo capital (absorber pérdida): la reserva tiene que
  estar constituida **antes** de activar el grupo; el sistema debe rechazar la activación si no hay
  capacidad y no recortar el pozo de grupos ya comprometidos.
- La cobertura es **todo o nada**: un grupo cuya reserva no alcanza no recibe una entrega parcial.
- Mientras convivan el fondo mutual y la reserva, hay **dos fuentes** que pueden cubrir el mismo
  faltante: hay que nombrar cuál manda para cada grupo o la contabilidad no cierra.

**Decidido en el repositorio:** el camino del pozo completo y la reserva por ciclo (código y pruebas
de la tercera vuelta, H8.S1.M3 y M4). **Abierto:** los números y la naturaleza jurídica (§4).

## 2. Comisión de servicio

| | Antes (documentado) | Después |
|---|---|---|
| Regla | `COMISION_PLATAFORMA` es una **deducción** de la bolsa: `neto = bruto − deducciones` (`CU-22`, `docs/entidades/04_entregas_fondo.md:95-98`) | **Cargo separado** del principal (PLAN §1.2); el beneficiario recibe el pozo íntegro |
| Tarifa sembrada | `seeders/minimos/04-tarifario.json`: «0,3 % de la bolsa con piso Bs 10 y techo Bs 50, **deducido de la entrega**»; `descripcion_usuario`: «Se descuenta de la bolsa cuando cobrás tu turno» | Sin cambio en el seed |
| Código | `CU22LiquidarEntrega` (`POST /entregas`, `liquidarEntrega`, permiso `ENTREGA_EJECUTAR`) sigue aceptando deducciones con tipo y origen | `CU22EntregarPozoCompleto`: «el importe a entregar es SIEMPRE el pozo, sin deducciones sobre el principal (A6)» — `POST` de `fondearPozo` |

**La contradicción sigue abierta en el código:** hay **dos caminos** de entrega. El que respeta la
regla (pozo completo) y el que deduce (liquidación con deducciones) están los dos publicados. Cuál
se usa en producción lo decide quien configure la operación, no el sistema.

**Consecuencias financieras (sintéticas).** Con el 1 % ilustrativo del informe del modelo
financiero sobre Bs 72.000 de volumen por ciclo, el ingreso es Bs 720. En la salida A (cargo
separado) se cobra aparte y los Bs 6.000 no cambian; en la B (deducción) reduce el neto del
receptor y la promesa «recibís el pozo completo» deja de ser literal. A 2 % de incumplimiento
hipotético el margen del 1 % ya es negativo: **precio y respaldo empresarial se acoplan**; no se
deciden por separado. Además el cargo separado **puede quedar impago**: hay que definir qué pasa
(ver §4, D-04).

**Decidido en el repositorio:** el camino de pozo completo no deduce (A6). **Abierto:** A o B, y qué
se hace con el cargo impago; el seed sigue diciendo «deducido» y debe reconciliarse con la decisión.

## 3. Autogestión

| | Antes | Después |
|---|---|---|
| Regla | «El organizador está habilitado **o el grupo es autogestionado**» (`CU-20`, precondición 1); «administra una persona habilitada **o el sistema solo (organizador digital)**» (`docs/entidades/02_grupos_turnos.md`) | Quien crea el grupo es un organizador **habilitado por backoffice** y queda administrador en la misma operación |
| Código | `organizador.map(afuera::organizadorHabilitado).orElse(true)`: sin organizador, permitido | `MapeoDeAltaDeGrupo.java:38-44` resuelve el organizador desde la sesión y rechaza con `AP-CU20-02`; `CU20CrearGrupo.java:88-90` lo exige de nuevo |
| Efecto | Cualquier usuario autenticado abría un grupo «de la plataforma» | Quien no está habilitado no crea grupos; todo grupo tiene un responsable identificable |

**Consecuencias financieras.** Cada grupo tiene un responsable con habilitación vigente, que es la
base para exigir verificación reforzada antes de administrar plata ajena. **No cambia RN-18**:
administrar no se cobra ni se custodia. Los grupos «de organizador digital» **dejan de crearse** por
esta vía; si el negocio los quiere, necesitan una decisión explícita (D-12).

**Decidido en el repositorio:** la regla y su control en dos capas. **Desalineado:** `CU-20` (líneas
25-26) y `02_grupos_turnos.md` (35-36) todavía describen el grupo autogestionado; hay que corregir
**el documento**, no el código.

## 4. Abierto — decisión de negocio, a quién consultar

Ninguna de estas filas tiene respuesta en el repositorio; no se infiere. Los valores «para
simulación» son supuestos reversibles y sintéticos (`apto_produccion = false`).

| ID | Qué falta decidir | A quién consultar | Mientras tanto |
|---|---|---|---|
| D-02 | Naturaleza jurídica del respaldo (garantía contractual, seguro, afianzamiento, capital propio) y su exigibilidad | Dirección + asesoría legal + finanzas | La empresa cubre todo faltante del escenario, sintético |
| D-03 | Capital, caja y topes agregados de cobertura; qué hace el sistema cuando la reserva no alcanza (hoy: todo o nada, sin fila de capacidad se deniega) | Finanzas + órgano de gobierno | Reserva sintética declarada; sin porcentaje inventado |
| D-04 | Comisión: cargo separado (A) o deducción con bruto/neto (B); qué pasa con el cargo impago; impuestos y factura | Producto + finanzas + área tributaria | Tabla sintética versionada; el seed mantiene «deducido» y está marcado como pendiente |
| D-12 | ¿Existen grupos sin organizador humano («organizador digital»)? | Producto | No existen: el código lo rechaza |
| D-15 | ¿Coexisten el fondo mutual y la reserva empresarial? Con qué prioridad | Producto + finanzas | Coexisten, nombrados aparte |
| D-16 | Tope monetario por operación de billetera y de QR (el informe remite al reglamento del BCB vigente) | Legal + cumplimiento | Sin tope fijo en el código |

## Consecuencias

- Se corrige la documentación (`CU-20`, `02_grupos_turnos.md`, `04_entregas_fondo.md`, `CU-22`, el seed
  del tarifario) **cuando** D-04 y D-12 estén respondidas, no antes.
- Hay que **retirar o marcar como legado** uno de los dos caminos de entrega (D-04) y uno de los dos
  mecanismos de cobertura (D-15); mientras convivan, el reporte financiero debe distinguirlos.
- Ninguna capacidad con dinero real se habilita por este ADR: sigue condicionada al dossier H15.

## Cómo se verifica

- Garantía: las pruebas de `CU133PozoCompleto` y `CU131Cobertura` (tercera vuelta) y `CU23CubrirConRespaldo`
  (todo o nada, una cobertura viva por turno).
- Autogestión: `AP-CU20-02` en `servicios/grupos` (rechazo sin organizador habilitado).
- Comisión: **no hay prueba que lo cierre** mientras D-04 siga abierta; es lo que falta.

## No cubierto

- Este carril **no ejecutó** pruebas del backend: las referencias de código son lecturas del
  repositorio en el HEAD de trabajo. La evidencia de ejecución está en los reportes de H8.S1.M3 y M4.
- No se verificó qué camino de entrega usan los clientes (web, backoffice, móvil) en una operación
  real; el frontend actual no tiene pantalla de entrega.
- La viabilidad jurídica del respaldo queda en el dossier de habilitación real (H15).

## Referencias

- `evidencia/carril-F/E-H1-03-ADR-contradicciones.md` y `E-H1-04-decisiones-comerciales-abiertas.md`
  del trabajo `2026-10-08-endurecimiento-integral` (primera versión, sobre el HEAD anterior).
- [[ADR-049 Doble aprobación de retiros|ADR-049]] — el mismo estilo de decisión con dinero.
