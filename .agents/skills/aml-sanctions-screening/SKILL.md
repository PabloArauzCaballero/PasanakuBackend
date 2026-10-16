---
name: aml-sanctions-screening
description: Prevención de lavado y financiamiento del terrorismo aplicada al software — cribado contra listas con versiones y coincidencia difusa, gestión de coincidencias sin bloquear a inocentes, monitoreo de operaciones inusuales con reglas explicables, alertas de cumplimiento con expediente y plazo, reporte a la autoridad con el deber de reserva (prohibido avisarle al usuario), retención probatoria y separación de funciones. Usar al construir o tocar cribado de listas, alertas de cumplimiento, reglas de monitoreo, reportes regulatorios, o cualquier decisión de bloquear una cuenta u operación por sospecha.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Cribado de listas y prevención de lavado

Esta skill es de **ingeniería**, no de asesoría legal. Qué se reporta, a quién, en qué plazo y
con qué formato lo define el marco regulatorio boliviano y el oficial de cumplimiento de la
empresa: **se valida con ellos antes de fijar umbrales, formatos o plazos**
(`regulatory-compliance-mapping`). Lo que sigue es cómo se construye el mecanismo sin romperlo.

Entidades: `coincidencia_lista`, `alerta_cumplimiento`, `lista_restriccion_interna`.

## 1. Cribado contra listas

- Las listas (internacionales, locales, internas) se **importan versionadas**: origen, fecha,
  hash, cantidad de registros. Sin versión no podés responder "¿contra qué lista se cribó a esta
  persona en marzo?", que es exactamente lo que va a preguntar una auditoría.
- Se criba: al alta (KYC), al cambiar datos identificatorios, **y periódicamente contra la lista
  nueva** — alguien puede entrar a una lista después de registrarse.
- **Coincidencia difusa** (nombres transliterados, orden invertido, apellidos compuestos) con
  umbral configurable. Coincidencia exacta sola no sirve; coincidencia demasiado laxa genera
  cientos de falsos positivos que nadie revisa.
- Cada `coincidencia_lista` guarda: lista, versión, criterio, puntaje y los campos que
  coincidieron. Una coincidencia sin explicación es imposible de descartar con criterio.

## 2. Coincidencia ≠ culpa

Una coincidencia de nombre es **una hipótesis**, no una condena. Hay muchos homónimos.

- La coincidencia abre un expediente con **estado y dueño**:
  `DETECTADA → EN_ANALISIS → { DESCARTADA | CONFIRMADA }`.
- `DESCARTADA` guarda **por qué** (fecha de nacimiento distinta, documento distinto) y, para no
  revisar lo mismo cada semana, queda registrada contra esa versión de lista con revisión al
  actualizarse.
- `CONFIRMADA` dispara lo que el marco regulatorio exija: eso lo define cumplimiento, no el
  código.
- El bloqueo preventivo, si corresponde, es **acotado y revisado por una persona**. Bloquear
  automáticamente por puntaje deja gente afuera de su plata por parecerse a alguien.

## 3. Monitoreo de operaciones inusuales

Reglas **explícitas, versionadas y explicables**, no un modelo opaco:

| Señal | Por qué importa en un pasanaku |
|---|---|
| Aportes muy superiores al perfil declarado (`perfil_financiero`) | Origen de fondos incoherente |
| Un mismo beneficiario bancario en cupos de personas distintas | Cuentas controladas por un tercero |
| Entradas y salidas rápidas sin lógica de ahorro | Uso del grupo como pasarela |
| Grupos formados solo por cuentas recién creadas y vinculadas | Estructura armada |
| Pagos desde orígenes múltiples no relacionados con el titular | Fraccionamiento |
| Cancelación anticipada pagando todo de golpe tras cobrar el turno | Patrón de colocación |

- Cada regla tiene **dueño, versión, umbral configurable y tasa de falsos positivos medida**.
  Una regla que dispara cien alertas por día y nadie revisa es peor que no tenerla.
- Los umbrales **no se hardcodean**: son configuración versionada y auditada al cambiarse
  (quién, cuándo, de qué a qué).
- La alerta guarda **qué regla y con qué datos** disparó. Sin eso, el analista no puede decidir.

