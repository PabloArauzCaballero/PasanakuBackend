# Diccionario de variables de riesgo del motor de admisión (H6.S1.M1)

Derivado del código (`servicios/grupos`) y de la evidencia que guarda cada postulación. No inventa ni fija
parámetros: lo que el código no dice se marca **AMBIGUO** con a quién preguntarle. Estado de evidencia: documento
derivado de lectura de código y esquema; la reproducibilidad por postulación la prueba `CU68MotorTest`
(versión del motor + entradas guardadas).

## 1. Qué es el motor y qué no es

- Es una **recomendación explicable**, nunca una decisión: `ACEPTAR` o `REVISION_HUMANA`
  (`MotorDeRecomendacion.recomendacion`). Nada se rechaza por el motor salvo la rama excluyente heredada de TEST
  (§5). La admisión la resuelve backoffice (`decision_ingreso`, append-only).
- Se ejecuta al postular (`CU68Postular.postular`) y deja un evento `grupos.ingreso_solicitado` con la **evidencia
  original** (`payload`): `versionMotor` (id de `criterio_emparejamiento`), `criterio`, `motivos`, `puntaje`,
  `recomendacion`, `entradas`, `decisionAutomatica=false`. Esa evidencia se copia a `decision_ingreso.evidencia_algoritmo`
  junto con `version_motor`, `recomendacion_algoritmo` y `apartamiento` al decidir un humano.
- Dado el mismo `criterio_emparejamiento` (pesos, mínimo de reputación, tope de morosos) y las mismas `entradas`,
  el puntaje y los motivos se reproducen (`CriterioDeEmparejamiento.puntuar`, división por la suma de pesos, 4
  decimales HALF_UP).

## 2. Variables que entran al motor

| Variable (clave en `entradas`) | Qué mide | Origen (quién la afirma) | Cómo llega | Fecha / vigencia | Calidad y fallo | Consentimiento aplicable |
|---|---|---|---|---|---|---|
| `reputacion` | Puntaje entero de reputación del postulante | `transparencia` `GET /reputacion/{usuarioId}/puntaje` (`HechosPorHttp.reputacion`) | Fuera de la transacción, con el token del postulante | Es el valor al momento de postular; se guarda en la evidencia, no se refresca | `tieneHistorial=false` o sin respuesta => `SIN_DATOS_REPUTACION`, NO es puntaje cero | **AMBIGUO** (§6, A2) |
| `morososDelGrupo` | Cantidad de morosos del grupo al que se postula | `aportes` `GET /aportes/grupos/{id}/morosos` | Ídem | Al momento de postular | Sin respuesta => valor `SIN_DATO` (`Integer.MAX_VALUE` internamente) => `SIN_DATOS_CONCENTRACION` | Dato agregado del grupo, no personal del postulante. **AMBIGUO** si se trata como personal (A2) |
| `afinidadReputacion` | Cercanía del riesgo del grupo al del postulante, 0..1 | Hoy **constante de configuración** `aportaya.grupo.afinidad-neutra` (0.50) | `MapeoDePostulacion` | Fija por despliegue | Sin dato real; valor neutro, no mide nada (A3) | n/a |
| `afinidadMonto` | Encaje del monto del aporte, 0..1 | Ídem (neutro 0.50) | Ídem | Ídem | Ídem (A3) | n/a |
| `afinidadGeografia` | Gente de la zona, 0..1 | Ídem (neutro 0.50) | Ídem | Ídem | Ídem (A3) | **AMBIGUO**: si se midiera con ubicación real exigiría consentimiento de ubicación (A4) |
| `afinidadHistorial` | Historial compartido con miembros, 0..1 | `1` si `transparencia` informa historial; si no, neutro | Ídem | Al postular | Derivada de `tieneHistorial` (no mide historial compartido real, A3) | **AMBIGUO** (A2) |

Parámetros del criterio (tabla `criterio_emparejamiento`, con vigencia; NO son constantes del código):
`pesoReputacion`, `pesoMonto`, `pesoGeografia`, `pesoHistorialComun`, `reputacionMinima`, `maxMorososPorGrupo`.
Sus valores son decisión de producto y riesgo y no se fijan aquí.

## 3. Hechos que bloquean antes del motor (no son variables de puntaje)

| Hecho | Origen | Efecto | Código |
|---|---|---|---|
| Restricción vigente | `garantia` `/cobranza/restricciones/vigentes/{usuarioId}`; sin respuesta se asume restringido | Rechazo, indicando `montoQueLaLevanta` | AP-CU68-01 |
| Nivel de KYC insuficiente | `identidad` `/usuarios/{id}/kyc` (sin respuesta: `NINGUNO`) vs `requiere_kyc_minimo` del grupo | Rechazo | AP-CU68-02 |
| Sin cupos libres / ya postuló / sin criterio vigente | Esquema propio de `grupos` | Rechazo | AP-CU68-04, 06, 05 |

