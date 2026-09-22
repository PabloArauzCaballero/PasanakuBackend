# Hallazgo — `--g100` no tiene variante para tema oscuro

- Fecha: 2026-09-22 · Encontrado en: PR11-Sesion.Frontend, H3.S3.M6 (prueba visual de `RestaurandoSesion`)
- Estado: **fuera de alcance de este carril** — se documenta, no se corrige (regla 00 §3, regla 60 "lo roto fuera de alcance se anota, no se arregla")

## Qué se observó

En las 12 capturas de `RestaurandoSesion` (3 viewports × 2 temas × 2 estados), el fondo de
página (`main { background: var(--g100) }`) queda **verde claro fijo** en los dos temas.
En tema oscuro, la tarjeta interior sí cambia a superficie oscura correctamente, pero el
fondo detrás no.

Evidencia: `evidencia/visual-restaurando-movil-claro.png` vs
`evidencia/visual-error-restauracion-tablet-oscuro.png` — mismo verde claro (`#E7F2EB`) en
ambos, pese a que uno está en `prefers-color-scheme: dark`.

## Causa raíz localizada

`packages/tokens/generado/tokens.css:13` define:

```css
:root { --g100: #E7F2EB; /* ... */ }
```

Verificado con:

```
grep -n "\-\-g100\|prefers-color-scheme\|data-theme" packages/tokens/generado/tokens.css
```

`--g100` solo se define en el bloque `:root` raíz (línea 13). Los bloques de tema oscuro
(`@media (prefers-color-scheme: dark)` en línea 115 y `:root[data-theme='dark']` en línea
193) **no redefinen `--g100`**, confirmado con:

```
sed -n '110,230p' packages/tokens/generado/tokens.css | grep -n "g100\|@media\|:root\["
```

Solo aparecen los abre-bloque, ningún `g100` dentro de ese rango.

## Por qué es preexistente, no introducido por este carril

`RestaurandoSesion` (`apps/backoffice/src/app/nucleo/restaurando-sesion.ts`, componente
nuevo de este carril) copió el patrón `main { min-height: 100dvh; ...; background:
var(--g100); }` **literalmente** de `apps/backoffice/src/app/rutas/ingreso/pantalla-de-ingreso.ts`,
un componente ya existente y no tocado por este carril salvo por el manejo del query-param
`volverA`. `pantalla-de-ingreso.ts` usa el mismo token de la misma forma, así que el mismo
problema de fondo-fijo-en-oscuro ya existía ahí antes de este trabajo.

## Impacto

Al menos dos pantallas (`pantalla-de-ingreso`, `restaurando-sesion`) muestran fondo claro
en tema oscuro. No es un defecto de comportamiento (no bloquea flujo), es un defecto visual
de contraste/consistencia de tema.

## Qué falta (fuera de este carril)

Corregir `--g100` requiere agregar su redefinición en los dos bloques de tema oscuro de
`packages/tokens/generado/tokens.css`, y verificar que no rompa otras pantallas que también
lo consuman (búsqueda de todos los usos de `var(--g100)` antes de tocar el token, porque es
compartido). Se registra para quien sea dueño del sistema de diseño (`frontend-design-system`).
