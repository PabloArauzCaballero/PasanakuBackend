---
tags:
  - caso-uso
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
codigo: CU-132
criticidad: alta
actores: [Riesgos, Cumplimiento, Sistema]
normas: [Regla 91 (dinero), ADR-056 §6, DECISIONES tandas 1, 6 y 7]
---

# CU-132 — Registrar, aprobar y congelar parámetros financieros

> **Objetivo.** Que ningún parámetro financiero del modelo A + D (aporte al fondo del grupo, días de gracia, tasa y
> tope del recargo, umbrales de solvencia y de disolución, reducción por «Grupo perfecto») viva en el código ni en
> un archivo de configuración: se registra versionado, lo aprueban **dos personas distintas** y cada grupo congela
> los suyos al iniciar. **Este caso no fija ningún valor**: los fijan Pablo y Cumplimiento (F3.S8.M3).

## Actores y disparador

- **Actores:** quien propone (Riesgos o Producto), quien aprueba (otra persona; Cumplimiento), el sistema (congela).
- **Disparador:** una decisión de negocio sobre un parámetro, o el inicio de un grupo (`grupo_iniciado`).

## Precondiciones

1. El código existe en [[parametro_financiero]] (catálogo cerrado; las unidades pendientes de decisión quedan sin
   unidad y no admiten aprobación, `R-TAR-15`).

## Flujo principal

1. Quien propone registra una [[version_parametro]] en `PROPUESTA` con valor, vigencia, nivel (si el parámetro es
   por nivel) y procedencia (fuente, referencia y fecha). Su identidad sale de su token.
2. Otra persona la aprueba con su propio token: `aprobado_por` distinto de `propuesto_por` (`R-TAR-14`). La versión
   aprobada no se edita; se corrige con una versión nueva (`R-TAR-16`). Valor no negativo y moneda solo si es un
   monto (`R-TAR-15`).
3. Al iniciar un grupo, el sistema escribe una fila de [[parametro_congelado_grupo]] por parámetro y nivel con la
   versión aprobada vigente (`R-TAR-17`). Un cambio posterior solo rige para grupos nuevos (ADR-056 §6), igual que
   la tarifa congelada de [[CU-20 Crear grupo y congelar tarifario]].

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | Quien aprueba es quien propuso | La base lo rechaza (`R-TAR-14`); la versión sigue `PROPUESTA` |
| 2b | El parámetro tiene la unidad todavía sin decidir | Se puede proponer un valor, pero no aprobarlo (`R-TAR-15`) |
| 2c | Hay que corregir un valor ya aprobado | No se edita: se propone y aprueba una versión nueva, que rige para los grupos que inicien desde su vigencia (`R-TAR-16`) |
| 3a | Falta una versión aprobada de un parámetro que el grupo necesita | El grupo no inicia; la consulta del congelamiento responde 422 con el código que falta, nunca un valor por omisión |
| 3b | Se consulta un grupo sin congelamiento | 404; quien consume falla cerrado (contrato de `garantia`) |

## Postcondiciones

- Cada grupo iniciado tiene exactamente una versión congelada por parámetro y nivel, inmutable para el rol de la
  aplicación.

## Contrato · `openapi/tarifas.yaml`

- Lado consumidor: `docs/trabajo/2026-11-21-tanda2-l2/evidencia/F3.L2.M14-contrato-parametros-tarifas.md`.
- Lado productor: `openapi/tarifas.yaml` (F3.S8.M2b, pendiente).

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `ParametroFinanciero.exigeMoneda` | Moneda solo para montos; puro |
| Molécula | `ParametroRepositorio` | Versiones y congelamiento, con la precondición en la escritura |
| Organismo | Proponer, aprobar y congelar (F3.S8.M2b, M6) | Transacción por acto; actores de `CuatroOjos` |
| Página | Rutas de parámetros en `tarifas` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `tarifas.parametros_congelados` (opcional, contrato de L2 §4) | `garantia` guarda su copia | — |
| Consume `grupos.grupo_iniciado` | Congelamiento del grupo | Identidad de servicio |

## Interfaz

- **Backoffice:** pantalla de parámetros con historial de versiones, propuesta y aprobación por otra persona
  (F3.S8.M4).

## Restricciones aplicables

`R-TAR-14` · `R-TAR-15` · `R-TAR-16` · `R-TAR-17`

## Evidencia que deja

[[parametro_financiero]] · [[version_parametro]] · [[parametro_congelado_grupo]]

## Criterios de aceptación

Los prueban `CU132Test` (F3.S8.M2b) y `CU132CongelamientoTest` (F3.S8.M6) contra PostgreSQL; la base los sostiene además por su cuenta
(`docs/trabajo/2026-11-21-tanda2-l3l5/evidencia/F3.S8.M2a-modelo-parametros.txt`).

```gherkin
Dada una versión de parámetro propuesta por una persona
Cuando otra persona la aprueba con su propio token
Entonces la versión queda APROBADA y rige desde su vigencia

Dada una versión de parámetro propuesta por una persona
Cuando la misma persona intenta aprobarla
Entonces la aprobación se rechaza y la versión sigue PROPUESTA

Dada una versión de parámetro APROBADA
Cuando alguien intenta cambiar su valor o borrarla
Entonces se rechaza y la corrección exige una versión nueva

Dado un parámetro con una versión aprobada vigente y otra que entra en vigor después
Cuando un grupo inicia
Entonces el grupo congela la versión que regía al iniciar

Dado un grupo que ya congeló un parámetro en un nivel
Cuando se intenta congelar otra versión del mismo parámetro y nivel para ese grupo
Entonces se rechaza y el grupo conserva la versión que congeló al iniciar
```

## Ver también

[[CU-20 Crear grupo y congelar tarifario]] · [[CU-34 Publicar un tarifario nuevo con preaviso]]
