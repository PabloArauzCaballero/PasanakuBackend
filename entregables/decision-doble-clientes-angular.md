# Doble de `clientes/angular` (regla 65) — qué se aisló y qué queda fuera

## El bloqueo real

Al 2026-09-21, `dev@a23bcb1` no trae el directorio `clientes/` en absoluto. Cualquier
`ng test`/`turbo run test:front`/`build` sobre `apps/backoffice` falla con `TS2307` porque
`tsconfig.spec.json` incluye **todos** los `*.spec.ts` de `src/` en un solo programa de
TypeScript, y el bundler (`@angular/build:unit-test`, esbuild) aborta si CUALQUIER archivo
del programa no compila — no hay forma de correr un test aislado sin que el programa
completo esté sano. Lo reproduje corriendo `permisos.spec.ts` (spec que ya existía antes de
este carril, sin tocar) contra el mismo error, antes de escribir una sola línea.

## Qué se aisló (regla 65: nombrar el contrato, simular, declarar)

`src/tipos/clientes-angular.doble.d.ts` declara, módulo por módulo, exactamente los
identificadores que el código HOY importa de `clientes/angular/**` (relevados con `grep`,
no supuestos), tipados `any`. No afirma campos, valores de enum ni formas reales — el
propósito es solo que el compilador deje de fallar por un módulo ausente.

Con eso, el `TS2307` desapareció del programa completo. Se agregaron ahí mismo los dos
identificadores que faltaban (`DecisionDeVerificacion`, `EnlaceDeFoto` de
`cumplimiento/dominio/cu02-expedientes.ts`) al mismo relevamiento.

## Qué NO se arregló, y por qué queda fuera de este carril

Después del doble, el programa completo (`apps/backoffice`, `ng test`/`build` sin acotar)
sigue en rojo por **siete errores de tipos preexistentes, en tres dominios que no son
Richard/sesión**, y que existen independientemente del doble (algunos, `TS7053`, son
indexados de objetos con claves literales — no se resuelven con un doble más laxo ni más
estricto sin conocer la forma real del contrato):

| Archivo | Error | Dominio | Por qué queda fuera |
|---|---|---|---|
| `rutas/publicidad/campanas/pantalla-de-campanas.ts:84` | `TS7053` indexando `this.t.estados[estado]` | Publicidad (Justin/Marcelo, `PR7`/`PR9`) | Fuera del `IN` de `PR11` y de `PR6` |
| `rutas/contabilidad/estados/pantalla-de-estados-financieros.ts:89` | `TS7053` indexando `this.t.tipos[tipo]` | Contabilidad | Nadie de este reparto lo tiene asignado todavía |
| `rutas/cumplimiento/verificaciones/tira-de-fotos.ts:53,55,93,94,126,131` | `TS2538`/`TS2749` (`ExpedienteEnRevisionFotosEnum` usado como tipo e indexado) | Cumplimiento | Fuera del `IN` de `PR11` |

Arreglarlos exigiría, o bien conocer la forma real de `SalidaCampana`/`ExpedienteEnRevisionFotosEnum`
(que es exactamente lo que este doble se prohíbe inventar — regla 00), o editar archivos de
dominios ajenos (regla 00 §3, diff mínimo). Quedan registrados como hallazgo de baseline,
no como regresión de este carril, y bloquean el `build`/`test:front`/`typecheck` **sin
acotar** de `apps/backoffice` hasta que el carril de contratos de Pablo (`clientes/angular`
real) o el dueño de cada dominio los resuelva.

## Cómo se verificó este carril mientras tanto

`apps/backoffice/tsconfig.spec.richard.json` acota el programa de TypeScript a
`src/app/nucleo/**`, `src/app/rutas/ingreso/**`, `src/tipos/**/*.d.ts` y `pruebas/**` — lo
que este carril toca — y se invoca con `ng test --ts-config tsconfig.spec.richard.json`.
Es la única forma de obtener una corrida real (no simulada) de los specs de sesión sin
depender de que el resto del monorepo (fuera de mi alcance) compile. Se borra cuando el
carril de contratos de Pablo publique `clientes/angular` real y los tres hallazgos de la
tabla se resuelvan cada uno por su dueño — en ese momento este carril vuelve a verificarse
contra `tsconfig.spec.json` compartido, como corresponde (regla 30: el peldaño de un área
tocada después vuelve a `WRITTEN` si el contexto cambia).