## 4. Expediente de alerta

`alerta_cumplimiento`: estado, dueño, plazo, análisis, decisión y evidencia. Append-only en su
historial (`audit-trail-history`).

- **Plazo objetivo** por tipo de alerta, medido. Alertas viejas sin resolver son el hallazgo
  número uno de cualquier auditoría.
- La decisión —descartar, escalar, reportar— queda con su justificación escrita.
- El expediente es la prueba de que el sistema funciona. Si no está escrito, no ocurrió.

## 5. Deber de reserva

> [!important] Prohibido avisarle al usuario que fue reportado.
> No es una preferencia de producto: en general está prohibido por norma ("tipping off"). El
> sistema **no** debe tener ninguna pantalla, notificación, mensaje de error, estado visible ni
> respuesta de API que revele que existe un reporte o una alerta de cumplimiento.

Consecuencias de diseño:

- Los estados de cumplimiento **no se exponen** en las APIs de cliente. Si una operación se
  frena, el mensaje al usuario es genérico y se coordina con cumplimiento.
- Ni en logs accesibles al soporte general, ni en analytics, ni en eventos que consuman
  servicios que alimenten pantallas de usuario.
- Acceso a los expedientes restringido a cumplimiento, con auditoría de lectura
  (`data-privacy-financial` §8).
- Un desarrollador que agrega un campo `enRevisionAML` a un DTO de cliente crea un incidente
  regulatorio. Se revisa explícitamente en el code review.

## 6. Separación de funciones

- Quien **opera** (soporte, organizador, desarrollo) no decide sobre alertas.
- Quien **analiza** cumplimiento no puede modificar los datos operativos ni las listas.
- El cambio de umbrales y de listas se audita y requiere autorización distinta de la de quien
  los usa (`service-to-service-security` §4, menor privilegio).

## 7. Retención probatoria

- Los expedientes, las listas versionadas y los reportes se conservan por el plazo que exija la
  norma: **no se borran** aunque el usuario pida el borrado de su cuenta
  (`data-privacy-financial` §6). Se borra lo borrable; esto se conserva y se declara por qué.
- Los reportes enviados se guardan con acuse, hash y fecha.

## Anti-patrones

- Cribar solo al alta y nunca más.
- Listas sin versión ni hash.
- Coincidencia exacta como único criterio, o umbral difuso sin medir falsos positivos.
- Bloqueo automático por puntaje, sin revisión humana.
- Coincidencia descartada sin registrar el motivo.
- Umbrales hardcodeados.
- Alerta sin dueño, sin plazo y sin expediente.
- **Cualquier cosa visible para el usuario que revele la alerta o el reporte.**
- Estado de cumplimiento en un DTO de cliente o en un evento de uso general.
- Borrar expedientes al borrar la cuenta.
- Reglas de monitoreo que nadie puede explicar.

## Checklist

- [ ] Listas importadas con versión, fecha, origen y hash.
- [ ] Cribado al alta, al cambiar datos y periódico contra la lista nueva.
- [ ] Coincidencia difusa con umbral configurable y campos coincidentes guardados.
- [ ] Coincidencias con expediente, estado, dueño y motivo de descarte.
- [ ] Sin bloqueo automático por puntaje; revisión humana de por medio.
- [ ] Reglas de monitoreo versionadas, explicables y con falsos positivos medidos.
- [ ] Alertas con plazo objetivo medido.
- [ ] Deber de reserva verificado: nada visible al usuario, en ninguna capa.
- [ ] Acceso a expedientes restringido y auditado.
- [ ] Separación de funciones aplicada en permisos.
- [ ] Retención probatoria declarada, por encima del borrado a pedido.

## Evidencia / DoD

1. Registro de importación de la lista con versión y hash.
2. Salida del cribado de un caso de prueba, con criterio, puntaje y campos coincidentes.
3. Expediente de una coincidencia descartada, con su motivo.
4. **Revisión explícita de que ningún DTO, evento, log de soporte o mensaje al usuario expone
   el estado de cumplimiento**, con el `grep` usado y su salida.
5. Consulta de auditoría de lectura sobre un expediente.
