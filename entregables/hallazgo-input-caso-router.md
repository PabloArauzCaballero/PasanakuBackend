# Hallazgo — el default de `input()` no sobrevive a `withComponentInputBinding()`

## Qué pasó

Al escribir el E2E real de `pantalla-de-caso.ts` (`caso-de-cumplimiento.e2e.ts`), navegar a
`/cumplimiento/casos/e2e-demo` por una ruta real del `Router` (login real +
`history.pushState`/`popstate`, no `TestBed`) hacía crashear la pantalla:

```
TypeError: Cannot read properties of undefined (reading 'causal')
  at etapasDelCaso (dominio/cu44-caso.ts)
```

Con un `console.log` temporal se confirmó: `casoId()` llegaba bien (`'e2e-demo'`, el
parámetro de ruta), pero `caso()` — que tiene un valor por defecto declarado en
`input<CasoDeCumplimiento>({...demo...})` — llegaba `undefined`.

## Por qué

`provideRouter(routes, withComponentInputBinding())` (en `app.config.ts`) pisa el valor
por defecto de cualquier `input()` opcional cuando el componente se activa por el router,
aunque no exista ningún parámetro, dato de ruta ni query param con ese nombre. Esto **no**
se reproduce en las pruebas unitarias con `TestBed.createComponent()` sin
`fixture.componentRef.setInput('caso', ...)` — ahí el valor por defecto de `input()` sí
se respeta, porque `withComponentInputBinding` es una característica del `Router`, no del
propio `input()`. Es la razón por la que los dos specs preexistentes (que nunca setean
`'caso'`) pasaban antes y ahora, sin que esto se notara: nunca ejercitaron el camino real
del router.

## Corrección

`caso` pasa a `input<CasoDeCumplimiento | undefined>(undefined)` (sin pretender un
default que el router no respeta) y se agrega `casoResuelto = computed(() => this.caso()
?? <demo>)`, usado en el resto del componente en vez de `this.caso()` directo. El
`computed()` sí controla su propio fallback, sin depender de si el router decidió setear
el input o no.

## Alcance de este hallazgo

Es un patrón real del framework (`withComponentInputBinding` + `input()` con default),
no algo específico de `pantalla-de-caso.ts`. **No se revisó si otros componentes del
backoffice tienen el mismo problema** — está fuera del alcance de este carril (solo se
tocó la pantalla piloto). Vale la pena que quien mantenga `native-code-patterns` o el
estándar de Angular del proyecto lo registre como advertencia general: un `input()`
opcional con valor por defecto, si el componente se monta por el router con
`withComponentInputBinding`, necesita resolver su propio fallback con un `computed()`, no
confiar en el argumento de `input()`.

## Evidencia

`evidencia/visual-caso-*.png` (12 capturas, miradas) + `caso-de-cumplimiento.e2e.ts`
(16/16 PASS, incluye el caso de consola sin errores que originalmente reproducía el bug).
