# Guía para corregir las 17 divergencias de criterios del PR #50

## Propósito

Esta guía convierte el fallo del check `verificar_criterios.py` en una lista de trabajo concreta. El check no indica que las 17 pruebas estén rotas: indica que la especificación Gherkin y los nombres de las pruebas no coinciden uno a uno. Hay tres situaciones distintas:

1. **Prueba sin criterio documentado:** existe un `@DisplayName` en `servicios/<servicio>/src/test/java`, pero el primer bloque `gherkin` del CU no contiene un escenario equivalente.
2. **Criterio sin prueba con el mismo nombre:** hay un escenario Gherkin, pero ninguna prueba del CU declara ese mismo escenario.
3. **Desfase en ambos sentidos:** el CU tiene un escenario y la prueba correspondiente tiene otro nombre o expresa una conducta más completa.

La fuente del reporte fue el paso `6d · verificar_criterios.py — cada gherkin con su prueba` del CI del PR #50. El verificador cuenta como escenario cada grupo de líneas consecutivas `Dado/Dada/Dados/Dadas`, `Cuando`, `Entonces`, `Y` o `Pero` dentro del primer bloque ````gherkin````. Une esas líneas con ` · ` y compara el resultado con `@DisplayName`, ignorando mayúsculas, tildes, puntuación y espacios repetidos. También comprueba las restricciones `R-XXX-nn`; no se debe quitar una referencia a una restricción para hacer pasar el gate.

## Resumen de las 17 fallas

| N.º | CU / servicio | Tipo | Qué hay que alinear |
|---:|---|---|---|
| 1 | CU-01 / identidad | Prueba sin criterio | Verificación de correo: código incorrecto y correcto, y alta única. |
| 2–3 | CU-08 / identidad | Dos pruebas sin criterio | Códigos de roles vigentes; usuario sin roles. |
| 4–5 | CU-22 / entregas | Desfase en ambos sentidos | Reintento idempotente de liquidación y rechazo de una segunda entrega por `R-GRP-01`. |
| 6 | CU-62 / grupos | Prueba sin criterio | Solo el titular del turno puede iniciar la permuta. |
| 7–8 | CU-63 / grupos | Dos pruebas sin criterio | B38: mantener abierta una votación aún alcanzable y rechazarla cuando el quórum ya sea imposible. |
| 9 | CU-64 / grupos | Prueba sin criterio | B39: repetir el mismo traspaso no cambia el resultado ni mueve el cupo de nuevo. |
| 10 | CU-68 / grupos | Prueba sin criterio | B24: aplicar reputación mínima a quien sí tiene historial, sin excluir a una persona nueva por falta de historial. |
| 11–17 | CU-68 / grupos | Siete pruebas sin criterio | Decisión de ingreso: aceptar, reintentar, rechazar, falta de cupo, actor ajeno, decisión contraria y cola propia. |

## Correcciones propuestas, CU por CU

En cada escenario de abajo, la secuencia de pasos debe ser el nombre del `@DisplayName`: al unir las líneas, se interpone ` · `. Si se conserva el nombre actual de la prueba, redacta el escenario para que produzca ese mismo nombre. Si el escenario propuesto describe mejor el comportamiento, cambia el `@DisplayName` correspondiente. No dupliques un escenario solo para satisfacer al verificador.

### CU-01 · Registro y apertura de billetera (`identidad`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-01 Registro y apertura de billetera.md`, bloque **Criterios de aceptación**.
- Prueba: `servicios/identidad/src/test/java/bo/aportaya/identidad/CU01VerificacionCorreoTest.java`, método `recorridoCompleto`.

El test comprueba la emisión de la verificación, que un código equivocado no confirme el correo y que el correcto habilite exactamente un alta. Falta expresar ese recorrido en el CU. Añade al bloque Gherkin, en una ubicación coherente con el flujo de registro:

```gherkin
Dada una solicitud de alta con una verificación de correo emitida
Cuando se confirma primero con un código incorrecto y luego con el correcto
Entonces el código incorrecto no confirma el correo y el correcto habilita exactamente un alta
```

Actualiza `@DisplayName` de `recorridoCompleto` para que coincida con esas tres líneas unidas por ` · `, o ajusta el escenario al texto que se decida conservar. Mantén en la prueba las aserciones del código incorrecto y la confirmación única.

### CU-08 · Asignar y revocar roles de operador (`identidad`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-08 Asignar y revocar roles de operador.md`, bloque **Criterios de aceptación**.
- Pruebas: `servicios/identidad/src/test/java/bo/aportaya/identidad/CU08CodigosDeRolVigentesTest.java`, métodos `soloLosVigentes` y `sinRolesNoHayCodigos`.

