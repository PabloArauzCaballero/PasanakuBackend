# Vectores dorados — origen y estado

## `sorteo.vectores.json`

El algoritmo (`barajarDeterminista`, `hashDelCompromiso`) es una reimplementación
byte a byte de `plataforma/comun-dominio/.../SorteoVerificable.java`, la única
implementación real que sortea (CU-60) y verifica (CU-61) en el backend — está
documentada como "el protocolo público" en el Javadoc de esa clase.

Estos vectores **no salieron de una prueba Java versionada** (esa clase no emite
un JSON de casos hoy — es un hueco, ver abajo). Se calcularon de forma
independiente con Python (`hashlib.sha256`), replicando el mismo algoritmo
(Fisher-Yates desde el final, índice = `SHA-256(semilla:paso) mod (paso+1)`,
preimagen del compromiso separada por `\n`), y se versionan acá para que
`sorteo.spec.ts` no dependa de recalcularlos en cada corrida.

**Hueco declarado:** `plataforma/comun-dominio` debería exponer una tarea que
genere `sorteo.vectores.json` directamente desde `SorteoVerificableTest.java`
(mismo patrón que ya pide `planes/14`), para que la garantía sea "el backend generó
esto" y no "otra implementación en Python coincidió". Pedido al carril de backend
dueño de `plataforma/comun-dominio`.

## `cadena` (CU-72/CU-73) — sin vectores

No hay vectores para `hashDeBloque`/`serializarCanonico` porque **no existe una
implementación Java canónica de esos átomos** al día de este carril — ver el
comentario al inicio de `../src/cadena.ts`. Los tests de `cadena.spec.ts` verifican
la reimplementación cliente contra sí misma (autoconsistencia), no contra el
backend. Pedido al carril de backend dueño de `servicios/transparencia`: publicar
`serializarCanonico`/`hashDeBloque` en `plataforma/comun-dominio` con sus propios
vectores, igual que `SorteoVerificable`.
