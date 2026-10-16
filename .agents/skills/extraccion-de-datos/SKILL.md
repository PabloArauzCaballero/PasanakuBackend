---
name: extraccion-de-datos
description: "Sacar datos de AportaYa con permiso, huella y vencimiento: definición de reporte con parámetros validados, ejecución con la sesión del solicitante y RLS vigente, hash del resultado, exportación cifrada que caduca, tope de descargas y programación con bloqueo entre réplicas. Úsala al crear un reporte, al exportar cualquier cosa con datos personales, al atender un pedido de auditoría, o cuando alguien quiera correr una consulta contra producción."
---

# Extraer datos

Toda fuga de datos empieza con alguien sacando un archivo. Esta skill es cómo se
saca un archivo sin que eso sea el principio de un incidente.

> **Una extracción no es una consulta: es un acto con solicitante, motivo, huella y
> caducidad.**

## Reglas duras

1. **Sin permiso, no hay reporte.** Cada [[definicion_reporte]] declara su
   `permiso_requerido`; el intento sin permiso también queda registrado.
2. **La consulta corre con la sesión del solicitante.** Las políticas de fila siguen
   rigiendo (`R-SEG-03`). Un reporte **no es una puerta trasera al RLS**.
3. **Parámetros validados contra `parametros_esperados`.** Nombre, tipo y rango.
   Nunca se concatena texto del usuario en la consulta.
4. **Réplica de lectura, con tiempo máximo.** Un reporte pesado no compite con el
   camino del dinero y no cuelga producción (skill `lecturas-proyecciones`).
5. **Datos sensibles exigen justificación** escrita, que va a
   [[registro_acceso_datos]] (`R-SEG-02`).

## El hash del resultado

`hash_resultado` sobre el conjunto canónico: dos ejecuciones con los mismos
parámetros y los mismos datos dan el mismo hash. Sirve para dos cosas concretas:

- probar que **el reporte entregado no fue alterado** después;
- detectar que un reporte "igual" en realidad cambió.

## La exportación caduca

```
esta_cifrado = true  (obligatorio si la definición marca datos sensibles)
expira_en            (obligatorio, siempre)
descargas            (con tope; agotado, el enlace muere)
```

> **Un enlace eterno es una fuga futura.** No importa que hoy esté en un correo
> interno: dentro de dos años ese correo se reenvía.

Vencido el archivo, se vuelve a ejecutar el reporte —dejando nueva huella—, no se
extiende la vigencia.

## Reporte vacío

Se entrega igual, con cero filas. "No hubo" es una respuesta, y para los reportes
regulatorios es **obligatoria** (`R-UIF-06`). Devolver un error cuando no hay datos
hace que el remitente crea que falló el sistema y no reporte nada.

## Programación

[[programacion_reporte]] con `expresion_cron`, destinatarios y canal. El trabajo la
toma **con bloqueo entre réplicas**: dos réplicas no mandan el mismo reporte dos
veces (skill `automatizacion-tareas`).

Y cuando una ejecución programada falla definitivamente: **se avisa a los
destinatarios de que no llegó**. Si no, el silencio parece normalidad y nadie nota
que hace tres meses no recibe el reporte.

## Versionado de la definición

Se versiona. Las ejecuciones viejas conservan con qué definición se produjeron, para
poder responder por qué el mismo reporte de marzo y de abril tienen columnas
distintas.

## Extracto ≠ reporte

| | Quién lo pide | Para quién | Caso de uso |
| --- | --- | --- | --- |
| **Extracto** | el titular | el titular | [[CU-15 Emitir extracto y certificado de saldo]] |
| **Reporte** | un operador | la organización o el supervisor | [[CU-58 Definir, programar y exportar un reporte]] |

No se implementa uno con el otro. El extracto tiene folio, hash y archivo
conservado; el reporte tiene permiso, justificación y caducidad.

## Auditoría: "¿quién sacó qué?"

Se responde con las ejecuciones, sus solicitantes, sus parámetros y sus descargas.
Si esa pregunta no se puede responder en una consulta, el diseño está mal.

## Qué no hacer

- No exportar sin fecha de caducidad.
- No permitir parámetros libres que lleguen a la consulta.
- No correr reportes con un usuario técnico que ve todas las filas.
- No mandar archivos con datos personales sin cifrar.
- No armar un reporte a medida para cada pedido: si se repite, es una definición.
- No usar el tablero para pedidos puntuales (skill `indicadores-tablero`).

## Ver también

- [[CU-58 Definir, programar y exportar un reporte]] · [[CU-45 Atender un requerimiento de autoridad]] ·
  [[CU-07 Ejercer derechos sobre datos personales]] · [[CU-15 Emitir extracto y certificado de saldo]]
- `R-SEG-02` · `R-SEG-03` · `R-AUD-08` · `R-UIF-06` · `R-CON-05` en [[Restricciones]]
- Skills: `lecturas-proyecciones`, `seguridad-sesion-rls`, `reportes-regulatorios`,
  `indicadores-tablero`, `automatizacion-tareas`