Agrega los dos escenarios que describen las aserciones ya existentes:

```gherkin
Dado un usuario con un rol vigente y otra asignación revocada
Cuando se piden sus códigos de rol vigentes
Entonces vienen los códigos de las asignaciones vigentes y no los de la asignación revocada

Dado un usuario sin roles asignados
Cuando se piden sus códigos de rol vigentes
Entonces no se devuelve ningún código de rol
```

Haz que el `@DisplayName` de cada método coincida con su escenario. Conserva la condición de que la asignación revocada no aporte un código, porque esa es la parte esencial del primer caso.

### CU-22 · Liquidar y entregar el fondo (`entregas`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-22 Liquidar y entregar el fondo.md`, bloque **Criterios de aceptación**.
- Prueba: `servicios/entregas/src/test/java/bo/aportaya/entregas/CU22Test.java`, método `criterio2`.

Aquí hay dos nombres distintos para el mismo test. La especificación dice que intentar crear otra entrega para el turno debe ser rechazado por `R-GRP-01`. El test primero verifica que repetir la liquidación devuelve la misma entrega y no crea otra; después verifica que la base rechaza un `INSERT` duplicado por `uq_entrega_turno`. Ajusta el criterio para reflejar ese comportamiento completo, por ejemplo:

```gherkin
Dado un turno cuya entrega ya fue liquidada
Cuando se repite la liquidación y se intenta insertar una segunda entrega directamente
Entonces el reintento devuelve la misma entrega sin duplicarla y la base rechaza la segunda entrega (R-GRP-01)
```

Renombra el `@DisplayName` de `criterio2` para que corresponda a este escenario. No elimines `R-GRP-01`: el gate requiere que cada restricción citada por el CU aparezca cubierta por una prueba del archivo del caso de uso. Verifica que la prueba siga comprobando ambos aspectos: idempotencia del reintento y defensa de la base ante duplicados.

### CU-62 · Permutar turnos entre participantes (`grupos`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-62 Permutar turnos entre participantes.md`, bloque **Criterios de aceptación**.
- Prueba: `servicios/grupos/src/test/java/bo/aportaya/grupos/CU62Test.java`, método `soloElTitularPideLaPermuta`.

La prueba intenta usar el turno de otra persona y verifica el rechazo. Añade un escenario que deje explícita esa autorización por titular:

```gherkin
Dado un turno que pertenece a otro participante
Cuando una persona que no es su titular solicita permutarlo
Entonces se rechaza la solicitud y no se crea una permuta
```

Haz coincidir el `@DisplayName` del método. Mantén la comprobación de que no se inserta una solicitud, además del rechazo de negocio.

### CU-63 · Proponer y votar un acuerdo (`grupos`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-63 Proponer y votar un acuerdo.md`, bloque **Criterios de aceptación**.
- Pruebas: `servicios/grupos/src/test/java/bo/aportaya/grupos/CU63Test.java`, métodos `elPrimerVotoNoCierraLaVotacion` y `sePierdeCuandoYaNoHayRemedio`.

Las dos pruebas cubren B38: no se debe cerrar la votación antes de tiempo, y sí debe rechazarse cuando ya no puede alcanzar quórum. Añade ambos escenarios:

```gherkin
Dado un acuerdo con votos pendientes que todavía pueden alcanzar el quórum
Cuando se emite el primer voto a favor y el resultado aún no alcanza el quórum
Entonces la votación permanece abierta para que los demás participantes voten

Dado un acuerdo cuyo quórum ya no puede alcanzarse ni con todos los votos pendientes a favor
Cuando se resuelve la votación
Entonces el acuerdo se cierra como RECHAZADO y no se ejecuta su efecto
```

Renombra ambos `@DisplayName` para que coincidan con las respectivas secuencias. Conserva la cobertura de los dos límites: quórum todavía alcanzable y quórum matemáticamente imposible.

### CU-64 · Traspasar un cupo (`grupos`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-64 Traspasar un cupo.md`, bloque **Criterios de aceptación**.
- Prueba: `servicios/grupos/src/test/java/bo/aportaya/grupos/CU64Test.java`, método `repetirElTraspasoEsInocuo`.

La prueba cubre B39, pero ese escenario no figura en el CU. Agrega:

```gherkin
Dado un traspaso de cupo ya ejecutado con un acuerdo aprobado
Cuando se repite el traspaso con el mismo acuerdo
Entonces se devuelve el traspaso ya realizado y el cupo no vuelve a cambiar
```

Haz coincidir su `@DisplayName`. Confirma que la prueba compare el mismo resultado y que las cantidades/participantes del grupo no cambien por segunda vez.

### CU-68 · Postular a un grupo y ser emparejado (`grupos`)