Estos son controles de elegibilidad, no recomendación de riesgo; su calidad es la del servicio que los afirma y el
fallo es siempre cerrado (denegar por omisión).

## 4. Motivos y alertas que produce

- Motivos legibles (`MotivoDelPuntaje`): «el monto del aporte encaja», «hay gente de tu zona», «el riesgo del grupo
  se parece al tuyo», «ya compartiste grupo con alguien de ahí», «es lo más cercano a lo que pediste». El corte es el
  promedio de las cuatro dimensiones; el usuario ve el criterio, nunca datos de otros.
- Alertas que pasan la recomendación a `REVISION_HUMANA`: `SIN_DATOS_REPUTACION`, `REVISAR_REPUTACION` (reputación
  < mínimo), `SIN_DATOS_CONCENTRACION`, `REVISAR_CONCENTRACION` (morosos >= tope).

## 5. Variables excluidas (a propósito) y lo que NO se usa

- **Rechazo automático por el motor: excluido.** La única rama que rechaza es la heredada de TEST
  (`reputacionExcluyente`, AP-CU68-03, CU-68 2a/B24): solo a quien TIENE historial por debajo del mínimo; la ruta HTTP
  de producción usa la forma de recomendación, que no rechaza.
- **Sin historial no es riesgo alto:** se declara `SIN_DATOS`, no cero.
- **No entran** (no están en el código ni en la evidencia): documento de identidad, nombre, edad, género, nacionalidad,
  dirección o geolocalización real, teléfono, correo, contactos/referencias personales, ingresos, perfil financiero
  (`perfil_financiero`), historial crediticio externo, listas restrictivas/PEP, ni datos de dispositivo. La evidencia
  guarda solo las seis entradas de §2 y el identificador de versión.
- **Prohibido como entrada** sin decisión registrada de cumplimiento y consentimiento: cualquiera de las anteriores,
  y toda variable que funcione como proxy de una categoría protegida (zona de residencia fina, apellido, etc.).

## 6. Ambigüedades abiertas (no resueltas por el código)

| ID | Pregunta | Supuesto tomado | A quién |
|---|---|---|---|
| A1 | Definición de **«apartamiento»**: hoy es `ACEPTAR` humano con recomendación `REVISION_HUMANA`, o `RECHAZAR` humano con recomendación `ACEPTAR` (`CU68AceptarIngreso:128`); solo se registra | Es la definición implementada, no la normativa | Cumplimiento |
| A2 | Qué `consentimiento` (`identidad.consentimiento.tipo`: PRIVACIDAD, TRATAMIENTO_DATOS, ...) habilita usar reputación e historial para recomendar admisión, y desde cuándo; el código **no verifica consentimiento antes de preguntar** | No se verifica; se asume cubierto por los términos aceptados al registrarse | Cumplimiento y Legal (datos personales) |
| A3 | Las tres afinidades (reputación, monto, geografía) son hoy el valor neutro 0.50: el puntaje real depende solo de los pesos y de `afinidadHistorial`. ¿Se medirán? ¿con qué fuente y calidad mínima? | El puntaje es informativo; la decisión es humana | Producto y Riesgos |
| A4 | Si la geografía se medirá, con qué dato y consentimiento de ubicación | No se mide | Producto, Legal |
| A5 | Frescura máxima tolerable de `reputacion` y `morososDelGrupo` al postular (hoy se toman en el momento y no se refrescan al decidir días después) | Vale lo guardado en la evidencia | Riesgos |
| A6 | Retención y acceso a la evidencia (`evidencia_algoritmo` contiene reputación y morosos del grupo) | Append-only; lectura por backoffice y por el participante sobre su solicitud | Cumplimiento |
| A7 | Valores de `reputacionMinima`, `maxMorososPorGrupo` y pesos: sintéticos de siembra, sin fuente | Marcados sintéticos; no aptos para producción | Riesgos y Dirección |

## 7. Dónde verificarlo

`servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/{CU68Postular,MotorDeRecomendacion,CU68AceptarIngreso}.java`,
`.../dominio/{CriterioDeEmparejamiento,MotivoDelPuntaje}.java`, `.../web/MapeoDePostulacion.java`,
`sql/10_tablas/02_grupos_turnos/{criterio_emparejamiento,decision_ingreso,solicitud_ingreso}.sql`; pruebas
`CU68MotorTest`, `CU68AdmisionTest`, `CU68Test`.
