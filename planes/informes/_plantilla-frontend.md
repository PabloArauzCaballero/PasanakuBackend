---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril <ID> — <dominio> (<Flutter | Angular>)"
ola: <N>
fase: <N>
mundo: <Flutter | Angular>
modulo: <apps/movil/lib/pantallas/<dominio> | apps/backoffice/src/app/rutas/<dominio> | apps/web/src/app/…>
rama: <usuario>/feature/carril-<id>
estado: en curso
---

# Carril \<ID\> — \<dominio\>

**Fase** \<N\> · **Mundo** \<Flutter | Angular\> · **Casos de uso** \<lista\> · **Puesto** \<P\> · \<máquina\>

> Este archivo lo escribe **solo este carril**. Ningún otro lo toca. La ficha del carril
> está en `planes/18`; el bloque de pantallas, en `planes/22` §2 o §3.

## Pantallas

Una fila por pantalla del bloque de `planes/22`. **Ninguna pantalla se agrega acá sin
estar antes en el mapa**; si la maqueta la tiene y el mapa no, primero se agrega al mapa.

| Pantalla | Ruta | CU | Delta | Organismos | Estados | Maqueta | Golden/captura | Gate |
| --- | --- | :-: | :-: | --- | :-: | :-: | :-: | :-: |
| | `/…` | | | | ⬜ 4/4 | ⬜ | ⬜ | ⬜ |

**Columna Maqueta:** ✅ reproducida igual · ≠ justificada distinta (decir por qué abajo) ·
✏️ corregida en la maqueta (decir qué). Se abre en **los dos escenarios**.

## Piezas declaradas por nivel

Antes de crear cada pieza se declara acá su nivel. Las de `planes/22` §6 **se consumen del
paquete de diseño**, no se declaran acá: si falta una, es micro-PR.

| Pieza | Nivel | Dónde vive | CU | Estado |
| --- | --- | --- | :-: | :-: |

## Comparación contra la maqueta

Por pantalla: qué se reprodujo igual, qué se justificó distinto y por qué, qué se
corrigió en la maqueta. Golden o captura **al lado** de la captura de la maqueta, en
claro y en oscuro, adjuntas al PR.

| Pantalla | Escenario | Igual | Distinto (y por qué) | Corregido en la maqueta |
| --- | :-: | --- | --- | --- |

## Ejemplos del contrato

| CU | Archivo en `packages/simulado/ejemplos/` | Creado acá | Consumido |
| :-: | --- | :-: | :-: |

## Supuestos declarados

Regla cero: ninguno silencioso. Lo que se asumió por falta de información no crítica
va acá, con su CU y su pantalla.

## Micro-PR abiertos al troncal

| Rama | Qué agrega (átomo, token, puerto) | Mundo | Estado |
| --- | --- | :-: | :-: |

## Ficha de paridad iOS (solo Flutter)

Una por bloque cerrado en Android ([[ADR-036 Android primero]]). Sin ficha, el bloque no
entra al pase.

| Pantalla | Área segura | Permisos | Gesto de retroceso | Tipografía dinámica | Decisión |
| --- | --- | --- | --- | --- | --- |

## Bloqueos

Qué está esperando, de quién, desde cuándo. Un contrato que no existe se pide al carril
de backend; **no se inventa**.

## Matriz de gates

Se reporta **así, no en prosa** (`definicion-de-terminado`). La evidencia es la salida
real del comando, no un adjetivo.

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Especificación | Cada pantalla sale de la sección Interfaz de su CU | CU citado por pantalla | ⬜ |
| Maqueta | Cada pantalla comparada en los dos escenarios | tabla de arriba, n/n | ⬜ |
| Estados | Los cuatro, vía `EstadoDePantalla`, con prueba | n/n pantallas | ⬜ |
| Dinero | Todo importe por `Monto`; doble envío bloqueado | prueba por flujo con efecto | ⬜ |
| Contrato | Cada ejemplo valida contra su esquema | `test:front` · contrato n/n | ⬜ |
| Diseño | Cero literales; piezas del paquete, no locales | lint · `verificar_maqueta.py` | ⬜ |
| Accesibilidad | axe / `meetsGuideline` sin violaciones serias | `test:a11y` | ⬜ |
| Arquitectura | Piezas por nivel, sin saltos; shell intacto | lint de capas · `git diff` del shell vacío | ⬜ |
| Entrega | Lint, tipos, pruebas, build | salida citada | ⬜ |

### Frases prohibidas sin evidencia

«Está listo» · «se parece a la maqueta» · «debería funcionar» · «ya está probado». Se
reemplazan por el número que salió: *«11 pantallas, 44 estados probados, 11 comparadas
contra la maqueta en 2 escenarios, `yarn test:front` y `yarn test:a11y` en verde,
arranque en frío 2,4 s en un Samsung A03»*.

## Gate de salida — evidencia

Comandos **ejecutados**, con su resultado. No se marca sin correr.

- [ ] `./gradlew generateOpenApiClients` sin diff
- [ ] `python3 scripts/verificar_maqueta.py` en verde
- [ ] `yarn lint && yarn typecheck`
- [ ] `yarn test:front && yarn test:a11y`
- [ ] Flutter: `flutter test --update-goldens` **no** corrido a ciegas; goldens revisados y en commit propio · `grep -r "Platform.is" lib/` vacío fuera de `infraestructura/`
- [ ] Angular: `budgets` de `angular.json` respetados · Lighthouse CI si es `apps/web`
- [ ] `git diff --stat dev -- <rutas del shell>` vacío
- [ ] Cada criterio de aceptación con su prueba nombrada igual
- [ ] Gate específico de la fase y el gate propio de la ficha

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[10b Estándar de ejecución del frontend]]