Archivos:

- Especificación: `docs/CasosDeUso/CU-68 Postular a un grupo y ser emparejado.md`, bloque **Criterios de aceptación**.
- Pruebas de postulación: `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68Test.java`.
- Pruebas de decisión: `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68AceptarIngresoTest.java`.

El CU ya documenta postulación y emparejamiento, pero no recoge B24 ni la resolución administrativa de las solicitudes. Incorpora estos ocho escenarios al bloque Gherkin, separando cada uno con una línea en blanco:

```gherkin
Dado un criterio de reputación mínima y dos solicitantes, uno con historial insuficiente y otro sin historial
Cuando ambos postulan al grupo
Entonces se excluye al primero por reputación y el nuevo no se excluye por falta de historial

Dada una solicitud PENDIENTE y un grupo con cupo libre
Cuando el organizador acepta la solicitud
Entonces se reserva el cupo, se crea el participante pendiente de firma y se registra el evento

Dada una solicitud de ingreso ya aceptada
Cuando el organizador repite la misma decisión
Entonces se devuelve el resultado ya resuelto y no se crea otro participante ni se mueve otro cupo

Dada una solicitud PENDIENTE
Cuando el organizador la rechaza sin motivo y luego indica un motivo
Entonces el rechazo sin motivo falla y la solicitud rechazada no ocupa un cupo

Dada una solicitud PENDIENTE y un grupo sin cupos libres
Cuando el organizador intenta aceptarla
Entonces la operación se rechaza y la solicitud permanece PENDIENTE

Dada una solicitud PENDIENTE de un grupo
Cuando decide un organizador de otro grupo o un participante
Entonces la solicitud no se revela ni se modifica

Dada una solicitud cuya decisión ya fue resuelta
Cuando el organizador intenta aplicar la decisión contraria
Entonces se devuelve un error y se conserva la decisión original

Dado un organizador con solicitudes de ingreso de varios grupos
Cuando consulta su cola de solicitudes
Entonces solo ve las solicitudes PENDIENTES de su propio grupo
```

La lista anterior contiene **ocho** escenarios: B24 más los siete escenarios de decisión reportados para CU-68. El método `elRecienLlegadoNoSeExcluyePorSerNuevo` comprueba los dos perfiles; por eso B24 debe permanecer como un único escenario y una única prueba. Al corregir, asigna cada escenario al método correspondiente y renombra sus `@DisplayName`.

En el rechazo por actor ajeno, conserva la no divulgación del recurso (`no existe`) para organizador de otro grupo y la denegación al participante. En el caso de cola, valida que se filtren tanto el estado `PENDIENTE` como el grupo autorizado.

## Secuencia recomendada para implementar

1. Edita únicamente el primer bloque ````gherkin```` del CU. No agregues el texto a un segundo bloque: `escenarios_gherkin` lee el primero.
2. Alinea cada escenario con una prueba existente. Renombra el `@DisplayName` si la especificación mejor describe el comportamiento. No agregues pruebas vacías ni reduzcas aserciones para hacer coincidir texto.
3. Mantén las referencias `R-XXX-nn`; cuando una referencia se conserva, el CU debe tener una prueba que la ejercite en el archivo `CU<NN>*Test.java` correspondiente.
4. Evita escenarios duplicados con nombres distintos para una misma conducta. Si una prueba cubre dos resultados relacionados, como CU-22, documenta ambos en un solo escenario comprobable.
5. Ejecuta el verificador por cada servicio afectado y luego el gate completo:

```powershell
python scripts/verificar_criterios.py --servicio identidad
python scripts/verificar_criterios.py --servicio entregas
python scripts/verificar_criterios.py --servicio grupos
python scripts/verificar_criterios.py
```

6. Ejecuta las pruebas del backend de los CU editados y `.\gradlew.bat spotlessCheck` para comprobar que el formato de `@DisplayName` no rompe el gate de estilo. Tras generar y revisar cambios, repite `python scripts/verificar_criterios.py`.

## Cómo interpretar el resultado esperado

El objetivo de este arreglo es que `python scripts/verificar_criterios.py` termine con código `0` y muestre `Sin divergencias entre la boveda y el codigo.`. Las pruebas sin implementación que el script reporte como **pendientes** no son fallas de este gate. Si tras alinear estos 17 puntos aparece una nueva divergencia, trata esa salida como otro desajuste real entre la especificación y el código; no la ocultes con excepciones en el verificador.

Esta guía describe el reporte observado en el CI del PR #50. Si la rama cambia, vuelve a ejecutar el script antes de aplicar las propuestas: el reporte actual del código es la fuente para confirmar que las 17 discrepancias siguen vigentes.
